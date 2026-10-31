package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.response.EmailDeadLetterSummary;

import java.util.List;
import java.util.UUID;

public interface EmailDeadLetterService {
    List<EmailDeadLetterSummary> list(int limit);

    void markResolved(UUID eventId);

    void replay(UUID eventId);
}
