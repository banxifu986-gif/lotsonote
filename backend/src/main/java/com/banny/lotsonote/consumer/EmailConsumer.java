package com.banny.lotsonote.consumer;

import com.banny.lotsonote.config.RabbitMQConfig;
import com.banny.lotsonote.model.enums.redisKey.RedisKey;
import com.banny.lotsonote.task.email.EmailTask;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
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

import jakarta.mail.internet.MimeMessage;
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

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${mail.verify-code.expire-minutes}")
    private int expireMinutes;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleEmailTask(EmailTask emailTask, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("开始处理邮件任务: {}", emailTask.getEmail());
            sendEmail(emailTask);
            channel.basicAck(deliveryTag, false);
            log.info("邮件发送成功: {}", emailTask.getEmail());
        } catch (Exception e) {
            log.error("邮件发送失败: {}", emailTask.getEmail(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                log.error("消息确认失败", ex);
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
        redisTemplate.opsForValue().set(redisKey, emailTask.getCode(), expireMinutes, TimeUnit.MINUTES);
    }
}
