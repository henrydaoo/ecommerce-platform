package com.ecommerce.platform.messaging.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxMetrics {

        private final JdbcTemplate jdbcTemplate;
        private final AtomicLong pendingCount = new AtomicLong();
        private final AtomicLong deadCount = new AtomicLong();
        private final AtomicLong oldestPendingSeconds = new AtomicLong();

        public OutboxMetrics(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
                this.jdbcTemplate = jdbcTemplate;
                Gauge.builder("app.outbox.pending", pendingCount, AtomicLong::doubleValue)
                                .register(meterRegistry);
                Gauge.builder("app.outbox.dead", deadCount, AtomicLong::doubleValue)
                                .register(meterRegistry);
                Gauge.builder("app.outbox.oldest.pending.seconds", oldestPendingSeconds, AtomicLong::doubleValue)
                                .register(meterRegistry);
        }

        @Scheduled(fixedDelayString = "${app.messaging.outbox-metrics-refresh-ms:10000}")
        public void refresh() {
                pendingCount.set(countByStatus("PENDING"));
                deadCount.set(countByStatus("DEAD"));
                Long oldestAge = jdbcTemplate.queryForObject("""
                                SELECT COALESCE(EXTRACT(EPOCH FROM (now() - MIN(created_at)))::bigint, 0)
                                FROM outbox_events
                                WHERE status = 'PENDING'
                                """, Long.class);
                oldestPendingSeconds.set(oldestAge == null ? 0 : oldestAge);
        }

        private long countByStatus(String status) {
                Long count = jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM outbox_events WHERE status = ?", Long.class, status);
                return count == null ? 0 : count;
        }
}
