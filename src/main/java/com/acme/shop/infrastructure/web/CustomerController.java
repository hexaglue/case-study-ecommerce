package com.acme.shop.infrastructure.web;

import com.acme.shop.domain.customer.Customer;
import com.acme.shop.domain.customer.CustomerId;
import com.acme.shop.domain.customer.Email;
import com.acme.shop.domain.order.Address;
import com.acme.shop.infrastructure.web.dto.CreateCustomerRequest;
import com.acme.shop.infrastructure.web.dto.CustomerResponse;
import com.acme.shop.ports.in.CustomerUseCases;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerUseCases customerUseCases;

    public CustomerController(CustomerUseCases customerUseCases) {
        this.customerUseCases = customerUseCases;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Address address = null;
        if (request.street() != null && !request.street().isBlank()) {
            address = new Address(request.street(), request.city(), request.zipCode(), request.country());
        }
        Customer customer = customerUseCases.createCustomer(
                request.firstName(), request.lastName(), new Email(request.email()),
                request.phone(), address);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody CreateCustomerRequest request) {
        Address address = null;
        if (request.street() != null && !request.street().isBlank()) {
            address = new Address(request.street(), request.city(), request.zipCode(), request.country());
        }
        Customer customer = customerUseCases.updateCustomer(
                new CustomerId(id), request.firstName(), request.lastName(), request.phone(), address);
        return ResponseEntity.ok(toResponse(customer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        Customer customer = customerUseCases.getCustomer(new CustomerId(id));
        return ResponseEntity.ok(toResponse(customer));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<Customer> customers = customerUseCases.getAllCustomers();
        return ResponseEntity.ok(customers.stream().map(this::toResponse).toList());
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId() != null ? customer.getId().value() : null,
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail().value(),
                customer.getPhone(),
                customer.getAddress() != null ? customer.getAddress().street() : null,
                customer.getAddress() != null ? customer.getAddress().city() : null,
                customer.getAddress() != null ? customer.getAddress().zipCode() : null,
                customer.getAddress() != null ? customer.getAddress().country() : null);
    }
}
