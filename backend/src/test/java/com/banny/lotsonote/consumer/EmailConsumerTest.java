package com.banny.lotsonote.consumer;

import com.banny.lotsonote.config.RabbitMQConfig;
import com.banny.lotsonote.model.enums.redisKey.RedisKey;
import com.banny.lotsonote.task.email.EmailTask;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailConsumerTest {

    private EmailConsumer emailConsumer;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private Channel channel;

    private Message message;

    @BeforeEach
    public void setUp() {
        emailConsumer = new EmailConsumer();
        ReflectionTestUtils.setField(emailConsumer, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailConsumer, "templateEngine", templateEngine);
        ReflectionTestUtils.setField(emailConsumer, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(emailConsumer, "rabbitTemplate", rabbitTemplate);
        ReflectionTestUtils.setField(emailConsumer, "fromEmail", "sender@qq.com");
        ReflectionTestUtils.setField(emailConsumer, "expireMinutes", 15);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        message = new Message(new byte[0], properties);
    }

    @Test
    public void handleEmailTaskShouldSendOnceAndMarkDone() throws Exception {
        EmailTask emailTask = baseTask();
        when(valueOperations.get(RedisKey.emailTaskDone(emailTask.getTaskId()))).thenReturn(null);
        mockMailRender();

        emailConsumer.handleEmailTask(emailTask, message, channel);

        verify(mailSender).send(any(MimeMessage.class));
        verify(valueOperations).set(eq(RedisKey.verificationCode(emailTask.getType(), emailTask.getEmail())), eq(emailTask.getCode()), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(valueOperations).set(eq(RedisKey.emailTaskDone(emailTask.getTaskId())), eq("1"), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(channel).basicAck(1L, false);
    }

    @Test
    public void handleEmailTaskShouldSkipWhenTaskAlreadyDone() throws Exception {
        EmailTask emailTask = baseTask();
        when(valueOperations.get(RedisKey.emailTaskDone(emailTask.getTaskId()))).thenReturn("1");

        emailConsumer.handleEmailTask(emailTask, message, channel);

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(channel).basicAck(1L, false);
    }

    @Test
    public void handleEmailTaskShouldRouteToRetryQueueWhenSendFailsAndCanRetry() throws Exception {
        EmailTask emailTask = baseTask();
        when(valueOperations.get(RedisKey.emailTaskDone(emailTask.getTaskId()))).thenReturn(null);
        mockMailRender();
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        emailConsumer.handleEmailTask(emailTask, message, channel);

        ArgumentCaptor<EmailTask> taskCaptor = ArgumentCaptor.forClass(EmailTask.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EMAIL_RETRY_EXCHANGE),
                eq(RabbitMQConfig.EMAIL_RETRY_ROUTING_KEY),
                taskCaptor.capture()
        );
        verify(channel).basicAck(1L, false);
        org.junit.jupiter.api.Assertions.assertEquals(1, taskCaptor.getValue().getRetryCount());
    }

    @Test
    public void handleEmailTaskShouldRouteToDeadLetterWhenRetryExceeded() throws Exception {
        EmailTask emailTask = baseTask();
        emailTask.setRetryCount(RabbitMQConfig.EMAIL_MAX_RETRY_COUNT);
        when(valueOperations.get(RedisKey.emailTaskDone(emailTask.getTaskId()))).thenReturn(null);
        mockMailRender();
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        emailConsumer.handleEmailTask(emailTask, message, channel);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EMAIL_DLX),
                eq(RabbitMQConfig.EMAIL_DEAD_ROUTING_KEY),
                any(EmailTask.class)
        );
        verify(channel).basicAck(1L, false);
    }

    @Test
    public void handleEmailTaskShouldDeadLetterExpiredTaskWithoutSending() throws Exception {
        EmailTask emailTask = baseTask();
        emailTask.setExpireAt(System.currentTimeMillis() - 1000L);
        when(valueOperations.get(RedisKey.emailTaskDone(emailTask.getTaskId()))).thenReturn(null);

        emailConsumer.handleEmailTask(emailTask, message, channel);

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EMAIL_DLX),
                eq(RabbitMQConfig.EMAIL_DEAD_ROUTING_KEY),
                any(EmailTask.class)
        );
        verify(channel).basicAck(1L, false);
    }

    private EmailTask baseTask() {
        EmailTask emailTask = new EmailTask();
        emailTask.setTaskId("task-1");
        emailTask.setEmail("demo@qq.com");
        emailTask.setCode("123456");
        emailTask.setType("LOGIN");
        emailTask.setRetryCount(0);
        emailTask.setCreatedAt(System.currentTimeMillis());
        emailTask.setExpireAt(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15));
        emailTask.setTraceId("trace-1");
        emailTask.setRequestIp("127.0.0.1");
        return emailTask;
    }

    private void mockMailRender() {
        when(templateEngine.process(eq("mail/verify-code"), any())).thenReturn("<html>code</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }
}
