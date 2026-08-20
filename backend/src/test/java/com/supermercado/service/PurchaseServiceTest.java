package com.supermercado.service;

import com.supermercado.dto.CreatePurchaseRequest;
import com.supermercado.dto.PurchaseItemRequest;
import com.supermercado.entity.Product;
import com.supermercado.entity.Purchase;
import com.supermercado.entity.PurchaseDetail;
import com.supermercado.entity.Supplier;
import com.supermercado.repository.InventoryMovementRepository;
import com.supermercado.repository.ProductRepository;
import com.supermercado.repository.PurchaseRepository;
import com.supermercado.repository.SupplierRepository;
import com.supermercado.service.impl.PurchaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private Product product;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Arroz");
        product.setSku("ARZ-001");
        product.setPurchasePrice(1000.0);
        product.setSalePrice(1200.0);
        product.setStock(10);

        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Proveedor A");

        PurchaseDetail detail = new PurchaseDetail();
        detail.setProduct(product);
        detail.setQuantity(5);
        detail.setUnitPrice(1000.0);
        detail.setSubtotal(5000.0);

        purchase = new Purchase();
        purchase.setId(1L);
        purchase.setSupplier(supplier);
        purchase.setStatus("PENDING");
        purchase.getDetails().add(detail);
    }

    @Test
    void receivingPurchaseShouldIncreaseProductStock() {
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        purchaseService.receivePurchase(1L);

        assertEquals(15, product.getStock());
        assertEquals("RECEIVED", purchase.getStatus());
    }

    @Test
    void receivingAnAlreadyReceivedPurchaseShouldFail() {
        purchase.setStatus("RECEIVED");
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));

        assertThrows(IllegalStateException.class, () -> purchaseService.receivePurchase(1L));
    }
}
