package com.supermercado.dto;

import lombok.Data;

@Data
public class SaleDetailResponse {
    private Long productId;
    private String sku;
    private String barcode;
    private String productName;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
}
