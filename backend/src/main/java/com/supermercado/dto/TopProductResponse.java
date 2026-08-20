package com.supermercado.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse {
    private String productName;
    private Long totalQuantity;
    private Double totalRevenue;
}
