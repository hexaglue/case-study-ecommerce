package com.acme.shop.application;

import com.acme.shop.domain.customer.Customer;
import com.acme.shop.domain.customer.CustomerId;
import com.acme.shop.domain.customer.Email;
import com.acme.shop.domain.order.Address;
import com.acme.shop.ports.in.CustomerUseCases;
import com.acme.shop.ports.out.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerApplicationService implements CustomerUseCases {

    private final CustomerRepository customerRepository;

    public CustomerApplicationService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer createCustomer(String firstName, String lastName, Email email, String phone, Address address) {
        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use: " + email.value());
        }

        Customer customer = Customer.create(firstName, lastName, email);
        if (phone != null) {
            customer.updateProfile(firstName, lastName, phone);
        }
        if (address != null) {
            customer.updateAddress(address);
        }

        return customerRepository.save(customer);
    }

    @Override
    public Customer updateCustomer(CustomerId customerId, String firstName, String lastName, String phone, Address address) {
        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        customer.updateProfile(firstName, lastName, phone);
        if (address != null) {
            customer.updateAddress(address);
        }

        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomer(CustomerId customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
}
