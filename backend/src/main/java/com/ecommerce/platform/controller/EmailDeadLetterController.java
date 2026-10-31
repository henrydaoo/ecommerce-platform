package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.response.EmailDeadLetterSummary;
import com.ecommerce.platform.service.EmailDeadLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/email-dead-letters")
@RequiredArgsConstructor
public class EmailDeadLetterController {

    private final EmailDeadLetterService deadLetterService;

    @GetMapping
    public ResponseEntity<List<EmailDeadLetterSummary>> list(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(deadLetterService.list(limit));
    }

    @PostMapping("/{eventId}/replay")
    public ResponseEntity<Void> replay(@PathVariable UUID eventId) {
        deadLetterService.replay(eventId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
