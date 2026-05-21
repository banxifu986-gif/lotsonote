package com.banny.lotsonote.consumer;

import com.banny.lotsonote.config.RabbitMQConfig;
import com.banny.lotsonote.model.enums.redisKey.RedisKey;
import com.banny.lotsonote.task.email.EmailTask;
import com.rabbitmq.client.Channel;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class EmailConsumer {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${mail.verify-code.expire-minutes}")
    private int expireMinutes;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleEmailTask(EmailTask emailTask, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            if (isTaskDone(emailTask.getTaskId())) {
                log.info("邮件任务已处理过，直接确认, taskId={}", emailTask.getTaskId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (isExpired(emailTask)) {
                log.warn("邮件任务已过期，转入死信队列, taskId={}", emailTask.getTaskId());
                deadLetter(emailTask, "TASK_EXPIRED");
                channel.basicAck(deliveryTag, false);
                return;
            }

            sendEmail(emailTask);
            markTaskDone(emailTask.getTaskId());
            channel.basicAck(deliveryTag, false);
            log.info("邮件发送成功, taskId={}, email={}", emailTask.getTaskId(), emailTask.getEmail());
        } catch (Exception e) {
            log.error("邮件发送失败, taskId={}, email={}", emailTask.getTaskId(), emailTask.getEmail(), e);
            try {
                if (shouldRetry(emailTask)) {
                    EmailTask retryTask = buildRetryTask(emailTask, e.getMessage());
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.EMAIL_RETRY_EXCHANGE,
                            RabbitMQConfig.EMAIL_RETRY_ROUTING_KEY,
                            retryTask
                    );
                    log.warn("邮件任务已转入重试队列, taskId={}, retryCount={}", retryTask.getTaskId(), retryTask.getRetryCount());
                } else {
                    deadLetter(emailTask, e.getMessage());
                }
                channel.basicAck(deliveryTag, false);
            } catch (Exception ackException) {
                log.error("邮件失败消息处理异常, taskId={}", emailTask.getTaskId(), ackException);
                try {
                    channel.basicNack(deliveryTag, false, false);
                } catch (Exception nackException) {
                    log.error("邮件失败消息拒绝异常, taskId={}", emailTask.getTaskId(), nackException);
                }
            }
        }
    }

    private void sendEmail(EmailTask emailTask) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(emailTask.getEmail());
        helper.setSubject("【Lotso笔记】邮箱验证码");

        Context context = new Context();
        context.setVariable("verifyCode", emailTask.getCode());
        context.setVariable("expireMinutes", expireMinutes);
        String htmlContent = templateEngine.process("mail/verify-code", context);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);

        String redisKey = RedisKey.verificationCode(emailTask.getType(), emailTask.getEmail());
        long ttlMillis = Math.max(emailTask.getExpireAt() - System.currentTimeMillis(), 1L);
        redisTemplate.opsForValue().set(redisKey, emailTask.getCode(), ttlMillis, TimeUnit.MILLISECONDS);
    }

    private boolean isTaskDone(String taskId) {
        return redisTemplate.opsForValue().get(RedisKey.emailTaskDone(taskId)) != null;
    }

    private void markTaskDone(String taskId) {
        redisTemplate.opsForValue().set(
                RedisKey.emailTaskDone(taskId),
                "1",
                TimeUnit.MINUTES.toMillis(expireMinutes),
                TimeUnit.MILLISECONDS
        );
    }

    private boolean isExpired(EmailTask emailTask) {
        return System.currentTimeMillis() >= emailTask.getExpireAt();
    }

    private boolean shouldRetry(EmailTask emailTask) {
        return !isExpired(emailTask) && emailTask.getRetryCount() < RabbitMQConfig.EMAIL_MAX_RETRY_COUNT;
    }

    private EmailTask buildRetryTask(EmailTask emailTask, String failureReason) {
        EmailTask retryTask = new EmailTask();
        retryTask.setTaskId(emailTask.getTaskId());
        retryTask.setEmail(emailTask.getEmail());
        retryTask.setCode(emailTask.getCode());
        retryTask.setType(emailTask.getType());
        retryTask.setRetryCount(emailTask.getRetryCount() + 1);
        retryTask.setCreatedAt(emailTask.getCreatedAt());
        retryTask.setExpireAt(emailTask.getExpireAt());
        retryTask.setTraceId(emailTask.getTraceId());
        retryTask.setRequestIp(emailTask.getRequestIp());
        retryTask.setFailureReason(failureReason);
        return retryTask;
    }

    private void deadLetter(EmailTask emailTask, String failureReason) {
        emailTask.setFailureReason(failureReason);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_DLX,
                RabbitMQConfig.EMAIL_DEAD_ROUTING_KEY,
                emailTask
        );
        log.error("邮件任务进入死信队列, taskId={}, email={}, reason={}", emailTask.getTaskId(), emailTask.getEmail(), failureReason);
    }
}
