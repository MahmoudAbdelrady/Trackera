package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.notification.NotificationDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSelectionDTO;
import com.mdevs.trackera.dto.worklog.*;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.job.handlers.WorkLogSyncJobHandler;
import com.mdevs.trackera.shared.enums.OAuthProvider;
import com.mdevs.trackera.repository.WorkLogDetailRepository;
import com.mdevs.trackera.repository.WorkLogRepository;
import com.mdevs.trackera.shared.FileHandler;
import com.mdevs.trackera.shared.WorkLogQueryBuilder;
import com.mdevs.trackera.shared.enums.UserPreferenceOption;
import com.mdevs.trackera.shared.enums.WorkLogColumn;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.enums.WorklogSyncOperation;
import com.mdevs.trackera.shared.enums.WorkLogSyncMessageType;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.exceptions.types.NotFoundException;
import com.mdevs.trackera.shared.exceptions.types.UnauthorizedException;
import com.mdevs.trackera.shared.DurationFormatter;
import com.mdevs.trackera.shared.mappers.WorkLogMapper;
import com.mdevs.trackera.utils.DateTimeUtil;
import com.mdevs.trackera.utils.JsonUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkLogService {
    private final WorkLogRepository workLogRepository;

    private final WorkLogDetailRepository workLogDetailRepository;

    private final UserPreferenceService userPreferenceService;

    private final OAuthConnectionService oAuthConnectionService;

    private final BackgroundJobService backgroundJobService;

    private final NotificationService notificationService;

    private final WorkLogMapper workLogMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)h)?\\s*(?:(\\d+)m)?");

    public static final String WORKLOG_SYNC_STATUS_EVENT_NAME = "worklog-sync-status";

    //<editor-fold desc="Search & Retrieval">
    public Page<WorkLogInfoDTO> searchAllWorkLogs(WorkLogSearchFilterDTO searchFilterDTO, Pageable pageable) {
        Map<String, Object> queryParameters = new HashMap<>();
        String searchQuery = buildSearchQuery(searchFilterDTO, queryParameters);

        TypedQuery<WorkLog> typedQuery = entityManager.createQuery(searchQuery, WorkLog.class);
        queryParameters.forEach(typedQuery::setParameter);
        typedQuery.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<WorkLog> workLogs = typedQuery.getResultList();
        List<WorkLogInfoDTO> workLogInfoDTOList = workLogs.stream().map(workLog -> {
            WorkLogInfoDTO workLogInfoDTO = workLogMapper.toDto(workLog);
            workLogInfoDTO.setId(workLog.getUuid());
            workLogInfoDTO.setTotalTime(DurationFormatter.formatDuration(workLog.getTotalMinutes(), true));
            return workLogInfoDTO;
        }).toList();

        List<String> logsWithErrors = workLogDetailRepository.findWorkLogUuidsWithSyncErrors(workLogs.stream().map(WorkLog::getId).collect(Collectors.toList()));
        workLogInfoDTOList.stream().filter(worklogInfo -> logsWithErrors.contains(worklogInfo.getId())).forEach(worklogInfo -> worklogInfo.setHasError(true));

        String countQueryStr = searchQuery.replaceFirst("SELECT wl FROM WorkLog wl", "SELECT COUNT(wl) FROM WorkLog wl");
        TypedQuery<Long> countQuery = entityManager.createQuery(countQueryStr, Long.class);
        queryParameters.forEach(countQuery::setParameter);
        Long totalRecords = countQuery.getSingleResult();

        return new PageImpl<>(workLogInfoDTOList, pageable, totalRecords);
    }

    public WorkLogInfoDTO getWorkLogByUUID(String uuid) {
        WorkLog workLog = ensureWorkLogExistsAndHasPermission(uuid);
        return buildWorkLogInfoDTO(workLog);
    }

    public List<WorkLogTaskDTO> getWorkLogTasks(String uuid) {
        WorkLog workLog = ensureWorkLogExistsAndHasPermission(uuid);
        return buildWorkLogTaskDTOs(workLog);
    }

    public List<WorkLogEntryDTO> getWorkLogTaskEntries(String uuid, String taskName) {
        WorkLog workLog = ensureWorkLogExistsAndHasPermission(uuid);
        List<WorkLogEntryDTO> entries = buildWorkLogEntryDTOs(workLog, taskName);
        if (entries.isEmpty()) {
            throw new NotFoundException("No logs found for the specified task in this worklog");
        }
        return entries;
    }
    //</editor-fold>

    //<editor-fold desc="Summary & Analytics">
    public WorklogSummaryResultDTO getCurrentMonthSummary() {
        User currentUser = AppConfig.getAuthenticatedCurrentUser();
        LocalDate now = LocalDate.now();
        LocalDate previousMonth = now.minusMonths(1);
        DecimalFormat durationDecimalFormat = DurationFormatter.getDurationDecimalFormat();
        int totalLogged = workLogRepository.sumTotalMinutesByUserAndWorkDateBetween(currentUser, now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
        int targetMinutes = (Integer) userPreferenceService.getPreferenceValue(currentUser, UserPreferenceOption.WORKLOGS_MONTHLY_TARGET_HOURS) * 60;
        int remainingMinutes = Math.max(targetMinutes - totalLogged, 0);
        int previousMonthLoggedHours = workLogRepository.sumTotalMinutesByUserAndWorkDateBetween(currentUser, previousMonth.withDayOfMonth(1), previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()));
        List<WorkLogSummaryDTO> summaryDTOS = List.of(
                new WorkLogSummaryDTO("Logged Hours", "Equivalent to " + DurationFormatter.formatDuration(totalLogged, true), "logged", durationDecimalFormat.format(totalLogged / 60.0)),
                new WorkLogSummaryDTO("Target Hours", "Equivalent to " + DurationFormatter.formatDuration(targetMinutes, true), "target", durationDecimalFormat.format(targetMinutes / 60.0)),
                new WorkLogSummaryDTO("Remaining Hours", "Equivalent to " + DurationFormatter.formatDuration(remainingMinutes, true), "remaining", durationDecimalFormat.format(remainingMinutes / 60.0))
        );
        return new WorklogSummaryResultDTO(summaryDTOS, durationDecimalFormat.format(previousMonthLoggedHours / 60.0));
    }
    //</editor-fold>

    //<editor-fold desc="Creation & Update">
    @Transactional
    public Map<String, Object> addWorkLog(ManageWorkLogDTO manageWorkLogDTO, MultipartFile worklogFile) {
        validateWorkLog(manageWorkLogDTO, null);

        Map<String, Object> processResult = processWorkLogFile(worklogFile);
        if (processResult.containsKey("isError")) {
            return processResult;
        }
        WorkLog workLog = saveWorkLog(manageWorkLogDTO, (int) processResult.get("totalMinutes"));
        List<WorkLogDetail> workLogDetails =  saveWorkLogDetails((List<WorkLogDetail>) processResult.get("workLogDetails"), workLog);
        if (manageWorkLogDTO.isSyncToJira()) {
            oAuthConnectionService.validateAndGetConnection(workLog.getUser(), OAuthProvider.JIRA);
            workLog.setStatus(WorkLogStatus.IN_QUEUE);
            workLogRepository.save(workLog);
            markDetailsForSync(workLogDetails);

            WorkLogSyncPayloadDTO workLogSyncPayloadDTO = new WorkLogSyncPayloadDTO(workLog.getUser().getId(), workLog.getUuid());
            List<WorkLogDetailSyncRequestDTO> detailsToSync = workLogDetails.stream().map(detail -> new WorkLogDetailSyncRequestDTO(detail.getId(), detail.getTaskName(), null)).toList();
            workLogSyncPayloadDTO.setDetailsToSync(detailsToSync);
            backgroundJobService.enqueueJob(WorkLogSyncJobHandler.class, JsonUtil.convertObjectToJsonString(workLogSyncPayloadDTO));

            WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.WORKLOG, workLog.getUuid(), null, null, workLog.getStatus(), null);
            notificationService.sendNotification(new NotificationDTO(AppConfig.getAuthenticatedCurrentUser().getUuid(), WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
        }
        return Map.of("message", "Worklog uploaded successfully");
    }

    @Transactional
    public Map<String, Object> updateWorkLog(String uuid, ManageWorkLogDTO manageWorkLogDTO, MultipartFile worklogFile) {
        WorkLog workLog = ensureWorkLogExistsAndHasPermission(uuid);
        ensureWorkLogSyncNotInProgress(workLog);
        validateWorkLog(manageWorkLogDTO, workLog.getId());

        WorkLogSyncPayloadDTO workLogSyncPayloadDTO = new WorkLogSyncPayloadDTO(workLog.getUser().getId(), workLog.getUuid());
        if (worklogFile != null) {
            Map<String, Object> processErrors = handleWorkLogFileUpdate(workLog, worklogFile, workLogSyncPayloadDTO);
            if (processErrors != null) return processErrors;
        }

        Set<WorkLogDetail> detailsToSync = new HashSet<>();
        if (manageWorkLogDTO.isSyncToJira()) {
            detailsToSync.addAll(workLogDetailRepository.findByWorkLogAndStatus(workLog, WorkLogStatus.NOT_SYNCED));
        }
        if (!workLog.getWorkDate().equals(manageWorkLogDTO.getLogDate())) {
            detailsToSync.addAll(workLogDetailRepository.findByWorkLogAndStatus(workLog, WorkLogStatus.SYNCED));
        }

        if (!detailsToSync.isEmpty()) {
            workLog.setStatus(WorkLogStatus.IN_QUEUE);
            markDetailsForSync(new ArrayList<>(detailsToSync));
            List<WorkLogDetailSyncRequestDTO> detailSyncRequests = detailsToSync.stream().map(detail -> new WorkLogDetailSyncRequestDTO(detail.getId(), detail.getTaskName(), null)).toList();
            workLogSyncPayloadDTO.setDetailsToSync(detailSyncRequests);

            WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.WORKLOG, workLog.getUuid(), null, null, workLog.getStatus(), null);
            notificationService.sendNotification(new NotificationDTO(AppConfig.getAuthenticatedCurrentUser().getUuid(), WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
        }

        handleUpdateMetaData(manageWorkLogDTO, workLog);

        if (workLogSyncPayloadDTO.hasWork()) {
            oAuthConnectionService.validateAndGetConnection(workLog.getUser(), OAuthProvider.JIRA);
            backgroundJobService.enqueueJob(WorkLogSyncJobHandler.class, JsonUtil.convertObjectToJsonString(workLogSyncPayloadDTO));
        }

        return Map.of("message", "Worklog updated successfully", "worklog", buildWorkLogInfoDTO(workLog));
    }

    @Transactional
    public UpdateWorkLogDetailResponseDTO updateWorkLogDetail(String uuid, UpdateWorkLogDetailPayloadDTO payload) {
        WorkLog workLog = ensureWorkLogExistsAndHasPermission(uuid);
        ensureWorkLogSyncNotInProgress(workLog);
        validateWorkLogDetail(payload);

        DetailUpdateResult result = payload.getIsTask() ? updateTaskDetails(workLog, payload) : updateEntryDetail(payload);

        WorkLogSyncPayloadDTO syncPayload = new WorkLogSyncPayloadDTO(workLog.getUser().getId(), workLog.getUuid());
        prepareSyncPayload(syncPayload, payload, result);

        workLog.setStatus(workLogRepository.calculateWorkLogStatus(workLog));
        workLogRepository.save(workLog);

        if (syncPayload.hasWork()) {
            oAuthConnectionService.validateAndGetConnection(workLog.getUser(), OAuthProvider.JIRA);
            backgroundJobService.enqueueJob(WorkLogSyncJobHandler.class, JsonUtil.convertObjectToJsonString(syncPayload));
        }

        return buildUpdateResponse(workLog, payload, result);
    }

    private void validateWorkLogDetail(UpdateWorkLogDetailPayloadDTO detailPayloadDTO) {
        WorkLogDetailNewDataDTO detailNewDataDTO = detailPayloadDTO.getNewData();
        if (detailNewDataDTO == null) {
            throw new BusinessException("New updated data is required");
        }
        if (StringUtils.isEmpty(detailNewDataDTO.getName())) {
            throw new BusinessException("WorkLog new task name is required");
        }

        if (detailPayloadDTO.getIsTask()) {
            if (StringUtils.isEmpty(detailPayloadDTO.getTaskName())) {
                throw new BusinessException("Task name is required");
            }
        } else {
            if (StringUtils.isEmpty(detailPayloadDTO.getEntryId())) {
                throw new BusinessException("WorkLog entry id is required");
            }

            if (detailNewDataDTO.getStartTime() == null) {
                throw new BusinessException("WorkLog new start time is required");
            }

            if (detailNewDataDTO.getEndTime() == null) {
                throw new BusinessException("WorkLog new end time is required");
            }

            if (StringUtils.isEmpty(detailNewDataDTO.getDuration())) {
                throw new BusinessException("WorkLog new duration is required");
            }

            if (StringUtils.isEmpty(detailNewDataDTO.getDescription())) {
                throw new BusinessException("WorkLog new description is required");
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Deletion">
    @Transactional
    public Map<String, Object> deleteWorkLog(String uuid, WorkLogSelectionDTO workLogSelectionDTO) {
        WorkLog workLog = ensureWorkLogExistsAndHasPermission(uuid);
        ensureWorkLogSyncNotInProgress(workLog);
        WorkLogSyncPayloadDTO workLogSyncPayloadDTO = new WorkLogSyncPayloadDTO(workLog.getUser().getId());

        List<WorkLogDetail> detailsToDelete = getWorkLogDetailsBySelection(workLog, workLogSelectionDTO, null);
        List<WorkLogDetailSyncRequestDTO> detailsToUnsync = detailsToDelete.stream()
                .filter(detail -> detail.getStatus().equals(WorkLogStatus.SYNCED))
                .map(detail -> new WorkLogDetailSyncRequestDTO(detail.getTaskName(), detail.getJiraId())).toList();

        if (!detailsToUnsync.isEmpty()) {
            oAuthConnectionService.validateAndGetConnection(workLog.getUser(), OAuthProvider.JIRA);
            workLogSyncPayloadDTO.setDetailsToUnsync(detailsToUnsync);
            backgroundJobService.enqueueJob(WorkLogSyncJobHandler.class, JsonUtil.convertObjectToJsonString(workLogSyncPayloadDTO));
        }
        workLogDetailRepository.deleteAll(detailsToDelete);

        Map<String, Object> result = new HashMap<>();
        if (!workLogDetailRepository.existsByWorkLog(workLog)) {
            workLogRepository.delete(workLog);
            result.put("isLast", true);
            result.put("message", "Worklog deleted successfully");
        } else {
            int totalDeletedMinutes = detailsToDelete.stream().mapToInt(WorkLogDetail::getDuration).sum();
            workLog.setTotalMinutes(workLog.getTotalMinutes() - totalDeletedMinutes);
            workLog.setStatus(workLogRepository.calculateWorkLogStatus(workLog));
            workLogRepository.save(workLog);
            result.put("message", "Selected logs deleted successfully");

            Set<String> deletedTaskNames = detailsToDelete.stream().map(WorkLogDetail::getTaskName).collect(Collectors.toSet());
            if (deletedTaskNames.size() == 1 && !workLogDetailRepository.existsByWorkLogAndTaskName(workLog, deletedTaskNames.iterator().next())) {
                result.put("isLastOfTask", true);
            }
        }

        return result;
    }

    @Transactional
    public long deleteDeprecatedWithBatch(long maxId, int pageSize) {
        List<Long> deprecatedWorkLogsIds = workLogRepository.findByWorkDateLessThanEqualAndIdGreaterThanOrderById(AppConfig.getMinQueryableDate(), maxId, Pageable.ofSize(pageSize));
        if (!deprecatedWorkLogsIds.isEmpty()) {
            workLogDetailRepository.deleteByWorkLogIn(deprecatedWorkLogsIds);
            workLogRepository.deleteAllByIdInBatch(deprecatedWorkLogsIds);
            return deprecatedWorkLogsIds.getLast();
        }
        return -1;
    }
    //</editor-fold>

    //<editor-fold desc="Jira Synchronization">
    @Transactional
    public void performJiraSync(String uuid, WorkLogSelectionDTO workLogSelectionDTO, WorklogSyncOperation syncOperation) {
        oAuthConnectionService.validateAndGetConnection(AppConfig.getAuthenticatedCurrentUser(), OAuthProvider.JIRA);
        WorkLog workLog = ensureWorkLogExistsAndHasPermission(uuid);
        validateSyncRequest(workLog, syncOperation);
        List<WorkLogDetail> workLogDetails = getWorkLogDetailsBySelection(workLog, workLogSelectionDTO, syncOperation.equals(WorklogSyncOperation.UNSYNC) ? WorkLogStatus.SYNCED : WorkLogStatus.NOT_SYNCED);
        workLog.setStatus(WorkLogStatus.IN_QUEUE);
        workLogRepository.save(workLog);
        handleJiraSyncing(workLog, workLogDetails, syncOperation);
        handleWorkLogSyncNotifications(AppConfig.getAuthenticatedCurrentUser(), workLog, workLogDetails, WorkLogStatus.IN_QUEUE);
    }
    //</editor-fold>

    //<editor-fold desc="Internal Methods & Validations">
    private String buildSearchQuery(WorkLogSearchFilterDTO searchFilterDTO, Map<String, Object> queryParameters) {
        WorkLogQueryBuilder workLogQueryBuilder = new WorkLogQueryBuilder(AppConfig.getAuthenticatedCurrentUser().getId());
        if (searchFilterDTO != null) {
            searchFilterDTO.validate();
            workLogQueryBuilder
                    .withLogName(searchFilterDTO.getLogName())
                    .withDateRange(searchFilterDTO.getDateFrom(), searchFilterDTO.getDateTo())
                    .withTotalHours(searchFilterDTO.getTotalHours())
                    .withEvaluation(searchFilterDTO.getEvaluation())
                    .withStatus(searchFilterDTO.getStatus());
        }

        queryParameters.putAll(workLogQueryBuilder.getParameters());
        return workLogQueryBuilder.getQuery();
    }

    private void validateWorkLog(ManageWorkLogDTO manageWorkLogDTO, Long existingWorkLogId) {
        if (workLogRepository.existsByUserAndWorkDateAndWorkLogNot(AppConfig.getAuthenticatedCurrentUser(), manageWorkLogDTO.getLogDate(), existingWorkLogId)) {
            throw new BusinessException("WorkLog for the date " + manageWorkLogDTO.getLogDate() + " already exists.");
        }
        if (manageWorkLogDTO.getLogDate().isAfter(LocalDate.now())) {
            throw new BusinessException("WorkLog date cannot be in the future.");
        }
        if (manageWorkLogDTO.getLogDate().isBefore(AppConfig.getMinQueryableDate())) {
            throw new BusinessException("WorkLog date cannot be before " + AppConfig.getMinQueryableDate() + ".");
        }
    }

    private Map<String, Object> handleWorkLogFileUpdate(WorkLog workLog, MultipartFile worklogFile, WorkLogSyncPayloadDTO syncPayloadDTO) {
        Map<String, Object> processResult = processWorkLogFile(worklogFile);
        if (processResult.containsKey("isError")) {
            return processResult;
        }
        workLog.setTotalMinutes((int) processResult.get("totalMinutes"));

        List<WorkLogDetailSyncRequestDTO> detailsToUnsync = workLogDetailRepository.findByWorkLogAndStatus(workLog, WorkLogStatus.SYNCED).stream()
                .map(detail -> new WorkLogDetailSyncRequestDTO(detail.getTaskName(), detail.getJiraId())).toList();

        if (!detailsToUnsync.isEmpty()) {
            workLog.setStatus(WorkLogStatus.IN_QUEUE);
            syncPayloadDTO.setDetailsToUnsync(detailsToUnsync);

            WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.WORKLOG, workLog.getUuid(), null, null, workLog.getStatus(), null);
            notificationService.sendNotification(new NotificationDTO(AppConfig.getAuthenticatedCurrentUser().getUuid(), WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
        }
        workLogDetailRepository.deleteAllByWorkLog(workLog);

        saveWorkLogDetails((List<WorkLogDetail>) processResult.get("workLogDetails"), workLog);
        return null;
    }

    private Map<String, Object> processWorkLogFile(MultipartFile worklogFile) {
        List<Map<WorkLogColumn, String>> parsedData = FileHandler.validateAndParse(worklogFile);

        if (parsedData.isEmpty()) {
            throw new BusinessException("The uploaded file is empty or does not contain any valid data.");
        }

        List<WorkLogDetail> allWorkLogDetails = new ArrayList<>();
        List<Map<String, Object>> rowErrors = new ArrayList<>();
        int totalMinutes = 0;
        for (Map<WorkLogColumn, String> row : parsedData) {
            totalMinutes += Optional.ofNullable(processWorkLogRow(row, allWorkLogDetails, rowErrors)).orElse(0);
        }

        Map<String, Object> result = new HashMap<>();
        if (!rowErrors.isEmpty()) {
            result.put("isError", true);
            result.put("message", "There were some errors in the uploaded file.");
            result.put("errors", rowErrors);
        } else {
            result.put("totalMinutes", totalMinutes);
            result.put("workLogDetails", allWorkLogDetails);
        }
        return result;
    }

    private Integer processWorkLogRow(Map<WorkLogColumn, String> row, List<WorkLogDetail> allWorkLogDetails, List<Map<String, Object>> rowErrors) {
        StringJoiner rowErrorMessages = new StringJoiner("; ");

        String taskName = validateAndGetCell(row, WorkLogColumn.TASK_NAME, rowErrorMessages);
        LocalTime fromHour = validateAndGetCell(row, WorkLogColumn.FROM_HOUR, rowErrorMessages);
        LocalTime toHour = validateAndGetCell(row, WorkLogColumn.TO_HOUR, rowErrorMessages);
        Integer taskLogDuration = validateAndGetCell(row, WorkLogColumn.DURATION, this::parseDuration, rowErrorMessages);
        String taskDescription = validateAndGetCell(row, WorkLogColumn.DESCRIPTION, rowErrorMessages);

        if (rowErrorMessages.length() > 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("row", row.get(WorkLogColumn.ROW_NUMBER));
            error.put("error", rowErrorMessages.toString());
            rowErrors.add(error);
            return null;
        }

        int taskLogDurationValue = taskLogDuration != null ? taskLogDuration : 0;

        WorkLogDetail workLogDetail = new WorkLogDetail();
        workLogDetail.setTaskName(taskName);
        workLogDetail.setStartTime(fromHour);
        workLogDetail.setEndTime(toHour);
        workLogDetail.setDuration(taskLogDurationValue);
        workLogDetail.setDescription(taskDescription);
        allWorkLogDetails.add(workLogDetail);

        return taskLogDurationValue;
    }

    private <T> T validateAndGetCell(Map<WorkLogColumn, String> row, WorkLogColumn columnType, StringJoiner errorMessages) {
        return validateAndGetCell(row, columnType, null, errorMessages);
    }

    private <T, R> R validateAndGetCell(Map<WorkLogColumn, String> row, WorkLogColumn columnType, Function<T, R> additionalParser, StringJoiner errorMessages) {
        try {
            T parsedCell = (T) parseCell(row.get(columnType), columnType.getLabel(), columnType.getResultType());
            return additionalParser != null ? additionalParser.apply(parsedCell) : (R) parsedCell;
        } catch (Exception e) {
            errorMessages.add(e.getMessage());
            return null;
        }
    }

    private <T> T parseCell(String cell, String cellName, Class<T> expectedType) {
        if (StringUtils.isEmpty(cell)) {
            throw new BusinessException("[" + cellName + "] cell is empty");
        }

        Object result = cell;

        if (expectedType == LocalTime.class) {
            try {
                result = LocalTime.parse(cell, DateTimeUtil.getDateTime12hFormatter());
            } catch (Exception e) {
                throw new BusinessException("[" + cellName + "] Invalid time format. Expected format is h:mm AM/PM");
            }
        }

        try {
            return expectedType.cast(result);
        } catch (Exception e) {
            throw new BusinessException("[" + cellName + "] cell type is not supported");
        }
    }

    private int parseDuration(String duration) {
        Matcher matcher = DURATION_PATTERN.matcher(duration);
        int hours = 0, minutes = 0;

        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                hours = Integer.parseInt(matcher.group(1));
            }
            if (matcher.group(2) != null) {
                minutes = Integer.parseInt(matcher.group(2));
            }
        } else {
            throw new BusinessException("[" + WorkLogColumn.DURATION.getLabel() + "] Invalid format: '" + duration + "'. Expected format is 'Xh Ym' (e.g., '2h 30m')");
        }

        return (hours * 60) + minutes;
    }

    private WorkLog saveWorkLog(ManageWorkLogDTO manageWorkLogDTO, int totalMinutes) {
        try {
            WorkLog workLog = new WorkLog();
            workLog.setUser(AppConfig.getAuthenticatedCurrentUser());
            workLog.setTotalMinutes(totalMinutes);
            workLog.setWorkDate(manageWorkLogDTO.getLogDate());
            if (!StringUtils.isEmpty(manageWorkLogDTO.getLogName())) {
                workLog.setName(manageWorkLogDTO.getLogName());
            } else {
                DayOfWeek dayOfWeek = manageWorkLogDTO.getLogDate().getDayOfWeek();
                String dayName = dayOfWeek.name().substring(0, 1).toUpperCase() + dayOfWeek.name().substring(1).toLowerCase();
                String formattedDate = DateTimeUtil.getCompactedDateFormatter().format(manageWorkLogDTO.getLogDate());
                workLog.setName("Worklog - " + dayName + formattedDate);
            }

            return workLogRepository.save(workLog);
        } catch (Exception e) {
            log.error("Error saving worklog", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private List<WorkLogDetail> saveWorkLogDetails(List<WorkLogDetail> allWorkLogDetails, WorkLog workLog) {
        try {
            allWorkLogDetails.forEach(logDetail -> logDetail.setWorkLog(workLog));
            return workLogDetailRepository.saveAll(allWorkLogDetails);
        } catch (Exception exception) {
            log.error("Error saving worklog details", exception);
            throw new RuntimeException(exception.getMessage());
        }
    }

    private WorkLog ensureWorkLogExistsAndHasPermission(String uuid) {
        WorkLog workLog = workLogRepository.findByUserAndUuid(AppConfig.getAuthenticatedCurrentUser(), uuid);
        if (workLog == null) {
            throw new NotFoundException("WorkLog not found");
        }
        if (!workLog.getUser().getId().equals(AppConfig.getAuthenticatedCurrentUser().getId())) {
            throw new UnauthorizedException("You are not authorized to access this worklog");
        }
        return workLog;
    }

    private void validateSyncRequest(WorkLog workLog, WorklogSyncOperation syncOperation) {
        if (syncOperation.equals(WorklogSyncOperation.RESYNC)) {
          throw new BusinessException("Unsupported sync operation");
        } else if (syncOperation.equals(WorklogSyncOperation.SYNC) && workLog.getStatus().equals(WorkLogStatus.SYNCED)) {
            throw new BusinessException("WorkLog is already synced to Jira");
        } else if (syncOperation.equals(WorklogSyncOperation.UNSYNC) && workLog.getStatus().equals(WorkLogStatus.NOT_SYNCED)) {
            throw new BusinessException("WorkLog is not synced to Jira");
        } else {
            ensureWorkLogSyncNotInProgress(workLog);
        }
    }

    private void ensureWorkLogSyncNotInProgress(WorkLog workLog) {
        if (workLog.getStatus().equals(WorkLogStatus.IN_QUEUE) || workLog.getStatus().equals(WorkLogStatus.SYNC_IN_PROGRESS) || workLog.getStatus().equals(WorkLogStatus.UNSYNC_IN_PROGRESS)) {
            throw new BusinessException("WorkLog synchronization is already in progress");
        }
    }

    private List<WorkLogDetail> getWorkLogDetailsBySelection(WorkLog workLog, WorkLogSelectionDTO workLogSelectionDTO, WorkLogStatus status) {
        List<WorkLogDetail> workLogDetails;
        if (workLogSelectionDTO != null && workLogSelectionDTO.getTaskNames() != null && !workLogSelectionDTO.getTaskNames().isEmpty()) {
            workLogDetails = workLogDetailRepository.findByWorkLogAndTaskNameInAndStatus(workLog, workLogSelectionDTO.getTaskNames(), status);
        } else if (workLogSelectionDTO != null && workLogSelectionDTO.getEntryIds() != null && !workLogSelectionDTO.getEntryIds().isEmpty()) {
            workLogDetails = workLogDetailRepository.findByWorkLogAndUuidInAndStatus(workLog, workLogSelectionDTO.getEntryIds(), status);
        } else {
            workLogDetails = workLogDetailRepository.findAllByWorkLogAndStatus(workLog, status);
        }

        if (workLogDetails.isEmpty()) {
            throw new NotFoundException("Logs not found");
        }

        return workLogDetails;
    }

    private void handleJiraSyncing(WorkLog workLog, List<WorkLogDetail> workLogDetails, WorklogSyncOperation syncOperation) {
        markDetailsForSync(workLogDetails);
        WorkLogSyncPayloadDTO workLogSyncPayloadDTO = new WorkLogSyncPayloadDTO(workLog.getUser().getId(), workLog.getUuid());
        List<WorkLogDetailSyncRequestDTO> detailSyncRequests = workLogDetails.stream()
                .map(detail -> new WorkLogDetailSyncRequestDTO(detail.getId(), detail.getTaskName(), detail.getJiraId()))
                .toList();
        if (syncOperation.equals(WorklogSyncOperation.UNSYNC)) {
            workLogSyncPayloadDTO.setDetailsToUnsync(detailSyncRequests);
        } else {
            workLogSyncPayloadDTO.setDetailsToSync(detailSyncRequests);
        }
        backgroundJobService.enqueueJob(WorkLogSyncJobHandler.class, JsonUtil.convertObjectToJsonString(workLogSyncPayloadDTO));
    }

    private void markDetailsForSync(List<WorkLogDetail> workLogDetails) {
        workLogDetails.forEach(detail -> detail.setStatus(WorkLogStatus.IN_QUEUE));
        workLogDetailRepository.saveAll(workLogDetails);
    }

    private void handleWorkLogSyncNotifications(User syncUser, WorkLog workLog, List<WorkLogDetail> workLogDetails, WorkLogStatus status) {
        Set<String> taskNames = workLogDetails.stream().map(WorkLogDetail::getTaskName).collect(Collectors.toSet());
        Set<String> detailIds = workLogDetails.stream().map(WorkLogDetail::getUuid).collect(Collectors.toSet());

        WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.ALL, workLog.getUuid(), new ArrayList<>(taskNames), new ArrayList<>(detailIds), status, null);
        notificationService.sendNotification(new NotificationDTO(syncUser.getUuid(), WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
    }

    private void handleUpdateMetaData(ManageWorkLogDTO manageWorkLogDTO, WorkLog workLog) {
        if (!StringUtils.isEmpty(manageWorkLogDTO.getLogName())) {
            workLog.setName(manageWorkLogDTO.getLogName());
        }
        workLog.setWorkDate(manageWorkLogDTO.getLogDate());
        workLogRepository.save(workLog);
    }

    private DetailUpdateResult updateTaskDetails(WorkLog workLog, UpdateWorkLogDetailPayloadDTO payload) {
        List<WorkLogDetail> workLogDetails = workLogDetailRepository.findByWorkLogAndTaskName(workLog, payload.getTaskName());
        if (workLogDetails.isEmpty()) {
            throw new NotFoundException("WorkLog task not found");
        }

        String oldTaskName = payload.getTaskName();
        boolean taskChanged = !oldTaskName.equals(payload.getNewData().getName());
        List<WorkLogDetail> detailsToUnsync = new ArrayList<>();

        workLogDetails.forEach(detail -> {
            detail.setTaskName(payload.getNewData().getName());
            if(taskChanged && !StringUtils.isEmpty(detail.getSyncError())) {
                detail.setSyncError(null);
            }
            if (detail.getStatus().equals(WorkLogStatus.SYNCED)) {
                detailsToUnsync.add(detail);
            }
        });

        workLogDetailRepository.saveAll(workLogDetails);
        return new DetailUpdateResult(workLogDetails, detailsToUnsync, null, oldTaskName, taskChanged);
    }

    private DetailUpdateResult updateEntryDetail(UpdateWorkLogDetailPayloadDTO payload) {
        WorkLogDetail workLogDetail = workLogDetailRepository.findByUuid(payload.getEntryId());
        if (workLogDetail == null) {
            throw new NotFoundException("WorkLog entry not found");
        }

        String oldTaskName = workLogDetail.getTaskName();
        boolean taskChanged = !oldTaskName.equals(payload.getNewData().getName());
        List<WorkLogDetail> detailsToUnsync = new ArrayList<>();

        WorkLogDetailNewDataDTO detailNewData = payload.getNewData();
        workLogDetail.setTaskName(StringUtils.isEmpty(detailNewData.getName()) ? workLogDetail.getTaskName() : detailNewData.getName());
        workLogDetail.setStartTime(detailNewData.getStartTime());
        workLogDetail.setEndTime(detailNewData.getEndTime());
        workLogDetail.setDuration(parseDuration(detailNewData.getDuration()));
        workLogDetail.setDescription(detailNewData.getDescription());
        if (workLogDetail.getStatus().equals(WorkLogStatus.SYNCED)) {
            detailsToUnsync.add(workLogDetail);
        }
        if(taskChanged && !StringUtils.isEmpty(workLogDetail.getSyncError())) {
            workLogDetail.setSyncError(null);
        }

        workLogDetailRepository.save(workLogDetail);
        return new DetailUpdateResult(List.of(workLogDetail), detailsToUnsync, workLogDetail, oldTaskName, taskChanged);
    }

    private void prepareSyncPayload(WorkLogSyncPayloadDTO syncPayload, UpdateWorkLogDetailPayloadDTO payload, DetailUpdateResult result) {
        if (payload.isSyncToJira()) {
            List<WorkLogDetail> detailsToSync = result.getUpdatedDetails();
            markDetailsForSync(Stream.concat(detailsToSync.stream(), result.getDetailsToUnsync().stream()).toList());

            Set<Long> unsyncDetailIds = result.getDetailsToUnsync().stream().map(WorkLogDetail::getId).collect(Collectors.toSet());
            List<WorkLogDetail> resyncDetails = detailsToSync.stream().filter(d -> unsyncDetailIds.contains(d.getId())).toList();
            List<WorkLogDetail> syncOnlyDetails = detailsToSync.stream().filter(d -> !unsyncDetailIds.contains(d.getId())).toList();
            Set<Long> resyncIds = resyncDetails.stream().map(WorkLogDetail::getId).collect(Collectors.toSet());
            List<WorkLogDetail> unsyncOnlyDetails = result.getDetailsToUnsync().stream().filter(d -> !resyncIds.contains(d.getId())).toList();

            if (!resyncDetails.isEmpty()) {
                syncPayload.setDetailsToReSync(resyncDetails.stream()
                        .map(detail -> {
                            WorkLogDetailSyncRequestDTO dto = new WorkLogDetailSyncRequestDTO(detail.getId(), detail.getTaskName(), detail.getJiraId());
                            dto.setOldTaskName(result.getOldTaskName());
                            return dto;
                        })
                        .toList());
            }
            if (!unsyncOnlyDetails.isEmpty()) {
                syncPayload.setDetailsToUnsync(unsyncOnlyDetails.stream()
                        .map(detail -> new WorkLogDetailSyncRequestDTO(detail.getId(), detail.getTaskName(), detail.getJiraId()))
                        .toList());
            }
            if (!syncOnlyDetails.isEmpty()) {
                syncPayload.setDetailsToSync(syncOnlyDetails.stream()
                        .map(detail -> new WorkLogDetailSyncRequestDTO(detail.getId(), detail.getTaskName(), null))
                        .toList());
            }
        } else if (!result.getDetailsToUnsync().isEmpty()) {
            markDetailsForSync(result.getDetailsToUnsync());
            syncPayload.setDetailsToUnsync(result.getDetailsToUnsync().stream()
                    .map(detail -> new WorkLogDetailSyncRequestDTO(detail.getId(), detail.getTaskName(), detail.getJiraId()))
                    .toList());
        }
    }

    private UpdateWorkLogDetailResponseDTO buildUpdateResponse(WorkLog workLog, UpdateWorkLogDetailPayloadDTO payload, DetailUpdateResult result) {
        UpdateWorkLogDetailResponseDTO response = new UpdateWorkLogDetailResponseDTO((payload.getIsTask() ? "Task" : "Entry") + " updated successfully");
        response.setWorklogInfo(buildWorkLogInfoDTO(workLog));
        response.setCurrentTask(buildSingleWorkLogTaskDTO(workLog, result.getOldTaskName()));
        if (result.isTaskChanged()) {
            response.setNewTask(buildSingleWorkLogTaskDTO(workLog, payload.getNewData().getName()));
        }
        if (!payload.getIsTask()) {
            WorkLogEntryDTO workLogEntryDTO = buildWorkLogEntryDTO(result.getSingleDetail());
            workLogEntryDTO.setTaskChanged(result.isTaskChanged());
            response.setEntry(workLogEntryDTO);
        }
        return response;
    }

    private WorkLogInfoDTO buildWorkLogInfoDTO(WorkLog workLog) {
        WorkLogInfoDTO workLogInfoDTO = workLogMapper.toDto(workLog);
        workLogInfoDTO.setId(workLog.getUuid());
        workLogInfoDTO.setTotalTime(DurationFormatter.formatDuration(workLog.getTotalMinutes(), true));
        workLogInfoDTO.setHasError(!workLogDetailRepository.findWorkLogUuidsWithSyncErrors(List.of(workLog.getId())).isEmpty());
        return workLogInfoDTO;
    }

    private List<WorkLogTaskDTO> buildWorkLogTaskDTOs(WorkLog workLog) {
        List<Map<String, Object>> workLogDetailGroups = workLogDetailRepository.getGroupedWorkLogDetailsByWorkLog(workLog, null);
        return workLogDetailGroups.stream().map(this::mapGroupedWorkLogDetailToTaskDTO).toList();
    }

    private List<WorkLogEntryDTO> buildWorkLogEntryDTOs(WorkLog workLog, String taskName) {
        List<WorkLogDetail> workLogDetails = workLogDetailRepository.findByWorkLogAndTaskName(workLog, taskName);
        return workLogDetails.stream().map(this::buildWorkLogEntryDTO).toList();
    }

    private WorkLogTaskDTO buildSingleWorkLogTaskDTO(WorkLog workLog, String taskName) {
        WorkLogTaskDTO workLogTaskDTO = workLogDetailRepository.getGroupedWorkLogDetailsByWorkLog(workLog, taskName).stream().findFirst().map(this::mapGroupedWorkLogDetailToTaskDTO).orElse(null);
        if (workLogTaskDTO == null) {
            workLogTaskDTO = new WorkLogTaskDTO(taskName);
            workLogTaskDTO.setIsDeleted(true);
        }
        return workLogTaskDTO;
    }

    private WorkLogEntryDTO buildWorkLogEntryDTO(WorkLogDetail workLogDetail) {
        WorkLogEntryDTO workLogEntryDTO = new WorkLogEntryDTO();
        workLogEntryDTO.setId(workLogDetail.getUuid());
        workLogEntryDTO.setFromTime(DateTimeUtil.getDateTime12hFormatter().format(workLogDetail.getStartTime()));
        workLogEntryDTO.setToTime(DateTimeUtil.getDateTime12hFormatter().format(workLogDetail.getEndTime()));
        workLogEntryDTO.setDuration(DurationFormatter.formatDuration(workLogDetail.getDuration(), false));
        workLogEntryDTO.setDescription(workLogDetail.getDescription());
        workLogEntryDTO.setStatus(workLogDetail.getStatus());
        workLogEntryDTO.setSyncError(workLogDetail.getSyncError());
        return workLogEntryDTO;
    }

    private WorkLogTaskDTO mapGroupedWorkLogDetailToTaskDTO(Map<String, Object> worklogGroup) {
        WorkLogTaskDTO workLogTaskDTO = new WorkLogTaskDTO(worklogGroup.get("taskName").toString());
        Integer totalMinutes = Integer.parseInt(worklogGroup.get("totalMinutes").toString());
        workLogTaskDTO.setTotalHours(DurationFormatter.formatDuration(totalMinutes, false));
        workLogTaskDTO.setTotalMinutes(totalMinutes);
        workLogTaskDTO.setStatus(WorkLogStatus.valueOf(worklogGroup.get("status").toString()));
        workLogTaskDTO.setHasError(Boolean.parseBoolean(worklogGroup.get("hasError").toString()));
        return workLogTaskDTO;
    }
    //</editor-fold>
}
