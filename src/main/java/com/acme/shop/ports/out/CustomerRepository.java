package com.acme.shop.ports.out;

import com.acme.shop.domain.customer.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(Long id);
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Customer> findAll();
}
