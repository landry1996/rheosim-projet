package com.rheosim.domain.shared;

public interface AuditPort {

    void log(AuditEvent event);
}
