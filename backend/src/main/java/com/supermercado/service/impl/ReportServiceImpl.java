package com.supermercado.service.impl;

import com.supermercado.dto.DailySalesResponse;
import com.supermercado.dto.LowStockProductResponse;
import com.supermercado.dto.SalesSummaryResponse;
import com.supermercado.dto.TopProductResponse;
import com.supermercado.entity.Product;
import com.supermercado.entity.Sale;
import com.supermercado.repository.ProductRepository;
import com.supermercado.repository.SaleDetailRepository;
import com.supermercado.repository.SaleRepository;
import com.supermercado.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final SaleRepository saleRepository;
    private final SaleDetailRepository saleDetailRepository;
    private final ProductRepository productRepository;

    @Override
    public SalesSummaryResponse getSalesSummary(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        List<Sale> sales = saleRepository.findCompletedSalesBetween(start, end);

        Map<LocalDate, List<Sale>> salesByDay = sales.stream()
                .collect(Collectors.groupingBy(sale -> sale.getSaleDate().toLocalDate()));

        List<DailySalesResponse> dailyBreakdown = salesByDay.entrySet().stream()
                .map(entry -> {
                    DailySalesResponse daily = new DailySalesResponse();
                    daily.setDate(entry.getKey());
                    daily.setSalesCount((long) entry.getValue().size());
                    daily.setRevenue(entry.getValue().stream().mapToDouble(Sale::getTotal).sum());
                    return daily;
                })
                .sorted(Comparator.comparing(DailySalesResponse::getDate))
                .collect(Collectors.toList());

        SalesSummaryResponse summary = new SalesSummaryResponse();
        summary.setTotalSalesCount((long) sales.size());
        summary.setTotalRevenue(sales.stream().mapToDouble(Sale::getTotal).sum());
        summary.setTotalTax(sales.stream().mapToDouble(Sale::getTax).sum());
        summary.setDailyBreakdown(dailyBreakdown);
        return summary;
    }

    @Override
    public List<TopProductResponse> getTopSellingProducts(int limit) {
        return saleDetailRepository.findTopSellingProducts(PageRequest.of(0, limit)).stream()
                .map(projection -> new TopProductResponse(
                        projection.getProductName(),
                        projection.getTotalQuantity(),
                        projection.getTotalRevenue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LowStockProductResponse> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(this::toLowStockResponse)
                .collect(Collectors.toList());
    }

    private LowStockProductResponse toLowStockResponse(Product product) {
        return new LowStockProductResponse(product.getId(), product.getName(), product.getStock(), product.getMinStock());
    }
}
