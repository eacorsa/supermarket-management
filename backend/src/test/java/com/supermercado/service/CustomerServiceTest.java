package com.supermercado.service;

import com.supermercado.dto.CustomerRequest;
import com.supermercado.entity.Customer;
import com.supermercado.repository.CustomerRepository;
import com.supermercado.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void creatingCustomerShouldPersistAllFields() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Juan Perez");
        request.setDocument("0102030405");
        request.setEmail("juan@correo.com");
        request.setPhone("0999999999");

        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = customerService.createCustomer(request);

        assertEquals("Juan Perez", response.getName());
        assertEquals("0102030405", response.getDocument());
        assertEquals("juan@correo.com", response.getEmail());
        assertEquals("0999999999", response.getPhone());
    }

    @Test
    void gettingAllCustomersShouldReturnMappedList() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Maria Lopez");

        when(customerRepository.findAll()).thenReturn(List.of(customer));

        var result = customerService.getAllCustomers();

        assertEquals(1, result.size());
        assertEquals("Maria Lopez", result.get(0).getName());
    }

    @Test
    void updatingExistingCustomerShouldOverwriteFields() {
        Customer existing = new Customer();
        existing.setId(1L);
        existing.setName("Nombre Viejo");

        CustomerRequest request = new CustomerRequest();
        request.setName("Nombre Nuevo");
        request.setEmail("nuevo@correo.com");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = customerService.updateCustomer(1L, request);

        assertEquals("Nombre Nuevo", response.getName());
        assertEquals("nuevo@correo.com", response.getEmail());
    }

    @Test
    void updatingNonExistingCustomerShouldFail() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Cualquiera");

        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> customerService.updateCustomer(99L, request));
    }

    @Test
    void deletingExistingCustomerShouldSucceed() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        customerService.deleteCustomer(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deletingNonExistingCustomerShouldFail() {
        when(customerRepository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> customerService.deleteCustomer(99L));
        verify(customerRepository, never()).deleteById(any());
    }
}
