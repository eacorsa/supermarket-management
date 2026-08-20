package com.supermercado.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerResponse {
    private Long id;
    private String name;
    private String document;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private String createdBy;
}
