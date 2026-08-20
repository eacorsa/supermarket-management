package com.supermercado.dto;

import lombok.Data;

@Data
public class PurchaseDetailResponse {
    private String productName;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
}
