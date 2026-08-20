package com.supermercado.service.impl;

import com.supermercado.dto.*;
import com.supermercado.entity.*;
import com.supermercado.repository.*;
import com.supermercado.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {
    private static final Set<String> VALID_PAYMENT_METHODS = Set.of("EFECTIVO", "TARJETA", "MIXTO");

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Value("${sales.tax-rate}")
    private double taxRate;

    @Override
    @Transactional
    public SaleResponse createSale(CreateSaleRequest request) {
        String paymentMethod = request.getPaymentMethod().toUpperCase();
        if (!VALID_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method: " + paymentMethod);
        }

        Sale sale = new Sale();
        sale.setPaymentMethod(paymentMethod);
        sale.setStatus("COMPLETADA");
        sale.setDiscount(request.getDiscount() != null ? request.getDiscount() : 0.0);

        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            sale.setCustomer(customer);
        }

        getCurrentUsername().flatMap(userRepository::findByUsername).ifPresent(sale::setCashier);

        double subtotal = 0.0;
        for (SaleItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));

            if (product.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }

            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            InventoryMovement movement = new InventoryMovement();
            movement.setProduct(product);
            movement.setType("SALIDA");
            movement.setQuantity(item.getQuantity());
            movement.setNote("Venta");
            inventoryMovementRepository.save(movement);

            SaleDetail detail = new SaleDetail();
            detail.setSale(sale);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(product.getSalePrice());
            detail.setSubtotal(item.getQuantity() * product.getSalePrice());

            sale.getDetails().add(detail);
            subtotal += detail.getSubtotal();
        }

        double tax = subtotal * taxRate;
        double total = subtotal + tax - sale.getDiscount();
        if (total < 0) {
            throw new IllegalArgumentException("Discount cannot be greater than subtotal plus tax");
        }

        sale.setSubtotal(subtotal);
        sale.setTax(tax);
        sale.setTotal(total);

        Sale saved = saleRepository.save(sale);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SaleResponse cancelSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found"));

        if ("ANULADA".equals(sale.getStatus())) {
            throw new IllegalStateException("Sale already cancelled");
        }

        for (SaleDetail detail : sale.getDetails()) {
            Product product = detail.getProduct();
            product.setStock(product.getStock() + detail.getQuantity());
            productRepository.save(product);

            InventoryMovement movement = new InventoryMovement();
            movement.setProduct(product);
            movement.setType("ENTRADA");
            movement.setQuantity(detail.getQuantity());
            movement.setNote("Anulación de venta #" + sale.getId());
            inventoryMovementRepository.save(movement);
        }

        sale.setStatus("ANULADA");
        Sale saved = saleRepository.save(sale);
        return toResponse(saved);
    }

    @Override
    public List<SaleResponse> getAllSales() {
        return saleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private java.util.Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(authentication.getName());
    }

    private SaleResponse toResponse(Sale sale) {
        SaleResponse response = new SaleResponse();
        response.setId(sale.getId());
        response.setCustomerName(sale.getCustomer() != null ? sale.getCustomer().getName() : "Consumidor final");
        response.setCashierName(sale.getCashier() != null ? sale.getCashier().getFullName() : null);
        response.setSaleDate(sale.getSaleDate());
        response.setSubtotal(sale.getSubtotal());
        response.setTax(sale.getTax());
        response.setDiscount(sale.getDiscount());
        response.setTotal(sale.getTotal());
        response.setPaymentMethod(sale.getPaymentMethod());
        response.setStatus(sale.getStatus());
        response.setDetails(sale.getDetails().stream().map(detail -> {
            SaleDetailResponse detailResponse = new SaleDetailResponse();
            detailResponse.setProductName(detail.getProduct().getName());
            detailResponse.setQuantity(detail.getQuantity());
            detailResponse.setUnitPrice(detail.getUnitPrice());
            detailResponse.setSubtotal(detail.getSubtotal());
            return detailResponse;
        }).collect(Collectors.toList()));
        return response;
    }
}
