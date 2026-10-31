package com.ecommerce.platform.messaging.dlq;

import com.ecommerce.platform.messaging.email.EmailNotificationMessage;
import com.ecommerce.platform.repository.EmailDeadLetterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDeadLetterConsumer {

    private final EmailDeadLetterRepository deadLetterRepository;

    @RabbitListener(
            queues = "${app.messaging.email-dead-letter-queue}",
            containerFactory = "emailDeadLetterListenerContainerFactory")
    public void capture(
            EmailNotificationMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            deadLetterRepository.capture(message);
        } catch (JsonProcessingException | DataAccessException exception) {
            log.error("Unable to persist dead-letter email eventId={}; requeueing",
                    message.eventId(), exception);
            channel.basicNack(deliveryTag, false, true);
            return;
        }
        channel.basicAck(deliveryTag, false);
    }
}
