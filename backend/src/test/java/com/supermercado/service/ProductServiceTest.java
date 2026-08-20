package com.supermercado.service;

import com.supermercado.dto.CreateProductRequest;
import com.supermercado.repository.ProductRepository;
import com.supermercado.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void shouldRejectProductWithSalePriceLowerThanPurchasePrice() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Arroz");
        request.setSku("ARZ-001");
        request.setPurchasePrice(1200.0);
        request.setSalePrice(1000.0);
        request.setStock(10);

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(request));
    }
}
