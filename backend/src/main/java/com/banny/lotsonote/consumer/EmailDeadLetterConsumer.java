package com.banny.lotsonote.consumer;

import com.banny.lotsonote.config.RabbitMQConfig;
import com.banny.lotsonote.mapper.EmailSendFailureMapper;
import com.banny.lotsonote.model.entity.EmailSendFailure;
import com.banny.lotsonote.task.email.EmailTask;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
public class EmailDeadLetterConsumer {

    @Autowired
    private EmailSendFailureMapper emailSendFailureMapper;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_DLQ)
    public void handleDeadLetter(EmailTask emailTask, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            EmailSendFailure emailSendFailure = new EmailSendFailure();
            emailSendFailure.setTaskId(emailTask.getTaskId());
            emailSendFailure.setEmail(emailTask.getEmail());
            emailSendFailure.setType(emailTask.getType());
            emailSendFailure.setRetryCount(emailTask.getRetryCount());
            emailSendFailure.setReason(emailTask.getFailureReason());
            emailSendFailure.setCreatedAt(toLocalDateTime(emailTask.getCreatedAt()));
            emailSendFailure.setFailedAt(LocalDateTime.now());
            emailSendFailure.setTraceId(emailTask.getTraceId());
            emailSendFailure.setExpiredFlag(System.currentTimeMillis() >= emailTask.getExpireAt());
            emailSendFailureMapper.insert(emailSendFailure);

            log.error("邮件任务最终失败并已记录审计, taskId={}, email={}", emailTask.getTaskId(), emailTask.getEmail());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("死信队列处理失败, taskId={}", emailTask.getTaskId(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackException) {
                log.error("死信队列拒绝消息失败, taskId={}", emailTask.getTaskId(), nackException);
            }
        }
    }

    private LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}
