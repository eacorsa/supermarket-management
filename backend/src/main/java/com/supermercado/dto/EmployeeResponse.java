package com.supermercado.dto;

import lombok.Data;

import java.util.Set;

@Data
public class EmployeeResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Set<String> roles;
}
