package com.supermercado.service;

import com.supermercado.dto.CreateEmployeeRequest;
import com.supermercado.entity.Role;
import com.supermercado.entity.User;
import com.supermercado.repository.RoleRepository;
import com.supermercado.repository.UserRepository;
import com.supermercado.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void creatingEmployeeWithExistingUsernameShouldFail() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setUsername("cajero1");
        request.setPassword("pass123");
        request.setFullName("Cajero Uno");
        request.setEmail("cajero1@supermercado.com");
        request.setRoles(Set.of("CAJERO"));

        when(userRepository.existsByUsername("cajero1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    void creatingEmployeeWithInvalidRoleShouldFail() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setUsername("nuevo");
        request.setPassword("pass123");
        request.setFullName("Nuevo Empleado");
        request.setEmail("nuevo@supermercado.com");
        request.setRoles(Set.of("SUPERUSUARIO"));

        when(userRepository.existsByUsername("nuevo")).thenReturn(false);
        when(roleRepository.findByName("SUPERUSUARIO")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    void creatingEmployeeWithValidRoleShouldAssignRole() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setUsername("nuevo");
        request.setPassword("pass123");
        request.setFullName("Nuevo Empleado");
        request.setEmail("nuevo@supermercado.com");
        request.setRoles(Set.of("CAJERO"));

        Role cajeroRole = new Role();
        cajeroRole.setId(1L);
        cajeroRole.setName("CAJERO");

        when(userRepository.existsByUsername("nuevo")).thenReturn(false);
        when(roleRepository.findByName("CAJERO")).thenReturn(Optional.of(cajeroRole));
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = employeeService.createEmployee(request);

        assertEquals(Set.of("CAJERO"), response.getRoles());
    }
}
