package com.mdevs.trackera.shared;

import com.mdevs.trackera.config.messaging.RabbitConfig;
import com.mdevs.trackera.dto.backgroundjob.BackgroundJobMessage;
import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.repository.BackgroundJobRepository;
import com.mdevs.trackera.shared.enums.BackgroundJobStatus;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class BackgroundJobConsumer {
    private final BackgroundJobRepository jobRepository;

    private final BackgroundJobProcessor jobProcessor;

    private final RabbitTemplate rabbitTemplate;

    public BackgroundJobConsumer(BackgroundJobRepository jobRepository, BackgroundJobProcessor jobProcessor, RabbitTemplate rabbitTemplate) {
        this.jobRepository = jobRepository;
        this.jobProcessor = jobProcessor;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.JOB_QUEUE)
    public void processJob(BackgroundJobMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(value = "x-death", required = false) List<Map<String, Object>> xDeathHeader) {
        int retryCount = getRetryCount(xDeathHeader);
        System.out.println("Retry Count: " + retryCount);
        BackgroundJob job = null;

        try {
            log.info("Processing job [{}, {}] (attempt {})", message.getJobId(), message.getJobName(), retryCount + 1);

            // Load job from database
            job = Optional.ofNullable(jobRepository.findOne(message.getJobId())).orElseThrow(() -> new IllegalStateException("Job not found: [" + message.getJobId() + ", " + message.getJobName() + "]"));

            // Update status to PROCESSING
            job.setStatus(BackgroundJobStatus.IN_PROGRESS);
            job.setRetryCount(retryCount);
            job = jobRepository.save(job);

            // Process the job (actual business logic)
            jobProcessor.process(job);

            // Success - update status
            job.setStatus(BackgroundJobStatus.COMPLETED);
            job = jobRepository.save(job);

            // Acknowledge message (remove from queue)
            channel.basicAck(deliveryTag, false);
            log.info("Job [{}, {}] completed successfully", message.getJobId(), message.getJobName());
        } catch (Exception e) {
            log.error("Job [{}, {}] failed (attempt {}): {}", message.getJobId(), message.getJobName(), retryCount + 1, e.getMessage(), e);

            try {
                if (retryCount >= RabbitConfig.MAX_RETRIES) {
                    // Max retries exceeded - send to DLQ
                    handleMaxRetriesExceeded(job, message, e);
                    channel.basicAck(deliveryTag, false); // ACK to remove from queue
                } else {
                    // Retry - send to appropriate retry queue
                    handleRetry(job, message, retryCount);
                    channel.basicAck(deliveryTag, false); // ACK original message
                }
            } catch (IOException ioException) {
                log.error("Failed to acknowledge message: {}", ioException.getMessage());
            }
        }
    }

    private int getRetryCount(List<Map<String, Object>> xDeathHeader) {
        if (xDeathHeader == null || xDeathHeader.isEmpty()) {
            return 0;
        }

        return xDeathHeader.stream().map(death -> Integer.parseInt(death.get("count").toString())).findFirst().orElse(0);
    }

    private void handleRetry(BackgroundJob job, BackgroundJobMessage message, int retryCount) {
        int delay = RabbitConfig.RETRY_DELAYS[retryCount];
        String retryRoutingKey = RabbitConfig.retryRoutingKey(delay);

        log.info("Job [{}, {}] will retry in {}ms (attempt {}/{})", message.getJobId(), message.getJobName(), delay, retryCount + 1, RabbitConfig.MAX_RETRIES);

        // Update job status
        if (job != null) {
            job.setStatus(BackgroundJobStatus.PENDING_RETRY);
            job.setRetryCount(retryCount + 1);
            jobRepository.save(job);
        }

        // Send to retry queue
        System.out.println("Routing to: " + retryRoutingKey);
        rabbitTemplate.convertAndSend(RabbitConfig.JOB_EXCHANGE, retryRoutingKey, message);
    }

    private void handleMaxRetriesExceeded(BackgroundJob job, BackgroundJobMessage message, Exception error) {
        log.error("Job [{}, {}] exceeded max retries, sending to DLQ", message.getJobId(), message.getJobName());

        // Update job status in database
        if (job != null) {
            job.setStatus(BackgroundJobStatus.FAILED);
            job.setRetryCount(RabbitConfig.MAX_RETRIES);
            jobRepository.save(job);
        }

        // Send to DLQ with error details
        rabbitTemplate.convertAndSend(
                RabbitConfig.JOB_EXCHANGE,
                RabbitConfig.DLQ_ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties().setHeader("error-message", error.getMessage());
                    msg.getMessageProperties().setHeader("error-class", error.getClass().getName());
                    msg.getMessageProperties().setHeader("failed-at", LocalDateTime.now().toLocalDate());
                    msg.getMessageProperties().setHeader("retry-count", RabbitConfig.MAX_RETRIES);
                    return msg;
                }
        );
    }
}