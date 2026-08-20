package com.supermercado.service;

import com.supermercado.dto.AuditRecordResponse;

import java.util.List;

public interface AuditService {
    List<AuditRecordResponse> getAuditLog(String entityType);
}
