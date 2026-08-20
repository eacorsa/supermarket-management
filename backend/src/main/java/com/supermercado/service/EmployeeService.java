package com.supermercado.service;

import com.supermercado.dto.CreateEmployeeRequest;
import com.supermercado.dto.EmployeeResponse;
import com.supermercado.dto.UpdateEmployeeRolesRequest;

import java.util.List;

public interface EmployeeService {
    EmployeeResponse createEmployee(CreateEmployeeRequest request);
    List<EmployeeResponse> getAllEmployees();
    EmployeeResponse updateEmployeeRoles(Long id, UpdateEmployeeRolesRequest request);
    void deleteEmployee(Long id);
}
