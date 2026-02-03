package com.acme.shop.infrastructure.persistence;

import com.acme.shop.domain.customer.Customer;
import com.acme.shop.ports.out.CustomerRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCustomerRepository extends JpaRepository<Customer, Long>, CustomerRepository {
}
