package com.acme.shop.service;

import com.acme.shop.dto.CreateCustomerRequest;
import com.acme.shop.dto.CustomerResponse;
import com.acme.shop.model.Customer;
import com.acme.shop.repository.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }

        // Anti-pattern: no email validation in domain, just basic check here
        if (request.email() == null || !request.email().contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        Customer customer = new Customer(request.firstName(), request.lastName(), request.email());
        customer.setPhone(request.phone());
        customer.setStreet(request.street());
        customer.setCity(request.city());
        customer.setZipCode(request.zipCode());
        customer.setCountry(request.country());

        return toResponse(customerRepository.save(customer));
    }

    public CustomerResponse updateCustomer(Long customerId, CreateCustomerRequest request) {
        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setPhone(request.phone());
        customer.setStreet(request.street());
        customer.setCity(request.city());
        customer.setZipCode(request.zipCode());
        customer.setCountry(request.country());

        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStreet(),
                customer.getCity(),
                customer.getZipCode(),
                customer.getCountry());
    }
}
