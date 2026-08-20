package com.supermercado.controller;

import com.supermercado.dto.CreatePurchaseRequest;
import com.supermercado.dto.PurchaseResponse;
import com.supermercado.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('GERENTE') or hasRole('ALMACENISTA')")
    public ResponseEntity<PurchaseResponse> createPurchase(@Valid @RequestBody CreatePurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.createPurchase(request));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasRole('ADMIN') or hasRole('GERENTE') or hasRole('ALMACENISTA')")
    public ResponseEntity<PurchaseResponse> receivePurchase(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.receivePurchase(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('GERENTE') or hasRole('ALMACENISTA')")
    public ResponseEntity<List<PurchaseResponse>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }
}
