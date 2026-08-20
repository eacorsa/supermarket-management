package com.supermercado.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateEmployeeRolesRequest {
    @NotEmpty
    private Set<String> roles;
}
