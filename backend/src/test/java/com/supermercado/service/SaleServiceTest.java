package com.supermercado.service;

import com.supermercado.dto.CreateSaleRequest;
import com.supermercado.dto.SaleItemRequest;
import com.supermercado.entity.Product;
import com.supermercado.entity.Sale;
import com.supermercado.entity.SaleDetail;
import com.supermercado.repository.*;
import com.supermercado.service.impl.SaleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private SaleServiceImpl saleService;

    private Product product;

    @BeforeEach
    void setUp() {
        setField(saleService, "taxRate", 0.15);

        product = new Product();
        product.setId(1L);
        product.setName("Arroz");
        product.setSku("ARZ-001");
        product.setPurchasePrice(1000.0);
        product.setSalePrice(1200.0);
        product.setStock(10);
    }

    @Test
    void creatingSaleShouldDecreaseStockAndCalculateTotals() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSaleRequest request = new CreateSaleRequest();
        request.setPaymentMethod("efectivo");
        request.setDiscount(0.0);
        SaleItemRequest item = new SaleItemRequest();
        item.setProductId(1L);
        item.setQuantity(3);
        request.setItems(List.of(item));

        var response = saleService.createSale(request);

        assertEquals(7, product.getStock());
        assertEquals(3600.0, response.getSubtotal());
        assertEquals(540.0, response.getTax(), 0.001);
        assertEquals(4140.0, response.getTotal(), 0.001);
        assertEquals("EFECTIVO", response.getPaymentMethod());
    }

    @Test
    void creatingSaleWithInsufficientStockShouldFail() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        CreateSaleRequest request = new CreateSaleRequest();
        request.setPaymentMethod("EFECTIVO");
        SaleItemRequest item = new SaleItemRequest();
        item.setProductId(1L);
        item.setQuantity(50);
        request.setItems(List.of(item));

        assertThrows(IllegalStateException.class, () -> saleService.createSale(request));
    }

    @Test
    void creatingSaleWithInvalidPaymentMethodShouldFail() {
        CreateSaleRequest request = new CreateSaleRequest();
        request.setPaymentMethod("CRIPTO");
        SaleItemRequest item = new SaleItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);
        request.setItems(List.of(item));

        assertThrows(IllegalArgumentException.class, () -> saleService.createSale(request));
    }

    @Test
    void cancellingSaleShouldRestoreStock() {
        SaleDetail detail = new SaleDetail();
        detail.setProduct(product);
        detail.setQuantity(4);
        detail.setUnitPrice(1200.0);
        detail.setSubtotal(4800.0);

        Sale sale = new Sale();
        sale.setId(1L);
        sale.setStatus("COMPLETADA");
        sale.getDetails().add(detail);
        product.setStock(6);

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = saleService.cancelSale(1L);

        assertEquals(10, product.getStock());
        assertEquals("ANULADA", response.getStatus());
    }
}
