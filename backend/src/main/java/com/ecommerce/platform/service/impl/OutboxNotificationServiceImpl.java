package com.ecommerce.platform.service.impl;

import com.ecommerce.platform.common.OrderStatus;
import com.ecommerce.platform.repository.OutboxRepository;
import com.ecommerce.platform.service.OutboxNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxNotificationServiceImpl implements OutboxNotificationService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void userRegistered(Long userId) {
        enqueue("USER_REGISTERED", "USER", userId, Map.of("userId", userId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void orderCreated(Long orderId) {
        enqueue("ORDER_CREATED", "ORDER", orderId, Map.of("orderId", orderId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void paymentSucceeded(Long orderId) {
        enqueue("PAYMENT_SUCCEEDED", "ORDER", orderId, Map.of("orderId", orderId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void orderStatusChanged(Long orderId, OrderStatus status) {
        enqueue(
                "ORDER_STATUS_CHANGED",
                "ORDER",
                orderId,
                Map.of("orderId", orderId, "status", status.name()));
    }

    private void enqueue(
            String eventType,
            String aggregateType,
            Long aggregateId,
            Map<String, Object> payload) {
        UUID eventId = UUID.randomUUID();
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxRepository.enqueue(eventId, eventType, aggregateType, aggregateId, json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event " + eventType, exception);
        }
    }
}
