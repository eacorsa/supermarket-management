package com.supermercado.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class CreateSaleRequest {
    private Long customerId;

    @NotBlank
    private String paymentMethod;

    @DecimalMin("0.0")
    private Double discount = 0.0;

    @DecimalMin("0.0")
    private Double expectedTotal;

    @DecimalMin("0.0")
    private Double cashReceived;

    @DecimalMin("0.0")
    private Double cardAmount;

    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private String checkoutKey;

    @NotEmpty
    @Valid
    private List<SaleItemRequest> items;
}
