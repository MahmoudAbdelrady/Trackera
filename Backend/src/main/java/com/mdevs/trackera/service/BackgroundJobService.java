package com.mdevs.trackera.service;

import com.mdevs.trackera.config.messaging.RabbitConfig;
import com.mdevs.trackera.dto.backgroundjob.BackgroundJobMessageDTO;
import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.job.handlers.BackgroundJobHandler;
import com.mdevs.trackera.repository.BackgroundJobRepository;
import com.mdevs.trackera.shared.enums.BackgroundJobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
public class BackgroundJobService {
    private final BackgroundJobRepository jobRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final RabbitTemplate rabbitTemplate;

    public BackgroundJobService(BackgroundJobRepository jobRepository, ApplicationEventPublisher applicationEventPublisher, RabbitTemplate rabbitTemplate) {
        this.jobRepository = jobRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void enqueueJob(Class<? extends BackgroundJobHandler> jobName, String payload) {
        BackgroundJob job = new BackgroundJob();
        job.setName(jobName.getSimpleName());
        job.setPayload(payload);
        job.setStatus(BackgroundJobStatus.PENDING);
        job.setRetryCount(0);
        job = jobRepository.save(job);

        BackgroundJobMessageDTO message = new BackgroundJobMessageDTO(job.getId(), job.getName(), job.getPayload());

        applicationEventPublisher.publishEvent(message);

        log.info("Enqueued job: {} (name: {})", job.getId(), jobName);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBackgroundJobCreated(BackgroundJobMessageDTO messageDTO) {
        rabbitTemplate.convertAndSend(RabbitConfig.JOB_EXCHANGE, RabbitConfig.JOB_ROUTING_KEY, messageDTO);
        log.info("Sent job to RabbitMQ: {} (name: {})", messageDTO.getJobId(), messageDTO.getJobName());
    }
}