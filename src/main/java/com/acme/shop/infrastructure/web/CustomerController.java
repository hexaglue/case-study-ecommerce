package com.acme.shop.infrastructure.web;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(customerUseCases.createCustomer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.ok(customerUseCases.updateCustomer(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerUseCases.getCustomer(id));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerUseCases.getAllCustomers());
    }
}
