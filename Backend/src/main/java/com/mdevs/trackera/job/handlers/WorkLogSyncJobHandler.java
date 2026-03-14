package com.mdevs.trackera.job.handlers;

import com.mdevs.trackera.config.messaging.RabbitConfig;
import com.mdevs.trackera.dto.worklog.WorkLogDetailSyncRequestDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSyncPayloadDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSyncResultDTO;
import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.service.BackgroundJobService;
import com.mdevs.trackera.service.JiraService;
import com.mdevs.trackera.service.UserService;
import com.mdevs.trackera.service.WorkLogService;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.enums.WorkLogSyncMessageType;
import com.mdevs.trackera.shared.enums.WorklogSyncOperation;
import com.mdevs.trackera.shared.exceptions.types.JiraException;
import com.mdevs.trackera.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkLogSyncJobHandler implements BackgroundJobHandler {
    private final JiraService jiraService;

    private final WorkLogService workLogService;

    private final UserService userService;

    private final BackgroundJobService backgroundJobService;

    @Override
    public void handle(BackgroundJob job) {
        WorkLogSyncPayloadDTO payload = JsonUtil.convertJsonStringToObject(job.getPayload(), WorkLogSyncPayloadDTO.class);
        User syncUser = userService.findByIdOrThrow(payload.getUserId());
        boolean isLastRetry = job.getRetryCount() >= RabbitConfig.MAX_RETRIES;

        WorkLogSyncResultDTO resultDTO = new WorkLogSyncResultDTO();

        if (!payload.getDetailsToReSync().isEmpty()) {
            resultDTO = processDetailsSyncOperation(payload, syncUser, WorklogSyncOperation.RESYNC, isLastRetry);
        }

        if (StringUtils.isEmpty(resultDTO.getHardError()) && !payload.getDetailsToUnsync().isEmpty()) {
            resultDTO = processDetailsSyncOperation(payload, syncUser, WorklogSyncOperation.UNSYNC, isLastRetry);
        }

        if (StringUtils.isEmpty(resultDTO.getHardError()) && !payload.getDetailsToSync().isEmpty()) {
            resultDTO = processDetailsSyncOperation(payload, syncUser, WorklogSyncOperation.SYNC, isLastRetry);
        }

        if (payload.getWorkLogUuid() != null) {
            WorkLog workLog = workLogService.recalculateAndSaveWorkLogStatus(payload.getWorkLogUuid());
            String error = resultDTO.hasError() ? "Some tasks had errors during synchronization." : null;
            workLogService.sendSyncNotification(syncUser, workLog.getUuid(), WorkLogSyncMessageType.WORKLOG, null, null, workLog.getStatus(), error);
        }

        backgroundJobService.updateJobPayload(job, JsonUtil.convertObjectToJsonString(payload));

        if (!StringUtils.isEmpty(resultDTO.getHardError())) {
            throw new RuntimeException(resultDTO.getHardError());
        }
    }

    private WorkLogSyncResultDTO processDetailsSyncOperation(WorkLogSyncPayloadDTO payload, User syncUser, WorklogSyncOperation syncOperation, boolean isLastRetry) {
        List<WorkLogDetailSyncRequestDTO> detailsList = getDetailsByOperation(payload, syncOperation);
        Map<String, List<WorkLogDetailSyncRequestDTO>> taskDetails = detailsList.stream().collect(Collectors.groupingBy(WorkLogDetailSyncRequestDTO::getTaskName));
        WorkLogSyncResultDTO resultDTO = new WorkLogSyncResultDTO();
        String workLogUuid = payload.getWorkLogUuid();

        Iterator<Map.Entry<String, List<WorkLogDetailSyncRequestDTO>>> iterator = taskDetails.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<WorkLogDetailSyncRequestDTO>> entry = iterator.next();
            String taskName = entry.getKey();
            boolean taskHasError = false;
            workLogService.markDetailsForSyncOperation(syncUser, workLogUuid, taskName, entry.getValue().stream().map(WorkLogDetailSyncRequestDTO::getDetailId).filter(Objects::nonNull).toList(), syncOperation);
            for (WorkLogDetailSyncRequestDTO detail : entry.getValue()) {
                try {
                    taskHasError = processSingleDetail(syncUser, workLogUuid, detail, syncOperation);
                    if (taskHasError) {
                        resultDTO.setHasSoftError(true);
                    }
                } catch (Exception e) {
                    if (isLastRetry) {
                        List<Long> failedIds = detailsList.stream().map(WorkLogDetailSyncRequestDTO::getDetailId).filter(Objects::nonNull).toList();
                        WorkLogStatus revertStatus = syncOperation.equals(WorklogSyncOperation.UNSYNC) ? WorkLogStatus.SYNCED : WorkLogStatus.NOT_SYNCED;
                        workLogService.handleFailedDetailSync(syncUser, failedIds, revertStatus, e.getMessage());
                    }
                    resultDTO.setHardError(e.getMessage());
                    break;
                }
            }
            if (!StringUtils.isEmpty(resultDTO.getHardError())) {
                break;
            }
            if (!payload.isWorkLogDeletion()) {
                handleTaskStatusAfterSyncOp(payload, syncUser, workLogUuid, taskName, syncOperation, taskHasError);
            }
            iterator.remove();
        }
        return resultDTO;
    }

    private List<WorkLogDetailSyncRequestDTO> getDetailsByOperation(WorkLogSyncPayloadDTO payload, WorklogSyncOperation syncOperation) {
        if (syncOperation.equals(WorklogSyncOperation.RESYNC)) {
            return payload.getDetailsToReSync();
        } else if (syncOperation.equals(WorklogSyncOperation.UNSYNC)) {
            return payload.getDetailsToUnsync();
        }
        return payload.getDetailsToSync();
    }

    private boolean processSingleDetail(User user, String workLogUuid, WorkLogDetailSyncRequestDTO detail, WorklogSyncOperation syncOperation) {
        if (syncOperation.equals(WorklogSyncOperation.RESYNC)) {
            WorkLogDetailSyncRequestDTO unsyncDto = new WorkLogDetailSyncRequestDTO(detail.getDetailId(), detail.getOldTaskName(), detail.getJiraId());
            unSyncWorkLogDetailFromJira(user, workLogUuid, unsyncDto, true);
            return syncWorkLogDetailToJira(user, workLogUuid, detail);
        } else if (syncOperation.equals(WorklogSyncOperation.UNSYNC)) {
            return unSyncWorkLogDetailFromJira(user, workLogUuid, detail, false);
        }
        return syncWorkLogDetailToJira(user, workLogUuid, detail);
    }

    private boolean syncWorkLogDetailToJira(User user, String workLogUuid, WorkLogDetailSyncRequestDTO detail) {
        log.info("Synchronizing WorkLogDetail with Id {} to Jira", detail.getDetailId());
        WorkLogDetail workLogDetail = workLogService.findWorkLogDetailById(detail.getDetailId());
        WorkLogStatus status;
        String jiraId;
        String syncError = null;
        boolean hasError = false;
        try {
            jiraId = jiraService.addOrUpdateWorkLog(user, workLogDetail);
            status = WorkLogStatus.SYNCED;
        } catch (JiraException exception) {
            if (exception.getStatusCode() == 404) {
                log.warn("Jira WorkLog [{}, {}] not found. Proceeding to skip synchronization locally.", workLogDetail.getId(), workLogDetail.getTaskName());
                status = WorkLogStatus.NOT_SYNCED;
                jiraId = workLogDetail.getJiraId();
                syncError = exception.getMessage();
                hasError = true;
            } else {
                log.warn("Error syncing WorkLogDetail with Id {} to Jira: {}", detail.getDetailId(), exception.getMessage(), exception);
                throw exception;
            }
        }
        String detailUuid = workLogService.updateDetailAfterSync(detail.getDetailId(), status, jiraId, syncError);
        workLogService.sendSyncNotification(user, workLogUuid, WorkLogSyncMessageType.ENTRY, null, List.of(detailUuid), status, syncError);
        return hasError;
    }

    private boolean unSyncWorkLogDetailFromJira(User user, String workLogUuid, WorkLogDetailSyncRequestDTO detailUnsyncRequestDto, boolean isResync) {
        log.info("UnSynchronizing [{}] task WorkLogDetail from Jira for User Id: {}", detailUnsyncRequestDto.getTaskName(), user.getId());
        String syncError = null;
        boolean hasError = false;
        try {
            jiraService.deleteWorkLog(user, detailUnsyncRequestDto);
        } catch (JiraException exception) {
            if (exception.getStatusCode() == 404) {
                log.warn("Jira WorkLog with ID {} not found. Proceeding to mark as unsynced locally.", detailUnsyncRequestDto.getJiraId());
                syncError = exception.getMessage();
                hasError = true;
            } else {
                log.warn("Error unsyncing WorkLogDetail with Jira ID {}: {}", detailUnsyncRequestDto.getJiraId(), exception.getMessage(), exception);
                throw exception;
            }
        }

        if (detailUnsyncRequestDto.getDetailId() != null) {
            WorkLogStatus entryStatus = isResync ? WorkLogStatus.SYNC_IN_PROGRESS : WorkLogStatus.NOT_SYNCED;
            String detailUuid = workLogService.updateDetailAfterSync(detailUnsyncRequestDto.getDetailId(), entryStatus, null, syncError);
            workLogService.sendSyncNotification(user, workLogUuid, WorkLogSyncMessageType.ENTRY, null, List.of(detailUuid), entryStatus, syncError);
        }
        return hasError;
    }

    private void handleTaskStatusAfterSyncOp(WorkLogSyncPayloadDTO payload, User user, String workLogUuid, String taskName, WorklogSyncOperation syncOperation, boolean hasError) {
        if (workLogUuid == null)
            return;

        boolean taskHasAnotherOperation = syncOperation.equals(WorklogSyncOperation.UNSYNC) && hasAnotherSyncOperation(payload.getDetailsToSync(), taskName);
        WorkLogStatus taskStatus = taskHasAnotherOperation ? WorkLogStatus.SYNC_IN_PROGRESS : workLogService.calculateTaskStatus(workLogUuid, taskName);
        String error = hasError ? "Some entries had errors during synchronization." : null;
        workLogService.sendSyncNotification(user, workLogUuid, WorkLogSyncMessageType.TASK, List.of(taskName), null, taskStatus, error);
    }

    private boolean hasAnotherSyncOperation(List<WorkLogDetailSyncRequestDTO> detailsToSync, String taskName) {
        return detailsToSync != null && detailsToSync.stream().anyMatch(ds -> ds.getTaskName().equals(taskName));
    }
}
