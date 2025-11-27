package com.mdevs.trackera.service;

import com.mdevs.trackera.config.messaging.RabbitConfig;
import com.mdevs.trackera.dto.backgroundjob.BackgroundJobMessage;
import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.repository.BackgroundJobRepository;
import com.mdevs.trackera.shared.enums.BackgroundJobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BackgroundJobService {
    private final BackgroundJobRepository jobRepository;

    private final RabbitTemplate rabbitTemplate;

    public BackgroundJobService(BackgroundJobRepository jobRepository, RabbitTemplate rabbitTemplate) {
        this.jobRepository = jobRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void enqueueJob(String jobName, String payload) {
        BackgroundJob job = new BackgroundJob();
        job.setName(jobName);
        job.setPayload(payload);
        job.setStatus(BackgroundJobStatus.PENDING);
        job.setRetryCount(0);
        job = jobRepository.save(job);

        BackgroundJobMessage message = new BackgroundJobMessage(job.getId(), job.getName(), job.getPayload());

        rabbitTemplate.convertAndSend(RabbitConfig.JOB_EXCHANGE, RabbitConfig.JOB_ROUTING_KEY, message);

        log.info("Enqueued job: {} (type: {})", job.getId(), jobName);
    }
}