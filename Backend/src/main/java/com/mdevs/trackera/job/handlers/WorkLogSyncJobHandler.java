package com.mdevs.trackera.job.handlers;

import com.mdevs.trackera.config.messaging.RabbitConfig;
import com.mdevs.trackera.dto.worklog.WorkLogDetailSyncRequestDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSyncMessageDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSyncPayloadDTO;
import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.repository.BackgroundJobRepository;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.repository.WorkLogDetailRepository;
import com.mdevs.trackera.repository.WorkLogRepository;
import com.mdevs.trackera.service.JiraService;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.enums.WorkLogSyncMessageType;
import com.mdevs.trackera.shared.exceptions.types.JiraException;
import com.mdevs.trackera.utils.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WorkLogSyncJobHandler implements BackgroundJobHandler {
    private final JiraService jiraService;

    private final UserRepository userRepository;

    private final WorkLogRepository workLogRepository;

    private final WorkLogDetailRepository workLogDetailRepository;

    private final BackgroundJobRepository backgroundJobRepository;

    private final SimpMessagingTemplate messagingTemplate;

    private final WorkLogSyncJobHandler selfRef;

    public WorkLogSyncJobHandler(JiraService jiraService, UserRepository userRepository, WorkLogRepository workLogRepository, WorkLogDetailRepository workLogDetailRepository,
                                 BackgroundJobRepository backgroundJobRepository, SimpMessagingTemplate messagingTemplate, @Lazy WorkLogSyncJobHandler selfRef) {
        this.jiraService = jiraService;
        this.userRepository = userRepository;
        this.workLogRepository = workLogRepository;
        this.workLogDetailRepository = workLogDetailRepository;
        this.backgroundJobRepository = backgroundJobRepository;
        this.messagingTemplate = messagingTemplate;
        this.selfRef = selfRef;
    }

    @Override
    public void handle(BackgroundJob job) {
        WorkLogSyncPayloadDTO workLogSyncPayloadDTO = AppUtils.convertJsonStringToObject(job.getPayload(), WorkLogSyncPayloadDTO.class);
        User syncUser = userRepository.findOne(workLogSyncPayloadDTO.getUserId());
        boolean isLastRetry = job.getRetryCount() >= RabbitConfig.MAX_RETRIES;
        String exceptionMessage = null;

        List<WorkLogDetailSyncRequestDTO> detailsToUnsync = workLogSyncPayloadDTO.getDetailsToUnsync();
        if (detailsToUnsync != null && !detailsToUnsync.isEmpty()) {
            exceptionMessage = processUnsyncDetails(detailsToUnsync, syncUser, isLastRetry);
        }

        List<WorkLogDetailSyncRequestDTO> detailsToSync = workLogSyncPayloadDTO.getDetailsToSync();
        if (StringUtils.isEmpty(exceptionMessage) && (detailsToSync != null && !detailsToSync.isEmpty())) {
            exceptionMessage = processSyncDetails(detailsToSync, syncUser, isLastRetry);
        }

        if (workLogSyncPayloadDTO.getWorkLogId() != null) {
            selfRef.updateWorkLogStatus(workLogSyncPayloadDTO);
        }

        selfRef.updateJobPayload(job, workLogSyncPayloadDTO);

        if (!StringUtils.isEmpty(exceptionMessage)) {
            throw new RuntimeException(exceptionMessage);
        }
    }

    private String processUnsyncDetails(List<WorkLogDetailSyncRequestDTO> detailsToUnsync, User syncUser, boolean isLastRetry) {
        Map<String, Long> tasksCount = detailsToUnsync.stream().collect(Collectors.groupingBy(WorkLogDetailSyncRequestDTO::getTaskName, Collectors.counting()));
        Iterator<WorkLogDetailSyncRequestDTO> iterator = detailsToUnsync.iterator();
        while (iterator.hasNext()) {
            WorkLogDetailSyncRequestDTO detail = iterator.next();
            try {
                selfRef.unSyncWorkLogDetailFromJira(syncUser, detail);
                iterator.remove();
                handleTasksCountAfterSyncOp(tasksCount, detail);
            } catch (Exception e) {
                if (isLastRetry) {
                    List<Long> failedIds = detailsToUnsync.stream().map(WorkLogDetailSyncRequestDTO::getDetailId).filter(Objects::nonNull).toList();
                    selfRef.handleFailedSync(failedIds, WorkLogStatus.SYNCED, e.getMessage());
                }
                return e.getMessage();
            }
        }
        return null;
    }

    private String processSyncDetails(List<WorkLogDetailSyncRequestDTO> detailsToSync, User syncUser, boolean isLastRetry) {
        Map<String, Long> tasksCount = detailsToSync.stream().collect(Collectors.groupingBy(WorkLogDetailSyncRequestDTO::getTaskName, Collectors.counting()));
        Iterator<WorkLogDetailSyncRequestDTO> iterator = detailsToSync.iterator();
        while (iterator.hasNext()) {
            WorkLogDetailSyncRequestDTO detail = iterator.next();
            try {
                selfRef.syncWorkLogDetailToJira(syncUser, detail);
                iterator.remove();
                handleTasksCountAfterSyncOp(tasksCount, detail);
            } catch (Exception e) {
                if (isLastRetry) {
                    List<Long> failedIds = detailsToSync.stream().map(WorkLogDetailSyncRequestDTO::getDetailId).filter(Objects::nonNull).toList();
                    selfRef.handleFailedSync(failedIds, WorkLogStatus.NOT_SYNCED, e.getMessage());
                }
                return e.getMessage();
            }
        }
        return null;
    }

    @Transactional
    public void syncWorkLogDetailToJira(User user, WorkLogDetailSyncRequestDTO detail) {
        log.info("Synchronizing WorkLogDetail with Id {} to Jira", detail.getDetailId());
        WorkLogDetail workLogDetail = workLogDetailRepository.findOne(detail.getDetailId());
        try {
            String jiraId = jiraService.addOrUpdateWorkLog(user, workLogDetail);
            workLogDetail.setStatus(WorkLogStatus.SYNCED);
            workLogDetail.setJiraId(jiraId);
            workLogDetail.setSyncError(null);
        } catch (JiraException exception) {
            if (exception.getStatusCode() == 404) {
                workLogDetail.setStatus(WorkLogStatus.NOT_SYNCED);
                workLogDetail.setSyncError(exception.getMessage());
                log.warn("Jira WorkLog [{}, {}] not found. Proceeding to skip synchronization locally.", workLogDetail.getId(), workLogDetail.getTaskName());
            } else {
                throw exception;
            }
        }
        workLogDetailRepository.save(workLogDetail);

        WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.ENTRY, detail.getWorkLogId(), null, List.of(workLogDetail.getUuid()), workLogDetail.getStatus());
        messagingTemplate.convertAndSend("/topic/log-status/" + detail.getWorkLogId(), syncMessageDTO);
    }

    @Transactional
    public void unSyncWorkLogDetailFromJira(User user, WorkLogDetailSyncRequestDTO detailSyncRequestDTO) {
        log.info("UnSynchronizing [{}] task WorkLogDetail from Jira for User Id: {}", detailSyncRequestDTO.getTaskName(), user.getId());
        try {
            jiraService.deleteWorkLog(user, detailSyncRequestDTO);
        } catch (JiraException exception) {
            if (exception.getStatusCode() == 404) {
                log.warn("Jira WorkLog with ID {} not found. Proceeding to mark as unsynced locally.", detailSyncRequestDTO.getJiraId());
            } else {
                throw exception;
            }
        }

        if (detailSyncRequestDTO.getDetailId() != null) {
            WorkLogDetail workLogDetail = workLogDetailRepository.findOne(detailSyncRequestDTO.getDetailId());
            workLogDetail.setStatus(WorkLogStatus.NOT_SYNCED);
            workLogDetail.setJiraId(null);
            workLogDetail.setSyncError(null);
            workLogDetailRepository.save(workLogDetail);

            WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.ENTRY, detailSyncRequestDTO.getWorkLogId(), null, List.of(workLogDetail.getUuid()), WorkLogStatus.NOT_SYNCED);
            messagingTemplate.convertAndSend("/topic/log-status/" + detailSyncRequestDTO.getWorkLogId(), syncMessageDTO);
        }
    }

    private void handleTasksCountAfterSyncOp(Map<String, Long> tasksCount, WorkLogDetailSyncRequestDTO detail) {
        if (detail.getDetailId() == null)
            return;

        Long count = tasksCount.get(detail.getTaskName());
        if (count == 0) {
            WorkLogStatus taskStatus = workLogDetailRepository.calculateWorkLogTaskStatus(detail.getTaskName());
            messagingTemplate.convertAndSend("/topic/log-status/" + detail.getWorkLogId(), new WorkLogSyncMessageDTO(WorkLogSyncMessageType.TASK, detail.getWorkLogId(), List.of(detail.getTaskName()), null, taskStatus));
        } else {
            tasksCount.put(detail.getTaskName(), count - 1);
        }
    }

    @Transactional
    public void updateWorkLogStatus(WorkLogSyncPayloadDTO workLogSyncPayloadDTO) {
        WorkLog workLog = workLogRepository.findOne(workLogSyncPayloadDTO.getWorkLogId());
        workLog.setStatus(workLogRepository.calculateWorkLogStatus(workLog));
        workLogRepository.save(workLog);

        messagingTemplate.convertAndSend("/topic/log-status/" + workLog.getUuid(), new WorkLogSyncMessageDTO(WorkLogSyncMessageType.WORKLOG, workLog.getUuid(), null, null, workLog.getStatus()));
    }

    @Transactional
    public void updateJobPayload(BackgroundJob job, WorkLogSyncPayloadDTO workLogSyncPayloadDTO) {
        job.setPayload(AppUtils.convertObjectToJsonString(workLogSyncPayloadDTO));
        backgroundJobRepository.save(job);
    }

    @Transactional
    public void handleFailedSync(List<Long> detailIds, WorkLogStatus status, String errorMessage) {
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
        messagingTemplate.convertAndSend("/topic/log-status/" + workLogDetails.getFirst().getWorkLog().getUuid(), syncMessageDTO);
    }
}
