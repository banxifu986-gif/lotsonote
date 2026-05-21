package com.banny.lotsonote.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_ROUTING_KEY = "email.send";
    public static final String EMAIL_RETRY_QUEUE = "email.retry.queue";
    public static final String EMAIL_RETRY_EXCHANGE = "email.retry.exchange";
    public static final String EMAIL_RETRY_ROUTING_KEY = "email.retry";
    public static final String EMAIL_DLQ = "email.dlq";
    public static final String EMAIL_DLX = "email.dlx";
    public static final String EMAIL_DEAD_ROUTING_KEY = "email.dead";
    public static final int EMAIL_MAX_RETRY_COUNT = 3;
    public static final int EMAIL_RETRY_TTL_MILLIS = 30_000;

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .deadLetterExchange(EMAIL_DLX)
                .deadLetterRoutingKey(EMAIL_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange()).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Queue emailRetryQueue() {
        return QueueBuilder.durable(EMAIL_RETRY_QUEUE)
                .ttl(EMAIL_RETRY_TTL_MILLIS)
                .deadLetterExchange(EMAIL_EXCHANGE)
                .deadLetterRoutingKey(EMAIL_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange emailRetryExchange() {
        return new DirectExchange(EMAIL_RETRY_EXCHANGE);
    }

    @Bean
    public Binding emailRetryBinding() {
        return BindingBuilder.bind(emailRetryQueue()).to(emailRetryExchange()).with(EMAIL_RETRY_ROUTING_KEY);
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public DirectExchange emailDeadLetterExchange() {
        return new DirectExchange(EMAIL_DLX);
    }

    @Bean
    public Binding emailDeadLetterBinding() {
        return BindingBuilder.bind(emailDeadLetterQueue()).to(emailDeadLetterExchange()).with(EMAIL_DEAD_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                String correlationId = correlationData == null ? "unknown" : correlationData.getId();
                log.error("邮件任务投递未被 Broker 确认, correlationId={}, cause={}", correlationId, cause);
            }
        });
        template.setReturnsCallback(returned ->
                log.error(
                        "邮件任务路由失败, exchange={}, routingKey={}, replyCode={}, replyText={}",
                        returned.getExchange(),
                        returned.getRoutingKey(),
                        returned.getReplyCode(),
                        returned.getReplyText()
                )
        );
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
