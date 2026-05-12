package com.rheosim.infrastructure.shared.config;

import com.rheosim.domain.shared.AuditEvent;
import com.rheosim.domain.shared.AuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.UUID;

@Service
public class AuditService implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Async
    public void log(AuditEvent event) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO rheosim.audit_logs (id, user_id, action, entity, entity_id, details, ip_address, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)",
                    UUID.randomUUID(),
                    event.userId(),
                    event.action(),
                    event.entity(),
                    event.entityId(),
                    event.details() != null ? mapToJson(event.details()) : null,
                    event.ipAddress(),
                    Timestamp.from(event.timestamp())
            );
        } catch (Exception e) {
            log.error("Failed to persist audit event: action={}, entity={}", event.action(), event.entity(), e);
        }
    }

    private String mapToJson(java.util.Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        var entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {
                sb.append("\"").append(val).append("\"");
            } else {
                sb.append(val);
            }
            if (entries.hasNext()) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }
}
