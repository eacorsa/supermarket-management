package com.supermercado.service;

import com.supermercado.dto.LowStockProductResponse;
import com.supermercado.dto.SalesSummaryResponse;
import com.supermercado.dto.TopProductResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    SalesSummaryResponse getSalesSummary(LocalDate startDate, LocalDate endDate);
    List<TopProductResponse> getTopSellingProducts(int limit);
    List<LowStockProductResponse> getLowStockProducts();
}
