package com.banny.lotsonote.consumer;

import com.banny.lotsonote.config.RabbitMQConfig;
import com.banny.lotsonote.task.email.EmailTask;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailDeadLetterConsumer {

    @RabbitListener(queues = RabbitMQConfig.EMAIL_DLQ)
    public void handleDeadLetter(EmailTask emailTask, Message message, Channel channel) {
        try {
            log.error("邮件发送最终失败，进入死信队列: {}", emailTask.getEmail());
            // 可以记录到数据库或发送告警
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("死信队列处理失败", e);
        }
    }
}
