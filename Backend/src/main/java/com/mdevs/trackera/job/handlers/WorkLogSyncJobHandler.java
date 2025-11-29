package com.mdevs.trackera.job.handlers;

import com.mdevs.trackera.dto.worklog.WorkLogDetailSyncRequestDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSyncPayloadDTO;
import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.repository.WorkLogDetailRepository;
import com.mdevs.trackera.repository.WorkLogRepository;
import com.mdevs.trackera.service.JiraService;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.exceptions.types.JiraException;
import com.mdevs.trackera.utils.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class WorkLogSyncJobHandler implements BackgroundJobHandler {
    private final JiraService jiraService;

    private final UserRepository userRepository;

    private final WorkLogRepository workLogRepository;

    private final WorkLogDetailRepository workLogDetailRepository;

    private final WorkLogSyncJobHandler selfRef;

    public WorkLogSyncJobHandler(JiraService jiraService, UserRepository userRepository, WorkLogRepository workLogRepository, WorkLogDetailRepository workLogDetailRepository, @Lazy WorkLogSyncJobHandler selfRef) {
        this.jiraService = jiraService;
        this.userRepository = userRepository;
        this.workLogRepository = workLogRepository;
        this.workLogDetailRepository = workLogDetailRepository;
        this.selfRef = selfRef;
    }

    @Override
    public void handle(BackgroundJob job) {
        WorkLogSyncPayloadDTO workLogSyncPayloadDTO = AppUtils.convertJsonStringToObject(job.getPayload(), WorkLogSyncPayloadDTO.class);
        User syncUser = userRepository.findOne(workLogSyncPayloadDTO.getUserId());

        if (workLogSyncPayloadDTO.getDetailsToUnsync() != null && !workLogSyncPayloadDTO.getDetailsToUnsync().isEmpty()) {
            for (WorkLogDetailSyncRequestDTO detailSyncRequestDTO : workLogSyncPayloadDTO.getDetailsToUnsync()) {
                selfRef.unSyncWorkLogDetailFromJira(syncUser, detailSyncRequestDTO);
                // @TODO --> should remove the object from the list after processing to avoid re-processing in case of retries
            }
        }

        if (workLogSyncPayloadDTO.getDetailsToSync() != null && !workLogSyncPayloadDTO.getDetailsToSync().isEmpty()) {
            for (Long detailId : workLogSyncPayloadDTO.getDetailsToSync()) {
                selfRef.syncWorkLogDetailToJira(syncUser, detailId);
                // @TODO --> should remove the object from the list after processing to avoid re-processing in case of retries
            }
        }

        if (workLogSyncPayloadDTO.getWorkLogId() != null) {
            WorkLog workLog = workLogRepository.findOne(workLogSyncPayloadDTO.getWorkLogId());
            workLog.setStatus(workLogRepository.calculateWorkLogStatus(workLog));
            workLogRepository.save(workLog);
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
}
