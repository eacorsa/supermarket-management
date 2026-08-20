package com.supermercado.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateSaleRequest {
    private Long customerId;

    @NotBlank
    private String paymentMethod;

    private Double discount = 0.0;

    @NotEmpty
    @Valid
    private List<SaleItemRequest> items;
}
