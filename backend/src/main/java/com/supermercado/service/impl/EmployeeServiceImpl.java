package com.supermercado.service.impl;

import com.supermercado.dto.CreateEmployeeRequest;
import com.supermercado.dto.UpdateEmployeeRequest;
import com.supermercado.dto.EmployeeResponse;
import com.supermercado.dto.UpdateEmployeeRolesRequest;
import com.supermercado.entity.Role;
import com.supermercado.entity.User;
import com.supermercado.repository.RoleRepository;
import com.supermercado.repository.UserRepository;
import com.supermercado.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRoles(resolveRoles(request.getRoles()));

        return toResponse(userRepository.save(user));
    }

    @Override
    public List<EmployeeResponse> getAllEmployees() {
        return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public EmployeeResponse updateEmployeeRoles(Long id, UpdateEmployeeRolesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        user.setRoles(resolveRoles(request.getRoles()));
        return toResponse(userRepository.save(user));
    }

    @Override
    public void deleteEmployee(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Employee not found");
        }
        userRepository.deleteById(id);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        Set<Role> roles = resolveRoles(request.getRoles());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRoles(roles);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return toResponse(userRepository.save(user));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        return roleNames.stream()
                .map(name -> roleRepository.findByName(name.toUpperCase())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + name)))
                .collect(Collectors.toSet());
    }

    private EmployeeResponse toResponse(User user) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return response;
    }
}
