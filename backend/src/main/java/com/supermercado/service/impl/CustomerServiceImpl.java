package com.supermercado.service.impl;

import com.supermercado.dto.CustomerRequest;
import com.supermercado.dto.CustomerResponse;
import com.supermercado.entity.Customer;
import com.supermercado.repository.CustomerRepository;
import com.supermercado.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        Customer customer = new Customer();
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Override
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found");
        }
        customerRepository.deleteById(id);
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setName(request.getName());
        customer.setDocument(request.getDocument());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
    }

    private CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setDocument(customer.getDocument());
        response.setEmail(customer.getEmail());
        response.setPhone(customer.getPhone());
        response.setCreatedAt(customer.getCreatedAt());
        response.setCreatedBy(customer.getCreatedBy());
        return response;
    }
}
