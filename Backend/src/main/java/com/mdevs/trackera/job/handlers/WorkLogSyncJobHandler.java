package com.mdevs.trackera.job.handlers;

import com.mdevs.trackera.config.messaging.RabbitConfig;
import com.mdevs.trackera.dto.notification.NotificationDTO;
import com.mdevs.trackera.dto.worklog.WorkLogDetailSyncRequestDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSyncMessageDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSyncPayloadDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSyncResultDTO;
import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.repository.BackgroundJobRepository;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.repository.WorkLogDetailRepository;
import com.mdevs.trackera.repository.WorkLogRepository;
import com.mdevs.trackera.service.JiraService;
import com.mdevs.trackera.service.NotificationService;
import com.mdevs.trackera.service.WorkLogService;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.enums.WorkLogSyncMessageType;
import com.mdevs.trackera.shared.exceptions.types.JiraException;
import com.mdevs.trackera.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WorkLogSyncJobHandler implements BackgroundJobHandler {
    private final JiraService jiraService;

    private final NotificationService notificationService;

    private final UserRepository userRepository;

    private final WorkLogRepository workLogRepository;

    private final WorkLogDetailRepository workLogDetailRepository;

    private final BackgroundJobRepository backgroundJobRepository;

    private final WorkLogSyncJobHandler selfRef;

    public WorkLogSyncJobHandler(JiraService jiraService, NotificationService notificationService, UserRepository userRepository, WorkLogRepository workLogRepository,
                                 WorkLogDetailRepository workLogDetailRepository, BackgroundJobRepository backgroundJobRepository, @Lazy WorkLogSyncJobHandler selfRef) {
        this.jiraService = jiraService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.workLogRepository = workLogRepository;
        this.workLogDetailRepository = workLogDetailRepository;
        this.backgroundJobRepository = backgroundJobRepository;
        this.selfRef = selfRef;
    }

    @Override
    public void handle(BackgroundJob job) {
        WorkLogSyncPayloadDTO workLogSyncPayloadDTO = JsonUtil.convertJsonStringToObject(job.getPayload(), WorkLogSyncPayloadDTO.class);
        User syncUser = userRepository.findOne(workLogSyncPayloadDTO.getUserId());
        boolean isLastRetry = job.getRetryCount() >= RabbitConfig.MAX_RETRIES;

        WorkLogSyncResultDTO resultDTO = new WorkLogSyncResultDTO();
        if (!workLogSyncPayloadDTO.getDetailsToUnsync().isEmpty()) {
            resultDTO = processDetailsSyncingOperationV2(workLogSyncPayloadDTO, syncUser, false, isLastRetry);
        }

        if (StringUtils.isEmpty(resultDTO.getHardError()) && !workLogSyncPayloadDTO.getDetailsToSync().isEmpty()) {
            resultDTO = processDetailsSyncingOperationV2(workLogSyncPayloadDTO, syncUser, true, isLastRetry);
        }

        if (workLogSyncPayloadDTO.getWorkLogUuid() != null) {
            selfRef.updateWorkLogStatus(syncUser, workLogSyncPayloadDTO, resultDTO.hasError());
        }

        selfRef.updateJobPayload(job, workLogSyncPayloadDTO);

        if (!StringUtils.isEmpty(resultDTO.getHardError())) {
            throw new RuntimeException(resultDTO.getHardError());
        }
    }

    private WorkLogSyncResultDTO processDetailsSyncingOperationV2(WorkLogSyncPayloadDTO workLogSyncPayloadDTO, User syncUser, boolean isSync, boolean isLastRetry) {
        List<WorkLogDetailSyncRequestDTO> detailsList = isSync ? workLogSyncPayloadDTO.getDetailsToSync() : workLogSyncPayloadDTO.getDetailsToUnsync();
        Map<String, List<WorkLogDetailSyncRequestDTO>> taskDetails = detailsList.stream().collect(Collectors.groupingBy(WorkLogDetailSyncRequestDTO::getTaskName));
        WorkLogSyncResultDTO resultDTO = new WorkLogSyncResultDTO();
        String workLogUuid = workLogSyncPayloadDTO.getWorkLogUuid();
        boolean isDeleteOperation = !isSync && workLogSyncPayloadDTO.getDetailsToUnsync().stream().anyMatch(d -> d.getDetailId() == null);

        Iterator<Map.Entry<String, List<WorkLogDetailSyncRequestDTO>>> iterator = taskDetails.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<WorkLogDetailSyncRequestDTO>> entry = iterator.next();
            for (WorkLogDetailSyncRequestDTO detail : entry.getValue()) {
                try {
                    boolean hasError = isSync ? selfRef.syncWorkLogDetailToJira(syncUser, workLogUuid, detail) : selfRef.unSyncWorkLogDetailFromJira(syncUser, workLogUuid, detail);
                    if (hasError) {
                        resultDTO.setHasSoftError(true);
                    }
                } catch (Exception e) {
                    if (isLastRetry) {
                        List<Long> failedIds = detailsList.stream().map(WorkLogDetailSyncRequestDTO::getDetailId).filter(Objects::nonNull).toList();
                        selfRef.handleFailedSync(syncUser, failedIds, isSync ? WorkLogStatus.NOT_SYNCED : WorkLogStatus.SYNCED, e.getMessage()); // Revert to previous status on failure
                    }
                    resultDTO.setHardError(e.getMessage());
                    break;
                }
            }
            if (StringUtils.isEmpty(resultDTO.getHardError())) {
                break;
            }
            if (!isDeleteOperation) {
                handleTasksCountAfterSyncOp(workLogSyncPayloadDTO, syncUser, entry.getKey(), workLogUuid, isSync, resultDTO.isHasSoftError());
            }
            iterator.remove();
        }
        return resultDTO;
    }

    @Transactional
    public boolean syncWorkLogDetailToJira(User user, String workLogUuid, WorkLogDetailSyncRequestDTO detail) {
        log.info("Synchronizing WorkLogDetail with Id {} to Jira", detail.getDetailId());
        WorkLogDetail workLogDetail = workLogDetailRepository.findOne(detail.getDetailId());
        boolean hasError = false;
        try {
            String jiraId = jiraService.addOrUpdateWorkLog(user, workLogDetail);
            workLogDetail.setStatus(WorkLogStatus.SYNCED);
            workLogDetail.setJiraId(jiraId);
            workLogDetail.setSyncError(null);
        } catch (JiraException exception) {
            if (exception.getStatusCode() == 404) {
                log.warn("Jira WorkLog [{}, {}] not found. Proceeding to skip synchronization locally.", workLogDetail.getId(), workLogDetail.getTaskName());
                workLogDetail.setStatus(WorkLogStatus.NOT_SYNCED);
                workLogDetail.setSyncError(exception.getMessage());
                hasError = true;
            } else {
                throw exception;
            }
        }
        workLogDetailRepository.save(workLogDetail);

        WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.ENTRY, workLogUuid, null, List.of(workLogDetail.getUuid()), workLogDetail.getStatus(), workLogDetail.getSyncError());
        notificationService.sendNotification(new NotificationDTO(user.getUuid(), WorkLogService.WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
        return hasError;
    }

    @Transactional
    public boolean unSyncWorkLogDetailFromJira(User user, String workLogUuid, WorkLogDetailSyncRequestDTO detailSyncRequestDTO) {
        log.info("UnSynchronizing [{}] task WorkLogDetail from Jira for User Id: {}", detailSyncRequestDTO.getTaskName(), user.getId());
        String syncError = null;
        boolean hasError = false;
        try {
            jiraService.deleteWorkLog(user, detailSyncRequestDTO);
        } catch (JiraException exception) {
            if (exception.getStatusCode() == 404) {
                log.warn("Jira WorkLog with ID {} not found. Proceeding to mark as unsynced locally.", detailSyncRequestDTO.getJiraId());
                syncError = exception.getMessage();
                hasError = true;
            } else {
                throw exception;
            }
        }

        if (detailSyncRequestDTO.getDetailId() != null) {
            WorkLogDetail workLogDetail = workLogDetailRepository.findOne(detailSyncRequestDTO.getDetailId());
            workLogDetail.setStatus(WorkLogStatus.NOT_SYNCED);
            workLogDetail.setJiraId(null);
            workLogDetail.setSyncError(syncError);
            workLogDetailRepository.save(workLogDetail);

            WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.ENTRY, workLogUuid, null, List.of(workLogDetail.getUuid()), WorkLogStatus.NOT_SYNCED, syncError);
            notificationService.sendNotification(new NotificationDTO(user.getUuid(), WorkLogService.WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
        }
        return hasError;
    }

    private void handleTasksCountAfterSyncOp(WorkLogSyncPayloadDTO workLogSyncPayloadDTO, User user, String workLogUuid, String taskName, boolean isSync, boolean hasError) {
        if (workLogUuid == null)
            return;

        List<WorkLogDetailSyncRequestDTO> detailsToSync = workLogSyncPayloadDTO.getDetailsToSync();
        boolean taskHasAnotherOperation = !isSync && detailsToSync != null && detailsToSync.stream().anyMatch(ds -> ds.getTaskName().equals(taskName));
        WorkLogStatus taskStatus = taskHasAnotherOperation ? WorkLogStatus.SYNC_IN_PROGRESS : workLogDetailRepository.calculateWorkLogTaskStatus(workLogUuid, taskName);
        WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.TASK, workLogUuid, List.of(taskName), null, taskStatus, hasError ? "Some entries had errors during synchronization." : null);
        notificationService.sendNotification(new NotificationDTO(user.getUuid(), WorkLogService.WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
    }

    @Transactional
    public void updateWorkLogStatus(User user, WorkLogSyncPayloadDTO workLogSyncPayloadDTO, boolean hasError) {
        WorkLog workLog = workLogRepository.findByUuid(workLogSyncPayloadDTO.getWorkLogUuid());
        workLog.setStatus(workLogRepository.calculateWorkLogStatus(workLog));
        workLogRepository.save(workLog);

        WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.WORKLOG, workLog.getUuid(), null, null, workLog.getStatus(), hasError ? "Some tasks had errors during synchronization." : null);
        notificationService.sendNotification(new NotificationDTO(user.getUuid(), WorkLogService.WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
    }

    @Transactional
    public void updateJobPayload(BackgroundJob job, WorkLogSyncPayloadDTO workLogSyncPayloadDTO) {
        job.setPayload(JsonUtil.convertObjectToJsonString(workLogSyncPayloadDTO));
        backgroundJobRepository.save(job);
    }

    @Transactional
    public void handleFailedSync(User user, List<Long> detailIds, WorkLogStatus status, String errorMessage) {
        List<WorkLogDetail> workLogDetails = workLogDetailRepository.findAllById(detailIds);
        for (WorkLogDetail workLogDetail : workLogDetails) {
            workLogDetail.setStatus(status);
            workLogDetail.setSyncError(errorMessage);
        }
        workLogDetailRepository.saveAll(workLogDetails);

        WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.ALL, workLogDetails.getFirst().getWorkLog().getUuid(),
                new ArrayList<>(workLogDetails.stream().map(WorkLogDetail::getTaskName).collect(Collectors.toSet())),
                new ArrayList<>(workLogDetails.stream().map(WorkLogDetail::getUuid).collect(Collectors.toSet())),
                status, errorMessage);
        notificationService.sendNotification(new NotificationDTO(user.getUuid(), WorkLogService.WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
    }

    @Transactional
    public void markDetailsForStartingSyncOperation(User user, String workLogUuid, String taskName, List<Long> detailIds, boolean sync) {
        if (detailIds.isEmpty())
            return;

        WorkLogStatus workLogStatus = sync ? WorkLogStatus.SYNC_IN_PROGRESS : WorkLogStatus.UNSYNC_IN_PROGRESS;
        List<WorkLogDetail> workLogDetails = workLogDetailRepository.findAllById(detailIds);
        workLogDetails.forEach(detail -> detail.setStatus(workLogStatus));
        workLogDetailRepository.saveAll(workLogDetails);

        WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.ALL, workLogUuid, List.of(taskName), workLogDetails.stream().map(WorkLogDetail::getUuid).toList(), workLogStatus, null);
        notificationService.sendNotification(new NotificationDTO(user.getUuid(), WorkLogService.WORKLOG_SYNC_STATUS_EVENT_NAME, syncMessageDTO));
    }
}
