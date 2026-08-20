package com.supermercado.service.impl;

import com.supermercado.dto.*;
import com.supermercado.entity.*;
import com.supermercado.repository.InventoryMovementRepository;
import com.supermercado.repository.ProductRepository;
import com.supermercado.repository.PurchaseRepository;
import com.supermercado.repository.SupplierRepository;
import com.supermercado.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Override
    @Transactional
    public PurchaseResponse createPurchase(CreatePurchaseRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setStatus("PENDING");

        double total = 0.0;
        for (PurchaseItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));

            PurchaseDetail detail = new PurchaseDetail();
            detail.setPurchase(purchase);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(item.getUnitPrice());
            detail.setSubtotal(item.getQuantity() * item.getUnitPrice());

            purchase.getDetails().add(detail);
            total += detail.getSubtotal();
        }
        purchase.setTotal(total);

        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseResponse receivePurchase(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));

        if ("RECEIVED".equals(purchase.getStatus())) {
            throw new IllegalStateException("Purchase already received");
        }

        for (PurchaseDetail detail : purchase.getDetails()) {
            Product product = detail.getProduct();
            product.setStock(product.getStock() + detail.getQuantity());
            productRepository.save(product);

            InventoryMovement movement = new InventoryMovement();
            movement.setProduct(product);
            movement.setType("ENTRADA");
            movement.setQuantity(detail.getQuantity());
            movement.setNote("Recepción de compra #" + purchase.getId());
            inventoryMovementRepository.save(movement);
        }

        purchase.setStatus("RECEIVED");
        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved);
    }

    @Override
    public List<PurchaseResponse> getAllPurchases() {
        return purchaseRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private PurchaseResponse toResponse(Purchase purchase) {
        PurchaseResponse response = new PurchaseResponse();
        response.setId(purchase.getId());
        response.setSupplierName(purchase.getSupplier().getName());
        response.setPurchaseDate(purchase.getPurchaseDate());
        response.setStatus(purchase.getStatus());
        response.setTotal(purchase.getTotal());
        response.setDetails(purchase.getDetails().stream().map(detail -> {
            PurchaseDetailResponse detailResponse = new PurchaseDetailResponse();
            detailResponse.setProductName(detail.getProduct().getName());
            detailResponse.setQuantity(detail.getQuantity());
            detailResponse.setUnitPrice(detail.getUnitPrice());
            detailResponse.setSubtotal(detail.getSubtotal());
            return detailResponse;
        }).collect(Collectors.toList()));
        return response;
    }
}
