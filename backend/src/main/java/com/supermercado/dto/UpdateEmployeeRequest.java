package com.supermercado.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Set;

@Data
public class UpdateEmployeeRequest {
    @NotBlank
    private String fullName;
    @NotBlank
    @Email
    private String email;
    // Null or blank preserves the current password. Username is immutable.
    private String password;
    @NotEmpty
    private Set<@NotBlank String> roles;
}
