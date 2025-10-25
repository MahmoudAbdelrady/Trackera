package com.mdevs.trackera.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.jira.AccessibleResourceDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.entity.UserPreferredSetting;
import com.mdevs.trackera.repository.UserOAuthProviderRepository;
import com.mdevs.trackera.repository.UserPreferredSettingRepository;
import com.mdevs.trackera.shared.enums.JiraTaskEvaluation;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthProviderFactory;
import com.mdevs.trackera.utils.AppUtils;
import com.mdevs.trackera.utils.TrackeraHasher;
import com.mdevs.trackera.utils.TrackeraTimeSpanUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class JiraService {
    private final RedisTemplate<String, Object> redisTemplate;

    private final UserOAuthProviderRepository userOAuthProviderRepository;

    private final UserPreferredSettingRepository userPreferredSettingRepository;

    private final OAuthProviderFactory oAuthProviderFactory;

    private final TrackeraHasher trackeraHasher;

    private final JiraService selfRef;

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

    public JiraService(RedisTemplate<String, Object> redisTemplate, UserOAuthProviderRepository userOAuthProviderRepository, UserPreferredSettingRepository userPreferredSettingRepository,
                       OAuthProviderFactory oAuthProviderFactory, TrackeraHasher trackeraHasher, @Lazy JiraService selfRef) {
        this.redisTemplate = redisTemplate;
        this.userOAuthProviderRepository = userOAuthProviderRepository;
        this.userPreferredSettingRepository = userPreferredSettingRepository;
        this.oAuthProviderFactory = oAuthProviderFactory;
        this.trackeraHasher = trackeraHasher;
        this.selfRef = selfRef;
    }

    public Map<String, Object> getUserTasks(boolean forceUpdate) {
        User currentUser = AppConfig.getCurrentUser();
        validateJiraOAuthProvider();

        String cacheKey = USER_JIRA_TASKS_CACHE_KEY_PREFIX + Objects.requireNonNull(currentUser).getId();
        String forceUpdateCacheKey = USER_JIRA_TASKS_FORCE_UPDATE_CACHE_KEY_PREFIX + Objects.requireNonNull(currentUser).getId();
        boolean shouldFetch = false;
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> cachedData;

        try {
            cachedData = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            LOGGER.error("Error while fetching cached Jira tasks for user with id: {}", currentUser.getId(), e);
            throw new RuntimeException("Something went wrong while fetching Jira tasks");
        }

        if (cachedData == null) {
            shouldFetch = true;
        } else if (forceUpdate) {
            LocalDateTime lastForceUpdate = Optional.ofNullable(redisTemplate.opsForValue().get(forceUpdateCacheKey)).map(date -> LocalDateTime.parse(date.toString(), TrackeraTimeSpanUtil.getSimpleDateTimeFormatter())).orElse(null);
            if (lastForceUpdate == null || lastForceUpdate.isBefore(now.minusMinutes(JIRA_TASKS_FORCE_FETCH_MINUTES_DURATION))) {
                shouldFetch = true;
            }
        } else if ((LocalDateTime.parse(cachedData.get("lastUpdated").toString(), TrackeraTimeSpanUtil.getSimpleDateTimeFormatter())).isBefore(now.minusHours(JIRA_TASKS_FETCH_HOURS_DURATION))) {
            shouldFetch = true;
        }

        if (shouldFetch) {
            List<Map<String, Object>> jiraTasks = getTasksFromJira();
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
        } else {
            return cachedData;
        }
    }

    private List<Map<String, Object>> getTasksFromJira() {
        List<Map<String, Object>> jiraTasks = new ArrayList<>();
        UserOAuthProvider userOAuthProvider = validateJiraOAuthProvider();
        Map<String, Object> userJiraPrimaryProject = getUserPrimaryProject();
        String accessToken;

        try {
            accessToken = userOAuthProvider.isExpired() ? validateAndGetNewAccessToken(userOAuthProvider) : trackeraHasher.decryptFromBase64(userOAuthProvider.getAccessToken(), false);
        } catch (Exception e) {
            LOGGER.error("Error while getting jira access token", e);
            throw new RuntimeException("Failed to fetch jira tasks");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        Map<String, Object> jiraResponse;
        String url = JIRA_API_BASE_URL.replace("{cloudId}", userJiraPrimaryProject.get("id").toString()) + "/search?jql=" + getJiraTasksSearchCondition() + "&fields=key,summary,status,timetracking,project,resolution";
        int page = 0;

        do {
            jiraResponse = callJiraApi(url, HttpMethod.GET, entity, Map.class, userOAuthProvider);
            if (jiraResponse.containsKey("code") && ((Integer) jiraResponse.get("code")) == 401) {
                try {
                    headers.setBearerAuth(validateAndGetNewAccessToken(userOAuthProvider));
                } catch (Exception e) {
                    LOGGER.error("Error while refreshing jira access token", e);
                    throw new RuntimeException("Failed to fetch jira tasks");
                }
            } else {
                page = page + Integer.parseInt(jiraResponse.get("maxResults").toString());
                processJiraResponseTasks(jiraResponse, userJiraPrimaryProject, jiraTasks);
            }
        } while (page < Integer.parseInt(jiraResponse.get("total").toString()));
        return jiraTasks;
    }

    private UserOAuthProvider validateJiraOAuthProvider() {
        UserOAuthProvider userOAuthProvider = userOAuthProviderRepository.findByUserAndProvider(AppConfig.getCurrentUser(), OAuthProvider.JIRA);
        if (userOAuthProvider == null) {
            throw new BusinessException("Jira account not linked");
        }
        if (userOAuthProvider.isRevoked()) {
            throw new BusinessException("Jira account link has been revoked. Please relink your account.");
        }
        return userOAuthProvider;
    }

    private void processJiraResponseTasks(Map<String, Object> jiraResponse, Map<String, Object> userJiraPrimaryProject, List<Map<String, Object>> jiraTasks) {
        for (Map<String, Object> task : (List<Map<String, Object>>) jiraResponse.get("issues")) {
            Map<String, Object> taskFields = (Map<String, Object>) task.get("fields");
            Map<String, Object> taskProject = (Map<String, Object>) taskFields.get("project");
            Map<String, Object> taskStatus = ((Map<String, Object>) taskFields.get("status"));
            Map<String, Object> taskTimeTracking = (Map<String, Object>) taskFields.get("timetracking");

            String originalEstimate = null;
            String loggedHours = null;
            String remainingHours = null;
            JiraTaskEvaluation jiraTaskEvaluation = null;
            String notes = null;

            if (taskTimeTracking != null && !taskTimeTracking.isEmpty()) {
                originalEstimate = (String) taskTimeTracking.get("originalEstimate");
                loggedHours = (String) taskTimeTracking.get("timeSpent");
                remainingHours = (String) taskTimeTracking.get("remainingEstimate");
                if (taskTimeTracking.containsKey("timeSpentSeconds") && taskTimeTracking.containsKey("originalEstimateSeconds")) {
                    int timeSpentSeconds = (Integer) taskTimeTracking.get("timeSpentSeconds");
                    int originalEstimateSeconds = (Integer) taskTimeTracking.get("originalEstimateSeconds");
                    if (timeSpentSeconds <= originalEstimateSeconds) {
                        jiraTaskEvaluation = JiraTaskEvaluation.ON_TIME;
                    } else {
                        jiraTaskEvaluation = JiraTaskEvaluation.OVERESTIMATED;
                        notes = "Overestimated by " + TrackeraTimeSpanUtil.formatDuration(BigDecimal.valueOf((timeSpentSeconds - originalEstimateSeconds) / 3600), true);
                    }
                }
            }

            Map<String, Object> timeTracking = new HashMap<>();
            timeTracking.put("originalEstimate", originalEstimate);
            timeTracking.put("loggedHours", loggedHours);
            timeTracking.put("remainingHours", remainingHours);
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

    private Map<String, Object> getUserPrimaryProject() {
        UserPreferredSetting userPreferredSetting = userPreferredSettingRepository.findByUserAndKey(AppConfig.getCurrentUser(), JIRA_PRIMARY_PROJECT_SETTING_KEY);
        if (userPreferredSetting == null) {
            throw new BusinessException("Jira primary project not set. Please set it in your settings.");
        }
        try {
            return AppUtils.getObjectMapper().readValue(userPreferredSetting.getValue(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String validateAndGetNewAccessToken(UserOAuthProvider userOAuthProvider) {
        String accessToken = selfRef.getNewAccessToken(userOAuthProvider);
        if (StringUtils.isEmpty(accessToken)) {
            throw new RuntimeException("Failed to refresh Jira access token");
        }
        return accessToken;
    }

    @Transactional
    public String getNewAccessToken(UserOAuthProvider userOAuthProvider) {
        String accessToken = null;
        try {
            accessToken = oAuthProviderFactory.getProvider(userOAuthProvider.getProvider()).refreshOAuthProviderCredentials(userOAuthProvider);
        } catch (Exception e) {
            LOGGER.error("Error while refreshing access token", e);
            userOAuthProvider.setRevoked(true);
            userOAuthProviderRepository.save(userOAuthProvider);
        }
        return accessToken;
    }

    private String getJiraTasksSearchCondition() {
        String maxDate = String.valueOf(LocalDate.now().minusMonths(3).withDayOfMonth(1));
        return "assignee=currentUser() AND (resolution IS EMPTY OR (resolutiondate >= '" + maxDate + "' AND timespent > 0)) ORDER BY created DESC";
    }

    public List<AccessibleResourceDTO> getUserSites(User user) {
        UserOAuthProvider userOAuthProvider = validateJiraOAuthProvider();
        String cacheKey = USER_JIRA_SITES_FETCH_CACHE_KEY_PREFIX + user.getId();
        if (redisTemplate.hasKey(cacheKey)) {
            try {
                return (List<AccessibleResourceDTO>) redisTemplate.opsForValue().get(cacheKey);
            } catch (Exception e) {
                LOGGER.error("Error while fetching cached Jira accessible resources for user with id: {}", user.getId(), e);
                throw new RuntimeException("Something went wrong while fetching Jira accessible resources");
            }
        }

        String accessToken;
        try {
            accessToken = userOAuthProvider.isExpired() ? validateAndGetNewAccessToken(userOAuthProvider) : trackeraHasher.decryptFromBase64(userOAuthProvider.getAccessToken(), false);
        } catch (Exception e) {
            LOGGER.error("Error while getting jira access token", e);
            throw new RuntimeException("Failed to fetch jira accessible resources");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        List<AccessibleResourceDTO> resources = Arrays.asList(callJiraApi("https://api.atlassian.com/oauth/token/accessible-resources", HttpMethod.GET, entity, AccessibleResourceDTO[].class, userOAuthProvider));
        redisTemplate.opsForValue().set(cacheKey, resources, Duration.ofMinutes(JIRA_SITES_FETCH_MINUTES_DURATION));
        return resources;
    }

    @Retryable(retryFor = Exception.class, backoff = @Backoff(delay = 1000, multiplier = 3))
    private <T> T callJiraApi(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType, UserOAuthProvider userOAuthProvider) {
        try {
            ResponseEntity<T> response = AppUtils.getRestTemplate().exchange(url, method, entity, responseType);

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                String newAccessToken = validateAndGetNewAccessToken(userOAuthProvider);

                HttpHeaders newHeaders = new HttpHeaders();
                newHeaders.putAll(entity.getHeaders());
                newHeaders.setBearerAuth(newAccessToken);

                HttpEntity<?> newEntity = new HttpEntity<>(newHeaders);

                response = AppUtils.getRestTemplate().exchange(url, method, newEntity, responseType);
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
