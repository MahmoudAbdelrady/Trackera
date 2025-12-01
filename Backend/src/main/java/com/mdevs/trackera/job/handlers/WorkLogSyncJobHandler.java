package com.mdevs.trackera.job.handlers;

import com.mdevs.trackera.config.messaging.RabbitConfig;
import com.mdevs.trackera.dto.worklog.WorkLogDetailSyncRequestDTO;
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
import com.mdevs.trackera.shared.exceptions.types.JiraException;
import com.mdevs.trackera.utils.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class WorkLogSyncJobHandler implements BackgroundJobHandler {
    private final JiraService jiraService;

    private final UserRepository userRepository;

    private final WorkLogRepository workLogRepository;

    private final WorkLogDetailRepository workLogDetailRepository;

    private final BackgroundJobRepository backgroundJobRepository;

    private final WorkLogSyncJobHandler selfRef;

    public WorkLogSyncJobHandler(JiraService jiraService, UserRepository userRepository, WorkLogRepository workLogRepository, WorkLogDetailRepository workLogDetailRepository,
                                 BackgroundJobRepository backgroundJobRepository, @Lazy WorkLogSyncJobHandler selfRef) {
        this.jiraService = jiraService;
        this.userRepository = userRepository;
        this.workLogRepository = workLogRepository;
        this.workLogDetailRepository = workLogDetailRepository;
        this.backgroundJobRepository = backgroundJobRepository;
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
            for (WorkLogDetailSyncRequestDTO detailSyncRequestDTO : detailsToUnsync) {
                try {
                    selfRef.unSyncWorkLogDetailFromJira(syncUser, detailSyncRequestDTO);
                    detailsToUnsync.remove(detailSyncRequestDTO);
                } catch (Exception e) {
                    exceptionMessage = e.getMessage();
                    break;
                }
            }

            if (!StringUtils.isEmpty(exceptionMessage) && isLastRetry) {
                selfRef.handleFailedSync(detailsToUnsync.stream().map(WorkLogDetailSyncRequestDTO::getDetailId).filter(Objects::nonNull).toList(), WorkLogStatus.SYNCED);
            }
        }

        List<Long> detailsToSync = workLogSyncPayloadDTO.getDetailsToSync();
        if (StringUtils.isEmpty(exceptionMessage) && (detailsToSync != null && !detailsToSync.isEmpty())) {
            for (Long detailId : detailsToSync) {
                try {
                    selfRef.syncWorkLogDetailToJira(syncUser, detailId);
                    detailsToSync.remove(detailId);
                } catch (Exception e) {
                    exceptionMessage = e.getMessage();
                    break;
                }
            }

            if (!StringUtils.isEmpty(exceptionMessage) && isLastRetry) {
                selfRef.handleFailedSync(detailsToSync, WorkLogStatus.NOT_SYNCED);
            }
        }

        if (workLogSyncPayloadDTO.getWorkLogId() != null) {
            selfRef.updateWorkLogStatus(workLogSyncPayloadDTO);
        }

        selfRef.updateJobPayload(job, workLogSyncPayloadDTO);

        if (!StringUtils.isEmpty(exceptionMessage)) {
            throw new RuntimeException(exceptionMessage);
        }
    }

    @Transactional
    public void syncWorkLogDetailToJira(User user, Long detailId) {
        log.info("Synchronizing WorkLogDetail with Id {} to Jira", detailId);
        WorkLogDetail workLogDetail = workLogDetailRepository.findOne(detailId);
        String jiraId = jiraService.addWorkLog(user, workLogDetail);
        workLogDetail.setStatus(WorkLogStatus.SYNCED);
        workLogDetail.setJiraId(jiraId);
        workLogDetailRepository.save(workLogDetail);
    }

    @Transactional
    public void unSyncWorkLogDetailFromJira(User user, WorkLogDetailSyncRequestDTO detailSyncRequestDTO) {
        log.info("UnSynchronizing [{}] task WorkLogDetail from Jira for User Id: {}", detailSyncRequestDTO.getTaskName(), user.getId());
        try {
            jiraService.deleteWorkLogV2(user, detailSyncRequestDTO);
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
            workLogDetailRepository.save(workLogDetail);
        }
    }

    @Transactional
    public void updateWorkLogStatus(WorkLogSyncPayloadDTO workLogSyncPayloadDTO) {
        WorkLog workLog = workLogRepository.findOne(workLogSyncPayloadDTO.getWorkLogId());
        workLog.setStatus(workLogRepository.calculateWorkLogStatus(workLog));
        workLogRepository.save(workLog);
    }

    @Transactional
    public void updateJobPayload(BackgroundJob job, WorkLogSyncPayloadDTO workLogSyncPayloadDTO) {
        job.setPayload(AppUtils.convertObjectToJsonString(workLogSyncPayloadDTO));
        backgroundJobRepository.save(job);
    }

    @Transactional
    public void handleFailedSync(List<Long> detailIds, WorkLogStatus status) {
        List<WorkLogDetail> workLogDetails = workLogDetailRepository.findAllById(detailIds);
        for (WorkLogDetail workLogDetail : workLogDetails) {
            workLogDetail.setStatus(status);
        }
        workLogDetailRepository.saveAll(workLogDetails);
    }
}
