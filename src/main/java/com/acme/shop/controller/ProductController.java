package com.acme.shop.controller;

import com.acme.shop.model.Category;
import com.acme.shop.model.Product;
import com.acme.shop.service.CatalogService;
import com.acme.shop.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anti-pattern: business logic in controller (price validation, product creation inline).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CatalogService catalogService;

    public ProductController(ProductService productService, CatalogService catalogService) {
        this.productService = productService;
        this.catalogService = catalogService;
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "EUR") String currency,
            @RequestParam String sku,
            @RequestParam Category category) {
        // Anti-pattern: validation in controller
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Product product = productService.createProduct(name, description, price, currency, sku, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<Product> updatePrice(@PathVariable Long id, @RequestParam BigDecimal price) {
        return ResponseEntity.ok(productService.updatePrice(id, price));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Product> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deactivate(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getActiveProducts() {
        return ResponseEntity.ok(productService.getActiveProducts());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(catalogService.searchProducts(query));
    }

    @GetMapping("/by-category")
    public ResponseEntity<Map<Category, List<Product>>> getProductsByCategory() {
        return ResponseEntity.ok(catalogService.getProductsByCategory());
    }
}
