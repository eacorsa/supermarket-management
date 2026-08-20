package com.supermercado.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProductRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String sku;

    private Long categoryId;
    private Long supplierId;

    @NotNull
    @Min(0)
    private Double purchasePrice;

    @NotNull
    @Min(0)
    private Double salePrice;

    @NotNull
    @Min(0)
    private Integer stock;

    private String unitOfMeasure;
    private Integer minStock;
}
