package com.supermercado.service;

import com.supermercado.dto.CreateSaleRequest;
import com.supermercado.dto.SaleResponse;

import java.util.List;

public interface SaleService {
    SaleResponse createSale(CreateSaleRequest request);
    SaleResponse quoteSale(CreateSaleRequest request);
    SaleResponse getSale(Long id);
    SaleResponse cancelSale(Long saleId);
    List<SaleResponse> getAllSales();
}
