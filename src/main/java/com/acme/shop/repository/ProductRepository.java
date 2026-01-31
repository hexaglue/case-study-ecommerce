package com.acme.shop.repository;

import com.acme.shop.model.Category;
import com.acme.shop.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategory(Category category);

    List<Product> findByActiveTrue();

    List<Product> findByNameContainingIgnoreCase(String name);
}
