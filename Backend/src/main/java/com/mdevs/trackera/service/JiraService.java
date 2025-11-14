package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.jira.JiraProjectDTO;
import com.mdevs.trackera.dto.jira.JiraTaskDTO;
import com.mdevs.trackera.dto.jira.JiraTaskResponse;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.OAuthConnection;
import com.mdevs.trackera.shared.enums.UserPreferenceOption;
import com.mdevs.trackera.shared.enums.JiraTaskEvaluation;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.exceptions.types.NotFoundException;
import com.mdevs.trackera.oauth.OAuthProvider;
import com.mdevs.trackera.shared.DurationFormatter;
import com.mdevs.trackera.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class JiraService {
    private final RedisTemplate<String, Object> redisTemplate;

    private final OAuthConnectionService oAuthConnectionService;

    private final UserPreferenceService userPreferenceService;

    public static final String JIRA_API_BASE_URL = "https://api.atlassian.com/ex/jira/{cloudId}/rest/api/3";

    private static final String USER_JIRA_TASKS_CACHE_KEY_PREFIX = "userJiraTasks:";

    private static final String USER_JIRA_TASKS_FORCE_UPDATE_CACHE_KEY_PREFIX = "userJiraTasks:forceUpdate:";

    private static final String USER_JIRA_SITES_FETCH_CACHE_KEY_PREFIX = "userJiraSites:";

    private static final int JIRA_TASKS_FETCH_HOURS_DURATION = 1;

    private static final int JIRA_TASKS_FORCE_FETCH_MINUTES_DURATION = 15;

    private static final int JIRA_SITES_FETCH_MINUTES_DURATION = 10;

    private static final Duration USER_JIRA_TASKS_CACHE_TTL = Duration.ofHours(JIRA_TASKS_FETCH_HOURS_DURATION);

    private static final Duration USER_JIRA_TASKS_FORCE_UPDATE_CACHE_TTL = Duration.ofMinutes(JIRA_TASKS_FORCE_FETCH_MINUTES_DURATION);

    public JiraService(RedisTemplate<String, Object> redisTemplate, OAuthConnectionService oAuthConnectionService, UserPreferenceService userPreferenceService) {
        this.redisTemplate = redisTemplate;
        this.oAuthConnectionService = oAuthConnectionService;
        this.userPreferenceService = userPreferenceService;
    }

    //<editor-fold desc="Retrieval">
    public Map<String, Object> getUserTasks(boolean forceUpdate) {
        User currentUser = AppConfig.getAuthenticatedCurrentUser();
        oAuthConnectionService.validateAndGetConnection(currentUser, OAuthProvider.JIRA);

        String cacheKey = USER_JIRA_TASKS_CACHE_KEY_PREFIX + currentUser.getId();
        String forceUpdateCacheKey = USER_JIRA_TASKS_FORCE_UPDATE_CACHE_KEY_PREFIX + currentUser.getId();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> cachedData = fetchFromCache(currentUser, cacheKey, Map.class);
        boolean shouldFetch = shouldFetchTasks(cachedData, forceUpdateCacheKey, now, forceUpdate);

        if (shouldFetch) {
            return fetchAndCacheTasks(currentUser, cacheKey, forceUpdateCacheKey, now);
        }

        return cachedData;
    }

    public List<JiraProjectDTO> getUserSites(User user) {
        OAuthConnection connection = oAuthConnectionService.validateAndGetConnection(user, OAuthProvider.JIRA);
        String cacheKey = USER_JIRA_SITES_FETCH_CACHE_KEY_PREFIX + user.getId();
        List<JiraProjectDTO> resources = fetchFromCache(user, cacheKey, List.class);
        if (resources != null) {
            return resources;
        }

        String accessToken = oAuthConnectionService.resolveValidAccessToken(connection);
        resources = Arrays.asList(callJiraApi("https://api.atlassian.com/oauth/token/accessible-resources", HttpMethod.GET, HttpUtil.createBearerAuthEntity(accessToken), connection, JiraProjectDTO[].class));
        redisTemplate.opsForValue().set(cacheKey, resources, Duration.ofMinutes(JIRA_SITES_FETCH_MINUTES_DURATION));

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
    private <T> T fetchFromCache(User user, String cacheKey, Class<T> resultType) {
        try {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            return cachedData != null ? resultType.cast(cachedData) : null;
        } catch (Exception e) {
            log.error("Error while fetching cached Jira data for user with id: {}, and cache key: {}", user.getId(), cacheKey, e);
            return null;
        }
    }

    private Map<String, Object> fetchAndCacheTasks(User user, String cacheKey, String forceUpdateCacheKey, LocalDateTime now) {
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

        redisTemplate.opsForValue().set(cacheKey, result, USER_JIRA_TASKS_CACHE_TTL);
        redisTemplate.opsForValue().set(forceUpdateCacheKey, lastUpdated, USER_JIRA_TASKS_FORCE_UPDATE_CACHE_TTL);

        return result;
    }

    private boolean shouldFetchTasks(Map<String, Object> cachedData, String forceUpdateCacheKey, LocalDateTime now, boolean forceUpdate) {
        if (cachedData == null) return true;

        if (forceUpdate) {
            LocalDateTime lastForceUpdate = Optional.ofNullable(redisTemplate.opsForValue().get(forceUpdateCacheKey))
                    .map(date -> LocalDateTime.parse(date.toString(), DurationFormatter.getSimpleDateTimeFormatter())).orElse(null);
            return lastForceUpdate == null || lastForceUpdate.isBefore(now.minusMinutes(JIRA_TASKS_FORCE_FETCH_MINUTES_DURATION));
        }

        LocalDateTime lastUpdated = LocalDateTime.parse(cachedData.get("lastUpdated").toString(), DurationFormatter.getSimpleDateTimeFormatter());
        return lastUpdated.isBefore(now.minusHours(JIRA_TASKS_FETCH_HOURS_DURATION));
    }
    //</editor-fold>

    //<editor-fold desc="Integration & Processing">
    private List<JiraTaskDTO> getTasksFromJira(User user) {
        List<JiraTaskDTO> jiraTasks = new ArrayList<>();
        OAuthConnection connection = oAuthConnectionService.validateAndGetConnection(user, OAuthProvider.JIRA);
        JiraProjectDTO userJiraPrimaryProject = (JiraProjectDTO) userPreferenceService.getPreferenceValue(user, UserPreferenceOption.JIRA_PRIMARY_PROJECT);
        if (userJiraPrimaryProject == null) {
            throw new BusinessException("Jira primary project not set. Please set it in your settings.");
        }

        String accessToken = oAuthConnectionService.resolveValidAccessToken(connection);
        Map<String, Object> jiraResponse;
        String url = getApiUrl(userJiraPrimaryProject) + "/search/jql?jql=" + getJiraTasksSearchCondition() + "&fields=key,summary,status,timetracking,project,resolution";
        String nextPageToken = null;
        boolean isLast;

        do {
            String pagedUrl = url + (nextPageToken != null ? "&nextPageToken=" + nextPageToken : "");
            jiraResponse = callJiraApi(pagedUrl, HttpMethod.GET, HttpUtil.createBearerAuthEntity(accessToken), connection, Map.class);
            processJiraResponseTasks(jiraResponse, userJiraPrimaryProject, jiraTasks);
            isLast = (boolean) jiraResponse.get("isLast");
            nextPageToken = (String) jiraResponse.get("nextPageToken");
        } while (!isLast);

        return jiraTasks;
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
    private <T> T callJiraApi(String url, HttpMethod method, HttpEntity<Void> entity, OAuthConnection OAuthConnection, Class<T> responseType) {
        try {
            ResponseEntity<T> response = HttpUtil.getRestTemplate().exchange(url, method, entity, responseType);

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                String newAccessToken = oAuthConnectionService.resolveValidAccessToken(OAuthConnection);

                HttpHeaders newHeaders = new HttpHeaders();
                newHeaders.putAll(entity.getHeaders());
                newHeaders.setBearerAuth(newAccessToken);
                entity = new HttpEntity<>(newHeaders);

                response = HttpUtil.getRestTemplate().exchange(url, method, entity, responseType);
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Jira API request failed with status: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error while calling Jira API", e);
        }
    }
    //</editor-fold>
}
