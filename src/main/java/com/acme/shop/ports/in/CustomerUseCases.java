package com.acme.shop.ports.in;

import com.acme.shop.domain.customer.Customer;
import com.acme.shop.domain.customer.CustomerId;
import com.acme.shop.domain.customer.Email;
import com.acme.shop.domain.order.Address;
import java.util.List;

public interface CustomerUseCases {
    Customer createCustomer(String firstName, String lastName, Email email, String phone, Address address);
    Customer updateCustomer(CustomerId id, String firstName, String lastName, String phone, Address address);
    Customer getCustomer(CustomerId id);
    List<Customer> getAllCustomers();
}
