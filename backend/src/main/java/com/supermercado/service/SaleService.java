package com.supermercado.service;

import com.supermercado.dto.CreateSaleRequest;
import com.supermercado.dto.SaleResponse;

import java.util.List;

public interface SaleService {
    SaleResponse createSale(CreateSaleRequest request);
    SaleResponse cancelSale(Long saleId);
    List<SaleResponse> getAllSales();
}
