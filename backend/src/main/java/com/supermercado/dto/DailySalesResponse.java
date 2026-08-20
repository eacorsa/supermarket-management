package com.supermercado.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailySalesResponse {
    private LocalDate date;
    private Long salesCount;
    private Double revenue;
}
