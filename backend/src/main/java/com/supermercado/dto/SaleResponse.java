package com.supermercado.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleResponse {
    private Long id;
    private String customerName;
    private String cashierName;
    private LocalDateTime saleDate;
    private Double subtotal;
    private Double tax;
    private Double discount;
    private Double total;
    private String paymentMethod;
    private String status;
    private List<SaleDetailResponse> details;
}
