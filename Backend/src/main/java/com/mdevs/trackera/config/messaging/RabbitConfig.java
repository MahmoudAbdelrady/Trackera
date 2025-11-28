package com.mdevs.trackera.config.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitConfig {
    // Exchange
    public static final String JOB_EXCHANGE = "trackera.job.exchange";

    // Queues
    public static final String JOB_QUEUE = "trackera.job.queue";
    public static final String JOB_DLQ = "trackera.job.dlq";

    // Retry configuration
    public static final String RETRY_KEY_PREFIX = "trackera.job.retry.";
    public static final int[] RETRY_DELAYS = {1000, 5000, 10000, 20000}; // 1s, 5s, 10s, 20s
    public static final int MAX_RETRIES = RETRY_DELAYS.length;

    // Routing Keys
    public static final String JOB_ROUTING_KEY = "job.process";
    public static final String RETRY_ROUTING_KEY_PREFIX = "job.retry.";
    public static final String DLQ_ROUTING_KEY = "job.dlq";

    @Value("${trackera.rabbitmq.prefetch}")
    private int prefetchCount;

    @Bean
    public DirectExchange jobExchange() {
        return new DirectExchange(JOB_EXCHANGE, true, false);
    }

    // Main Queue
    @Bean
    public Queue jobQueue() {
        return QueueBuilder.durable(JOB_QUEUE).build();
    }

    // Retry Declarables
    @Bean
    public Declarables retryDeclarables(DirectExchange jobExchange) {
        List<Declarable> declarables = new ArrayList<>();
        for (int delay : RETRY_DELAYS) {
            Queue q = createRetryQueue(delay);
            Binding b = BindingBuilder.bind(q).to(jobExchange).with(retryRoutingKey(delay));
            declarables.add(q);
            declarables.add(b);
        }
        return new Declarables(declarables);
    }

    private Queue createRetryQueue(int delay) {
        return QueueBuilder.durable(retryQueueName(delay))
                .withArgument("x-message-ttl", delay)
                .withArgument("x-dead-letter-exchange", JOB_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", JOB_ROUTING_KEY)
                .build();
    }

    // DLQ
    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(JOB_DLQ).build();
    }

    // Bindings
    @Bean
    public Binding jobBinding(DirectExchange jobExchange) {
        return BindingBuilder.bind(jobQueue()).to(jobExchange).with(JOB_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(DirectExchange jobExchange) {
        return BindingBuilder.bind(dlq()).to(jobExchange).with(DLQ_ROUTING_KEY);
    }

    // helper methods
    public static String retryQueueName(int delay) {
        return RETRY_KEY_PREFIX + delay;
    }

    public static String retryRoutingKey(int delay) {
        return RETRY_ROUTING_KEY_PREFIX + delay;
    }

    // Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(prefetchCount);
        return factory;
    }
}
