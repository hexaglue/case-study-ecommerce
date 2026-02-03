package com.acme.shop.application;

import com.acme.shop.domain.product.Category;
import com.acme.shop.domain.product.Product;
import com.acme.shop.ports.in.CatalogUseCases;
import com.acme.shop.ports.out.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogApplicationService implements CatalogUseCases {

    private final ProductRepository productRepository;

    public CatalogApplicationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Map<Category, List<Product>> getProductsByCategory() {
        return productRepository.findByActiveTrue().stream()
                .collect(Collectors.groupingBy(Product::getCategory));
    }

    @Override
    public List<Product> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return productRepository.findByActiveTrue();
        }
        return productRepository.findByNameContainingIgnoreCase(query);
    }
}
