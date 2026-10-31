package com.ecommerce.platform.service.impl;

import com.ecommerce.platform.messaging.email.EmailNotificationMessage;
import com.ecommerce.platform.repository.EmailDeadLetterRepository;
import com.ecommerce.platform.dto.response.EmailDeadLetterSummary;
import com.ecommerce.platform.service.EmailDeadLetterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeadLetterServiceImpl implements EmailDeadLetterService {

    private final EmailDeadLetterRepository deadLetterRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.messaging.email-exchange}")
    private String exchange;

    @Value("${app.messaging.email-routing-key}")
    private String routingKey;

    @Override
    public List<EmailDeadLetterSummary> list(int limit) {
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 100");
        }
        deadLetterRepository.releaseStaleReplayClaims();
        return deadLetterRepository.findUnresolved(limit);
    }

    @Override
    public void markResolved(UUID eventId) {
        deadLetterRepository.markResolved(eventId);
    }

    @Override
    public void replay(UUID eventId) {
        EmailNotificationMessage message = deadLetterRepository.findMessage(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dead letter not found"));
        if (!deadLetterRepository.claimReplay(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Dead letter is already being replayed or has been resolved");
        }

        try {
            CorrelationData correlation = new CorrelationData(eventId + "-replay-" + UUID.randomUUID());
            rabbitTemplate.convertAndSend(exchange, routingKey, message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ did not confirm the replayed message");
            }
            deadLetterRepository.markReplayQueued(eventId);
            log.info("Email dead letter replay queued eventId={} replayCountIncremented=true", eventId);
        } catch (InterruptedException exception) {
            deadLetterRepository.releaseReplayClaim(eventId);
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Replay was interrupted", exception);
        } catch (ExecutionException | TimeoutException | AmqpException | IllegalStateException exception) {
            deadLetterRepository.releaseReplayClaim(eventId);
            log.error("Unable to replay email dead letter eventId={}", eventId, exception);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Unable to publish the replayed message", exception);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void releaseStaleReplayClaims() {
        int released = deadLetterRepository.releaseStaleReplayClaims();
        if (released > 0) {
            log.warn("Released stale email dead-letter replay claims count={}", released);
        }
    }
}
