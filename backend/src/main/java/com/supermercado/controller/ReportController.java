package com.supermercado.controller;

import com.supermercado.dto.LowStockProductResponse;
import com.supermercado.dto.SalesSummaryResponse;
import com.supermercado.dto.TopProductResponse;
import com.supermercado.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('GERENTE')")
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/sales-summary")
    public ResponseEntity<SalesSummaryResponse> getSalesSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getSalesSummary(startDate, endDate));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductResponse>> getTopSellingProducts(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(reportService.getTopSellingProducts(limit));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockProductResponse>> getLowStockProducts() {
        return ResponseEntity.ok(reportService.getLowStockProducts());
    }
}
