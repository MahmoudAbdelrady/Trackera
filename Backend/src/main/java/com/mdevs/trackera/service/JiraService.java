package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.jira.AccessibleResourceDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.shared.enums.JiraTaskEvaluation;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.utils.AppUtils;
import com.mdevs.trackera.utils.TrackeraTimeSpanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class JiraService {
    private final RedisTemplate<String, Object> redisTemplate;

    private final UserOAuthProviderService userOAuthProviderService;

    private final UserPreferredSettingService userPreferredSettingService;

    public static final String JIRA_PRIMARY_PROJECT_SETTING_KEY = "jiraPrimaryProject";

    public static final String JIRA_API_BASE_URL = "https://api.atlassian.com/ex/jira/{cloudId}/rest/api/3";

    private static final String USER_JIRA_TASKS_CACHE_KEY_PREFIX = "userJiraTasks:";

    private static final String USER_JIRA_TASKS_FORCE_UPDATE_CACHE_KEY_PREFIX = "userJiraTasks:forceUpdate:";

    private static final String USER_JIRA_SITES_FETCH_CACHE_KEY_PREFIX = "userJiraSites:";

    private static final int JIRA_TASKS_FETCH_HOURS_DURATION = 1;

    private static final int JIRA_TASKS_FORCE_FETCH_MINUTES_DURATION = 15;

    private static final int JIRA_SITES_FETCH_MINUTES_DURATION = 10;

    private static final Duration USER_JIRA_TASKS_CACHE_TTL = Duration.ofHours(JIRA_TASKS_FETCH_HOURS_DURATION);

    private static final Duration USER_JIRA_TASKS_FORCE_UPDATE_CACHE_TTL = Duration.ofMinutes(JIRA_TASKS_FORCE_FETCH_MINUTES_DURATION);

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraService.class);

    public JiraService(RedisTemplate<String, Object> redisTemplate, UserOAuthProviderService userOAuthProviderService, UserPreferredSettingService userPreferredSettingService) {
        this.redisTemplate = redisTemplate;
        this.userOAuthProviderService = userOAuthProviderService;
        this.userPreferredSettingService = userPreferredSettingService;
    }

    public Map<String, Object> getUserTasks(boolean forceUpdate) {
        User currentUser = AppConfig.getAuthenticatedCurrentUser();
        userOAuthProviderService.validateAndGetOAuthProvider(currentUser, OAuthProvider.JIRA);

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

    private <T> T fetchFromCache(User user, String cacheKey, Class<T> resultType) {
        try {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            return cachedData != null ? resultType.cast(cachedData) : null;
        } catch (Exception e) {
            LOGGER.error("Error while fetching cached Jira data for user with id: {}, and cache key: {}", user.getId(), cacheKey, e);
            return null;
        }
    }

    private boolean shouldFetchTasks(Map<String, Object> cachedData, String forceUpdateCacheKey, LocalDateTime now, boolean forceUpdate) {
        if (cachedData == null) return true;

        if (forceUpdate) {
            LocalDateTime lastForceUpdate = Optional.ofNullable(redisTemplate.opsForValue().get(forceUpdateCacheKey))
                    .map(date -> LocalDateTime.parse(date.toString(), TrackeraTimeSpanUtil.getSimpleDateTimeFormatter())).orElse(null);
            return lastForceUpdate == null || lastForceUpdate.isBefore(now.minusMinutes(JIRA_TASKS_FORCE_FETCH_MINUTES_DURATION));
        }

        LocalDateTime lastUpdated = LocalDateTime.parse(cachedData.get("lastUpdated").toString(), TrackeraTimeSpanUtil.getSimpleDateTimeFormatter());
        return lastUpdated.isBefore(now.minusHours(JIRA_TASKS_FETCH_HOURS_DURATION));
    }

    private Map<String, Object> fetchAndCacheTasks(User user, String cacheKey, String forceUpdateCacheKey, LocalDateTime now) {
        List<Map<String, Object>> jiraTasks = getTasksFromJira(user);
        List<Map<String, Object>> currentTasks = jiraTasks.stream().filter(task -> !(Boolean) task.get("isResolved")).toList();
        List<Map<String, Object>> overestimatedTasks = jiraTasks.stream().filter(task -> ((Map<String, Object>) task.get("timeTracking")).get("evaluation") == JiraTaskEvaluation.OVERESTIMATED).toList();
        String lastUpdated = TrackeraTimeSpanUtil.getSimpleDateTimeFormatter().format(now);

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

    private List<Map<String, Object>> getTasksFromJira(User user) {
        List<Map<String, Object>> jiraTasks = new ArrayList<>();
        UserOAuthProvider userOAuthProvider = userOAuthProviderService.validateAndGetOAuthProvider(user, OAuthProvider.JIRA);
        Map<String, Object> userJiraPrimaryProject = userPreferredSettingService.getPreferenceValue(user, JIRA_PRIMARY_PROJECT_SETTING_KEY, Map.class);
        if (userJiraPrimaryProject == null) {
            throw new BusinessException("Jira primary project not set. Please set it in your settings.");
        }

        String accessToken = userOAuthProviderService.resolveValidAccessToken(userOAuthProvider);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        Map<String, Object> jiraResponse;
        String url = JIRA_API_BASE_URL.replace("{cloudId}", userJiraPrimaryProject.get("id").toString()) + "/search/jql?jql=" + getJiraTasksSearchCondition() + "&fields=key,summary,status,timetracking,project,resolution";
        String nextPageToken = null;
        boolean isLast;

        do {
            String pagedUrl = url + (nextPageToken != null ? "&nextPageToken=" + nextPageToken : "");
            jiraResponse = callJiraApi(pagedUrl, HttpMethod.GET, headers, userOAuthProvider, Map.class);
            processJiraResponseTasks(jiraResponse, userJiraPrimaryProject, jiraTasks);
            isLast = (boolean) jiraResponse.get("isLast");
            nextPageToken = (String) jiraResponse.get("nextPageToken");
        } while (!isLast);

        return jiraTasks;
    }

    private void processJiraResponseTasks(Map<String, Object> jiraResponse, Map<String, Object> userJiraPrimaryProject, List<Map<String, Object>> jiraTasks) {
        for (Map<String, Object> task : (List<Map<String, Object>>) jiraResponse.get("issues")) {
            Map<String, Object> taskFields = (Map<String, Object>) task.get("fields");
            Map<String, Object> taskProject = (Map<String, Object>) taskFields.get("project");
            Map<String, Object> taskStatus = ((Map<String, Object>) taskFields.get("status"));
            Map<String, Object> taskTimeTracking = (Map<String, Object>) taskFields.get("timetracking");

            String originalEstimate = null;
            String loggedTime = null;
            String remainingTime = null;
            JiraTaskEvaluation jiraTaskEvaluation = null;
            String notes = null;

            if (taskTimeTracking != null && !taskTimeTracking.isEmpty()) {
                originalEstimate = (String) taskTimeTracking.get("originalEstimate");
                loggedTime = (String) taskTimeTracking.get("timeSpent");
                remainingTime = (String) taskTimeTracking.get("remainingEstimate");
                if (taskTimeTracking.containsKey("timeSpentSeconds") && taskTimeTracking.containsKey("originalEstimateSeconds")) {
                    int timeSpentSeconds = ((Number) taskTimeTracking.get("timeSpentSeconds")).intValue();
                    int originalEstimateSeconds = ((Number) taskTimeTracking.get("originalEstimateSeconds")).intValue();
                    if (timeSpentSeconds <= originalEstimateSeconds) {
                        jiraTaskEvaluation = JiraTaskEvaluation.ON_TIME;
                    } else {
                        jiraTaskEvaluation = JiraTaskEvaluation.OVERESTIMATED;
                        notes = "Overestimated by " + TrackeraTimeSpanUtil.formatDuration((timeSpentSeconds - originalEstimateSeconds) / 60, true);
                    }
                }
            }

            Map<String, Object> timeTracking = new HashMap<>();
            timeTracking.put("originalEstimate", originalEstimate);
            timeTracking.put("loggedTime", loggedTime);
            timeTracking.put("remainingTime", remainingTime);
            timeTracking.put("evaluation", jiraTaskEvaluation);
            timeTracking.put("notes", notes);

            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskName", taskFields.get("summary"));
            taskInfo.put("taskUrl", userJiraPrimaryProject.get("url") + "/browse/" + task.get("key"));
            taskInfo.put("status", Map.of("name", taskStatus.get("name"), "category", ((Map<String, Object>) taskStatus.get("statusCategory")).get("key")));
            taskInfo.put("isResolved", taskFields.get("resolution") != null && !taskFields.get("resolution").toString().isEmpty());
            taskInfo.put("project", Map.of("name", taskProject.get("name"), "icon", ((Map<String, Object>) taskProject.get("avatarUrls")).get("48x48")));
            taskInfo.put("timeTracking", timeTracking);

            jiraTasks.add(taskInfo);
        }
    }

    private String getJiraTasksSearchCondition() {
        String maxDate = String.valueOf(LocalDate.now().minusMonths(3).withDayOfMonth(1));
        return "assignee=currentUser() AND (resolution IS EMPTY OR (resolutiondate >= '" + maxDate + "' AND timespent > 0)) ORDER BY created DESC";
    }

    public List<AccessibleResourceDTO> getUserSites(User user) {
        UserOAuthProvider userOAuthProvider = userOAuthProviderService.validateAndGetOAuthProvider(user, OAuthProvider.JIRA);
        String cacheKey = USER_JIRA_SITES_FETCH_CACHE_KEY_PREFIX + user.getId();
        List<AccessibleResourceDTO> resources = fetchFromCache(user, cacheKey, List.class);
        if (resources != null) {
            return resources;
        }

        String accessToken = userOAuthProviderService.resolveValidAccessToken(userOAuthProvider);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        resources = Arrays.asList(callJiraApi("https://api.atlassian.com/oauth/token/accessible-resources", HttpMethod.GET, headers, userOAuthProvider, AccessibleResourceDTO[].class));
        redisTemplate.opsForValue().set(cacheKey, resources, Duration.ofMinutes(JIRA_SITES_FETCH_MINUTES_DURATION));

        return resources;
    }

    @Retryable(retryFor = Exception.class, backoff = @Backoff(delay = 1000, multiplier = 3))
    private <T> T callJiraApi(String url, HttpMethod method, HttpHeaders headers, UserOAuthProvider userOAuthProvider, Class<T> responseType) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<T> response = AppUtils.getRestTemplate().exchange(url, method, entity, responseType);

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                String newAccessToken = userOAuthProviderService.resolveValidAccessToken(userOAuthProvider);

                HttpHeaders newHeaders = new HttpHeaders();
                newHeaders.putAll(entity.getHeaders());
                newHeaders.setBearerAuth(newAccessToken);
                entity = new HttpEntity<>(newHeaders);

                response = AppUtils.getRestTemplate().exchange(url, method, entity, responseType);
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Jira API request failed with status: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error while calling Jira API", e);
        }
    }
}
