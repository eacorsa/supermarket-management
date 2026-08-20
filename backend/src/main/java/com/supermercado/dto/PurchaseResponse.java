package com.supermercado.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseResponse {
    private Long id;
    private String supplierName;
    private LocalDateTime purchaseDate;
    private String status;
    private Double total;
    private List<PurchaseDetailResponse> details;
}
