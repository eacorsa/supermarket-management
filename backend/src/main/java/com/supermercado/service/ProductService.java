package com.supermercado.service;

import com.supermercado.dto.CreateProductRequest;
import com.supermercado.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);
    List<ProductResponse> getAllProducts();
    ProductResponse updateProduct(Long id, CreateProductRequest request);
    void deleteProduct(Long id);
}
