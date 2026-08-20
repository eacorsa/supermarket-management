package com.supermercado.controller;

import com.supermercado.dto.AuditRecordResponse;
import com.supermercado.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditRecordResponse>> getAuditLog(
            @RequestParam(required = false) String entityType) {
        return ResponseEntity.ok(auditService.getAuditLog(entityType));
    }
}
