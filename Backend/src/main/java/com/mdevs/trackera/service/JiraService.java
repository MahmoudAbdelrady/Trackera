package com.mdevs.trackera.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.jira.JiraProjectDTO;
import com.mdevs.trackera.dto.jira.JiraTaskDTO;
import com.mdevs.trackera.dto.jira.JiraTaskResponse;
import com.mdevs.trackera.dto.worklog.WorkLogDetailSyncRequestDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.OAuthConnection;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.shared.CacheService;
import com.mdevs.trackera.shared.enums.UserPreferenceOption;
import com.mdevs.trackera.shared.enums.JiraTaskEvaluation;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.enums.OAuthProvider;
import com.mdevs.trackera.shared.DurationFormatter;
import com.mdevs.trackera.shared.exceptions.types.JiraException;
import com.mdevs.trackera.shared.exceptions.types.NotFoundException;
import com.mdevs.trackera.utils.AppUtils;
import com.mdevs.trackera.utils.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class JiraService {
    private final OAuthConnectionService oAuthConnectionService;

    private final UserPreferenceService userPreferenceService;

    private final CacheService cacheService;

    public static final String JIRA_API_BASE_URL = "https://api.atlassian.com/ex/jira/{cloudId}/rest/api/3";

    public static final String USER_JIRA_TASKS_CACHE_KEY_PREFIX = "userJiraTasks:";

    public static final String USER_JIRA_SITES_FETCH_CACHE_KEY_PREFIX = "userJiraSites:";

    private static final int JIRA_TASKS_FETCH_HOURS_DURATION = 1;

    private static final int JIRA_SITES_FETCH_MINUTES_DURATION = 10;

    private static final Duration USER_JIRA_TASKS_CACHE_TTL = Duration.ofHours(JIRA_TASKS_FETCH_HOURS_DURATION);

    private static final DateTimeFormatter JIRA_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    //<editor-fold desc="Retrieval">
    public Map<String, Object> getUserTasks(boolean forceUpdate) {
        User currentUser = AppConfig.getAuthenticatedCurrentUser();
        oAuthConnectionService.validateAndGetConnection(currentUser, OAuthProvider.JIRA);

        String cacheKey = USER_JIRA_TASKS_CACHE_KEY_PREFIX + currentUser.getId();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> cachedData = cacheService.get(cacheKey, Map.class);
        boolean shouldFetch = shouldFetchTasks(cachedData, now, forceUpdate);

        if (shouldFetch) {
            return fetchAndCacheTasks(currentUser, cacheKey, now);
        }

        return cachedData;
    }

    public List<JiraProjectDTO> getUserSites(User user) {
        OAuthConnection connection = oAuthConnectionService.getOrRefresh(user, OAuthProvider.JIRA);
        String cacheKey = USER_JIRA_SITES_FETCH_CACHE_KEY_PREFIX + user.getId();
        List<JiraProjectDTO> resources = cacheService.get(cacheKey, List.class);
        if (resources != null) {
            return resources;
        }

        String accessToken = oAuthConnectionService.getAccessToken(connection);
        resources = Arrays.asList(callJiraApi("https://api.atlassian.com/oauth/token/accessible-resources", HttpMethod.GET, HttpUtil.createBearerAuthEntity(accessToken), connection, JiraProjectDTO[].class));
        cacheService.set(cacheKey, resources, Duration.ofMinutes(JIRA_SITES_FETCH_MINUTES_DURATION));

        return resources;
    }

    public JiraProjectDTO findSiteById(User user, String siteId) {
        return getUserSites(user).stream()
                .filter(site -> site.getId().equals(siteId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Site not found"));
    }

    public static String getApiUrl(JiraProjectDTO projectDTO) {
        return JIRA_API_BASE_URL.replace("{cloudId}", projectDTO.getId());
    }
    //</editor-fold>

    //<editor-fold desc="Caching & Data access">
    public void handleSiteChange(User user) {
        String cacheKey = USER_JIRA_TASKS_CACHE_KEY_PREFIX + user.getId();
        cacheService.evict(cacheKey);
    }

    private Map<String, Object> fetchAndCacheTasks(User user, String cacheKey, LocalDateTime now) {
        List<JiraTaskDTO> jiraTasks = getTasksFromJira(user);
        List<JiraTaskDTO> currentTasks = jiraTasks.stream().filter(task -> !task.isResolved()).toList();
        List<JiraTaskDTO> overestimatedTasks = jiraTasks.stream().filter(task -> task.timeTracking().evaluation() == JiraTaskEvaluation.OVERESTIMATED).toList();
        String lastUpdated = DurationFormatter.getSimpleDateTimeFormatter().format(now);

        Map<String, Object> allTasks = new HashMap<>();
        allTasks.put("currentTasks", Map.of("total", currentTasks.size(), "data", currentTasks));
        allTasks.put("overestimatedTasks", Map.of("total", overestimatedTasks.size(), "data", overestimatedTasks));

        Map<String, Object> result = new HashMap<>();
        result.put("lastUpdated", lastUpdated);
        result.put("tasks", allTasks);

        cacheService.set(cacheKey, result, USER_JIRA_TASKS_CACHE_TTL);

        return result;
    }

    private boolean shouldFetchTasks(Map<String, Object> cachedData, LocalDateTime now, boolean forceUpdate) {
        if (cachedData == null || forceUpdate) return true;

        LocalDateTime lastUpdated = LocalDateTime.parse(cachedData.get("lastUpdated").toString(), DurationFormatter.getSimpleDateTimeFormatter());
        return lastUpdated.isBefore(now.minusHours(JIRA_TASKS_FETCH_HOURS_DURATION));
    }
    //</editor-fold>

    //<editor-fold desc="Integration & Processing">
    public String addOrUpdateWorkLog(User user, WorkLogDetail workLogDetail) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("comment", createJiraCommentObject(workLogDetail.getDescription()));
        requestBody.put("started", JIRA_DATE_FORMATTER.format(LocalDateTime.of(workLogDetail.getWorkLog().getWorkDate(), workLogDetail.getStartTime()).atZone(ZoneId.systemDefault())));
        requestBody.put("timeSpentSeconds", workLogDetail.getDuration() * 60);

        boolean isUpdate = !StringUtils.isEmpty(workLogDetail.getJiraId());
        String apiUrl = getApiUrl(validateAndGetUserJiraPrimaryProject(user)) + "/issue/" + workLogDetail.getTaskName() + "/worklog" + (isUpdate ? ("/" + workLogDetail.getJiraId()) : "");
        OAuthConnection oAuthConnection = oAuthConnectionService.getOrRefresh(user, OAuthProvider.JIRA);
        String accessToken = oAuthConnectionService.getAccessToken(oAuthConnection);
        Map<String, Object> response = callJiraApi(apiUrl, isUpdate ? HttpMethod.PUT : HttpMethod.POST, HttpUtil.createBearerAuthEntity(accessToken, requestBody), oAuthConnection, Map.class);

        return response.get("id").toString();
    }

    public void deleteWorkLog(User user, WorkLogDetailSyncRequestDTO detailSyncRequestDTO) {
        String apiUrl = getApiUrl(validateAndGetUserJiraPrimaryProject(user)) + "/issue/" + detailSyncRequestDTO.getTaskName() + "/worklog/" + detailSyncRequestDTO.getJiraId();
        OAuthConnection oAuthConnection = oAuthConnectionService.getOrRefresh(user, OAuthProvider.JIRA);
        String accessToken = oAuthConnectionService.getAccessToken(oAuthConnection);
        callJiraApi(apiUrl, HttpMethod.DELETE, HttpUtil.createBearerAuthEntity(accessToken), oAuthConnection, Void.class);
    }

    private List<JiraTaskDTO> getTasksFromJira(User user) {
        List<JiraTaskDTO> jiraTasks = new ArrayList<>();
        OAuthConnection oAuthConnection = oAuthConnectionService.getOrRefresh(user, OAuthProvider.JIRA);
        JiraProjectDTO userJiraPrimaryProject = validateAndGetUserJiraPrimaryProject(user);

        String accessToken = oAuthConnectionService.getAccessToken(oAuthConnection);
        Map<String, Object> jiraResponse;
        String url = getApiUrl(userJiraPrimaryProject) + "/search/jql?jql=" + getJiraTasksSearchCondition() + "&fields=key,summary,status,timetracking,project,resolution";
        String nextPageToken = null;
        boolean isLast;

        do {
            String pagedUrl = url + (nextPageToken != null ? "&nextPageToken=" + nextPageToken : "");
            jiraResponse = callJiraApi(pagedUrl, HttpMethod.GET, HttpUtil.createBearerAuthEntity(accessToken), oAuthConnection, Map.class);
            processJiraResponseTasks(jiraResponse, userJiraPrimaryProject, jiraTasks);
            isLast = (boolean) jiraResponse.get("isLast");
            nextPageToken = (String) jiraResponse.get("nextPageToken");
        } while (!isLast);

        return jiraTasks;
    }

    private JiraProjectDTO validateAndGetUserJiraPrimaryProject(User user) {
        JiraProjectDTO userJiraPrimaryProject = (JiraProjectDTO) userPreferenceService.getPreferenceValue(user, UserPreferenceOption.JIRA_PRIMARY_PROJECT);
        if (userJiraPrimaryProject == null) {
            throw new BusinessException("Jira primary project not set. Please set it in your settings.");
        }
        return userJiraPrimaryProject;
    }

    private void processJiraResponseTasks(Map<String, Object> jiraResponse, JiraProjectDTO userJiraPrimaryProject, List<JiraTaskDTO> jiraTasks) {
        List<Map<String, Object>> retrievedTasks = (List<Map<String, Object>>) jiraResponse.get("issues");
        String projectBaseUrl = userJiraPrimaryProject.getUrl();
        for (Map<String, Object> rawTask : retrievedTasks) {
            JiraTaskResponse task = new JiraTaskResponse((String) rawTask.get("key"), (Map<String, Object>) rawTask.get("fields"));

            JiraTaskDTO.TimeTrackingDTO timeTracking = buildTimeTrackingInfo(task.getTimeTracking());

            JiraTaskDTO taskInfo = buildTaskInfo(task, projectBaseUrl, timeTracking);

            jiraTasks.add(taskInfo);
        }
    }

    private JiraTaskDTO.TimeTrackingDTO buildTimeTrackingInfo(Map<String, Object> taskTimeTracking) {
        if (taskTimeTracking == null || taskTimeTracking.isEmpty()) {
            return new JiraTaskDTO.TimeTrackingDTO(null, null, null, null, null);
        }

        String originalEstimate = (String) taskTimeTracking.get("originalEstimate");
        String loggedTime = (String) taskTimeTracking.get("timeSpent");
        String remainingTime = (String) taskTimeTracking.get("remainingEstimate");
        JiraTaskEvaluation jiraTaskEvaluation = null;
        String notes = null;

        if (taskTimeTracking.containsKey("timeSpentSeconds") && taskTimeTracking.containsKey("originalEstimateSeconds")) {
            int timeSpentSeconds = ((Number) taskTimeTracking.get("timeSpentSeconds")).intValue();
            int originalEstimateSeconds = ((Number) taskTimeTracking.get("originalEstimateSeconds")).intValue();
            if (timeSpentSeconds <= originalEstimateSeconds) {
                jiraTaskEvaluation = JiraTaskEvaluation.ON_TIME;
            } else {
                jiraTaskEvaluation = JiraTaskEvaluation.OVERESTIMATED;
                notes = "Overestimated by " + DurationFormatter.formatDuration((timeSpentSeconds - originalEstimateSeconds) / 60, true);
            }
        }

        return new JiraTaskDTO.TimeTrackingDTO(originalEstimate, loggedTime, remainingTime, jiraTaskEvaluation, notes);
    }

    private JiraTaskDTO buildTaskInfo(JiraTaskResponse task, String projectBaseUrl, JiraTaskDTO.TimeTrackingDTO timeTracking) {
        Map<String, Object> status = task.getStatus();
        Map<String, Object> project = task.getProject();

        return new JiraTaskDTO(
                task.getSummary(),
                projectBaseUrl + "/browse/" + task.key(),
                new JiraTaskDTO.StatusDTO(
                        (String) status.get("name"),
                        (String) ((Map<String, Object>) status.get("statusCategory")).get("key")
                ),
                task.getResolution() != null && !task.getResolution().toString().isEmpty(),
                new JiraTaskDTO.ProjectDTO(
                        (String) project.get("name"),
                        (String) ((Map<String, Object>) project.get("avatarUrls")).get("48x48")
                ),
                timeTracking
        );
    }

    private String getJiraTasksSearchCondition() {
        String maxDate = String.valueOf(LocalDate.now().minusMonths(3).withDayOfMonth(1));
        return "assignee=currentUser() AND (resolution IS EMPTY OR (resolutiondate >= '" + maxDate + "' AND timespent > 0)) ORDER BY created DESC";
    }

    @Retryable(retryFor = Exception.class, backoff = @Backoff(delay = 1000, multiplier = 3))
    private <T> T callJiraApi(String url, HttpMethod method, HttpEntity<?> entity, OAuthConnection oAuthConnection, Class<T> responseType) {
        try {
            ResponseEntity<T> response = HttpUtil.getRestTemplate().exchange(url, method, entity, responseType);

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                oAuthConnection = oAuthConnectionService.getOrRefresh(oAuthConnection);

                HttpHeaders newHeaders = new HttpHeaders();
                newHeaders.putAll(entity.getHeaders());
                newHeaders.setBearerAuth(oAuthConnectionService.getAccessToken(oAuthConnection));
                entity = new HttpEntity<>(newHeaders);

                response = HttpUtil.getRestTemplate().exchange(url, method, entity, responseType);
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Jira API request failed with status: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (HttpClientErrorException exception) {
            log.error("Error while calling Jira API with url: {}", url, exception);
            extractErrorAndThrow(exception);
            return null; // Unreachable, but required for compilation
        }
    }

    private void extractErrorAndThrow(HttpClientErrorException e) {
        String responseBody = e.getResponseBodyAsString();

        JsonNode root;
        try {
            root = AppUtils.getObjectMapper().readTree(responseBody);
        } catch (Exception parseErr) {
            throw new JiraException("Jira API Error: " + responseBody, e.getStatusCode().value());
        }

        // 1. handle "errorMessages" list
        if (root.has("errorMessages") && root.get("errorMessages").isArray() && !root.get("errorMessages").isEmpty()) {
            List<String> errors = new ArrayList<>();
            root.get("errorMessages").forEach(msg -> errors.add(msg.asText()));

            String message = String.join(" | ", errors);
            throw new JiraException(message, e.getStatusCode().value());
        }

        // 3. fallback unknown Jira error
        throw new JiraException("Jira API Error: " + responseBody, e.getStatusCode().value());
    }

    private Object createJiraCommentObject(String comment) {
        Map<String, Object> commentObject = new HashMap<>();
        commentObject.put("type", "doc");
        commentObject.put("version", 1);

        List<Object> paragraphContent = new ArrayList<>();
        Map<String, Object> textNode = new HashMap<>();
        textNode.put("type", "text");
        textNode.put("text", comment);
        paragraphContent.add(textNode);

        Map<String, Object> paragraph = new HashMap<>();
        paragraph.put("type", "paragraph");
        paragraph.put("content", paragraphContent);

        List<Object> contentList = new ArrayList<>();
        contentList.add(paragraph);

        commentObject.put("content", contentList);

        return commentObject;
    }
    //</editor-fold>
}
