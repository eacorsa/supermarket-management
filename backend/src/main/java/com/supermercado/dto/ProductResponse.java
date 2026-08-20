package com.supermercado.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String sku;
    private String categoryName;
    private String supplierName;
    private Double purchasePrice;
    private Double salePrice;
    private Integer stock;
    private String unitOfMeasure;
    private Integer minStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
