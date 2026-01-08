package com.mdevs.trackera.service;

import com.mdevs.trackera.config.messaging.RabbitConfig;
import com.mdevs.trackera.dto.backgroundjob.BackgroundJobMessageDTO;
import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.job.handlers.BackgroundJobHandler;
import com.mdevs.trackera.repository.BackgroundJobRepository;
import com.mdevs.trackera.shared.enums.BackgroundJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackgroundJobService {
    private final BackgroundJobRepository backgroundJobRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void enqueueJob(Class<? extends BackgroundJobHandler> jobName, String payload) {
        BackgroundJob job = new BackgroundJob();
        job.setName(jobName.getSimpleName());
        job.setPayload(payload);
        job.setStatus(BackgroundJobStatus.PENDING);
        job.setRetryCount(0);
        job = backgroundJobRepository.save(job);

        BackgroundJobMessageDTO message = new BackgroundJobMessageDTO(job.getId(), job.getName(), job.getPayload());

        applicationEventPublisher.publishEvent(message);

        log.info("Enqueued job: {} (name: {})", job.getId(), jobName);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBackgroundJobCreated(BackgroundJobMessageDTO messageDTO) {
        rabbitTemplate.convertAndSend(RabbitConfig.JOB_EXCHANGE, RabbitConfig.JOB_ROUTING_KEY, messageDTO);
        log.info("Sent job to RabbitMQ: {} (name: {})", messageDTO.jobId(), messageDTO.jobName());
    }

    @Transactional
    public long deleteFinishedJobs(long maxId, int pageSize) {
        List<Long> finishedJobsIds = backgroundJobRepository
                .findByStatusInAndIdAfterOrderById(List.of(BackgroundJobStatus.COMPLETED, BackgroundJobStatus.FAILED), maxId, Pageable.ofSize(pageSize));
        if (!finishedJobsIds.isEmpty()) {
            backgroundJobRepository.deleteAllByIdInBatch(finishedJobsIds);
            return finishedJobsIds.getLast();
        }
        return -1;
    }
}