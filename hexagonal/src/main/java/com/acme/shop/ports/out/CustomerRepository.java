package com.acme.shop.ports.out;

import com.acme.shop.domain.customer.Customer;
import com.acme.shop.domain.customer.CustomerId;
import com.acme.shop.domain.customer.Email;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(CustomerId id);
    Optional<Customer> findByEmail(Email email);
    boolean existsByEmail(Email email);
    List<Customer> findAll();
}
