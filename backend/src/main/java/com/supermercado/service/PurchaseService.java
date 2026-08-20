package com.supermercado.service;

import com.supermercado.dto.CreatePurchaseRequest;
import com.supermercado.dto.PurchaseResponse;

import java.util.List;

public interface PurchaseService {
    PurchaseResponse createPurchase(CreatePurchaseRequest request);
    PurchaseResponse receivePurchase(Long purchaseId);
    List<PurchaseResponse> getAllPurchases();
}
