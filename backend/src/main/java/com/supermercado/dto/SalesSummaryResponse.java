package com.supermercado.dto;

import lombok.Data;

import java.util.List;

@Data
public class SalesSummaryResponse {
    private Long totalSalesCount;
    private Double totalRevenue;
    private Double totalTax;
    private List<DailySalesResponse> dailyBreakdown;
}
