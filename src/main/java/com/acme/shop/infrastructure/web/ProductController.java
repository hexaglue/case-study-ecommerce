package com.acme.shop.infrastructure.web;

import com.acme.shop.domain.product.Category;
import com.acme.shop.domain.product.Product;
import com.acme.shop.ports.in.CatalogUseCases;
import com.acme.shop.ports.in.ProductUseCases;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductUseCases productUseCases;
    private final CatalogUseCases catalogUseCases;

    public ProductController(ProductUseCases productUseCases, CatalogUseCases catalogUseCases) {
        this.productUseCases = productUseCases;
        this.catalogUseCases = catalogUseCases;
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "EUR") String currency,
            @RequestParam String sku,
            @RequestParam Category category) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Product product = productUseCases.createProduct(name, description, price, currency, sku, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<Product> updatePrice(@PathVariable Long id, @RequestParam BigDecimal price) {
        return ResponseEntity.ok(productUseCases.updatePrice(id, price));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Product> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(productUseCases.deactivate(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productUseCases.getProduct(id));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getActiveProducts() {
        return ResponseEntity.ok(productUseCases.getActiveProducts());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(catalogUseCases.searchProducts(query));
    }

    @GetMapping("/by-category")
    public ResponseEntity<Map<Category, List<Product>>> getProductsByCategory() {
        return ResponseEntity.ok(catalogUseCases.getProductsByCategory());
    }
}
