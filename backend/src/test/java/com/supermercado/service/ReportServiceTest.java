package com.supermercado.service;

import com.supermercado.entity.Product;
import com.supermercado.entity.Sale;
import com.supermercado.repository.ProductRepository;
import com.supermercado.repository.SaleDetailRepository;
import com.supermercado.repository.SaleRepository;
import com.supermercado.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private SaleDetailRepository saleDetailRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void salesSummaryShouldGroupSalesByDayAndSumTotals() {
        Sale sale1 = new Sale();
        sale1.setSaleDate(LocalDateTime.of(2026, 8, 1, 10, 0));
        sale1.setTotal(1000.0);
        sale1.setTax(150.0);

        Sale sale2 = new Sale();
        sale2.setSaleDate(LocalDateTime.of(2026, 8, 1, 15, 0));
        sale2.setTotal(500.0);
        sale2.setTax(75.0);

        Sale sale3 = new Sale();
        sale3.setSaleDate(LocalDateTime.of(2026, 8, 2, 9, 0));
        sale3.setTotal(2000.0);
        sale3.setTax(300.0);

        when(saleRepository.findCompletedSalesBetween(any(), any())).thenReturn(List.of(sale1, sale2, sale3));

        var summary = reportService.getSalesSummary(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));

        assertEquals(3, summary.getTotalSalesCount());
        assertEquals(3500.0, summary.getTotalRevenue());
        assertEquals(525.0, summary.getTotalTax());
        assertEquals(2, summary.getDailyBreakdown().size());
        assertEquals(1500.0, summary.getDailyBreakdown().get(0).getRevenue());
        assertEquals(2000.0, summary.getDailyBreakdown().get(1).getRevenue());
    }

    @Test
    void salesSummaryWithInvertedDatesShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> reportService.getSalesSummary(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 1)));
    }

    @Test
    void lowStockShouldReturnProductsAtOrBelowMinStock() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Aceite");
        product.setStock(2);
        product.setMinStock(5);

        when(productRepository.findLowStockProducts()).thenReturn(List.of(product));

        var lowStock = reportService.getLowStockProducts();

        assertEquals(1, lowStock.size());
        assertEquals("Aceite", lowStock.get(0).getName());
    }
}
