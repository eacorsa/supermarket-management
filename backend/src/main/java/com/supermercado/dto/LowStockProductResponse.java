package com.supermercado.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LowStockProductResponse {
    private Long productId;
    private String name;
    private Integer stock;
    private Integer minStock;
}
