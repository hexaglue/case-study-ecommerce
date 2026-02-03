package com.acme.shop.infrastructure.persistence;

import com.acme.shop.domain.product.Product;
import com.acme.shop.ports.out.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProductRepository extends JpaRepository<Product, Long>, ProductRepository {
}
