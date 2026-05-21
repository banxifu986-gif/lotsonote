package com.banny.lotsonote.consumer;

import com.banny.lotsonote.mapper.EmailSendFailureMapper;
import com.banny.lotsonote.model.entity.EmailSendFailure;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EmailDeadLetterConsumerTest {

    private EmailDeadLetterConsumer consumer;

    @Mock
    private EmailSendFailureMapper emailSendFailureMapper;

    @Mock
    private Channel channel;

    private Message message;

    @BeforeEach
    public void setUp() {
        consumer = new EmailDeadLetterConsumer();
        ReflectionTestUtils.setField(consumer, "emailSendFailureMapper", emailSendFailureMapper);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(10L);
        message = new Message(new byte[0], properties);
    }

    @Test
    public void handleDeadLetterShouldPersistFailureAuditAndAck() throws Exception {
        EmailTask emailTask = new EmailTask();
        emailTask.setTaskId("task-1");
        emailTask.setEmail("demo@qq.com");
        emailTask.setType("LOGIN");
        emailTask.setRetryCount(3);
        emailTask.setFailureReason("smtp down");
        emailTask.setCreatedAt(System.currentTimeMillis() - 1000L);
        emailTask.setExpireAt(System.currentTimeMillis() - 500L);
        emailTask.setTraceId("trace-1");

        consumer.handleDeadLetter(emailTask, message, channel);

        ArgumentCaptor<EmailSendFailure> captor = ArgumentCaptor.forClass(EmailSendFailure.class);
        verify(emailSendFailureMapper).insert(captor.capture());
        EmailSendFailure failure = captor.getValue();
        assertEquals("task-1", failure.getTaskId());
        assertEquals("demo@qq.com", failure.getEmail());
        assertEquals("smtp down", failure.getReason());
        assertEquals(true, failure.getExpiredFlag());
        verify(channel).basicAck(10L, false);
    }

    @Test
    public void handleDeadLetterShouldNotRepublishTask() throws Exception {
        EmailTask emailTask = new EmailTask();
        emailTask.setTaskId("task-2");
        emailTask.setEmail("demo@qq.com");
        emailTask.setType("LOGIN");
        emailTask.setRetryCount(3);
        emailTask.setFailureReason("smtp down");
        emailTask.setCreatedAt(System.currentTimeMillis());
        emailTask.setExpireAt(System.currentTimeMillis() + 1000L);

        consumer.handleDeadLetter(emailTask, message, channel);

        verify(emailSendFailureMapper).insert(org.mockito.ArgumentMatchers.any(EmailSendFailure.class));
        verify(channel).basicAck(10L, false);
        verify(channel, never()).basicNack(10L, false, false);
        verify(channel, never()).basicNack(10L, false, true);
    }
}
