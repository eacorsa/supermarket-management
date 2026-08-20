package com.supermercado.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreatePurchaseRequest {
    @NotNull
    private Long supplierId;

    @NotEmpty
    @Valid
    private List<PurchaseItemRequest> items;
}
