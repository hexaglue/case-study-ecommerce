package com.acme.shop.ports.in;

import com.acme.shop.infrastructure.web.dto.CreateCustomerRequest;
import com.acme.shop.infrastructure.web.dto.CustomerResponse;
import java.util.List;

public interface CustomerUseCases {
    CustomerResponse createCustomer(CreateCustomerRequest request);
    CustomerResponse updateCustomer(Long id, CreateCustomerRequest request);
    CustomerResponse getCustomer(Long id);
    List<CustomerResponse> getAllCustomers();
}
