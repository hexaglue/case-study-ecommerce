package com.acme.shop.infrastructure.web;

import com.acme.shop.domain.order.Money;
import com.acme.shop.domain.product.Category;
import com.acme.shop.domain.product.Product;
import com.acme.shop.domain.product.ProductId;
import com.acme.shop.infrastructure.web.dto.ProductResponse;
import com.acme.shop.ports.in.CatalogUseCases;
import com.acme.shop.ports.in.ProductUseCases;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    public ResponseEntity<ProductResponse> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "EUR") String currency,
            @RequestParam String sku,
            @RequestParam Category category) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Product product = productUseCases.createProduct(name, description, Money.of(price, currency), sku, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(product));
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<ProductResponse> updatePrice(
            @PathVariable UUID id, @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "EUR") String currency) {
        return ResponseEntity.ok(toResponse(productUseCases.updatePrice(new ProductId(id), Money.of(price, currency))));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ProductResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(productUseCases.deactivate(new ProductId(id))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(productUseCases.getProduct(new ProductId(id))));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {
        return ResponseEntity.ok(productUseCases.getActiveProducts().stream().map(this::toResponse).toList());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(catalogUseCases.searchProducts(query).stream().map(this::toResponse).toList());
    }

    @GetMapping("/by-category")
    public ResponseEntity<Map<String, List<ProductResponse>>> getProductsByCategory() {
        return ResponseEntity.ok(
                catalogUseCases.getProductsByCategory().entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey().name(),
                                e -> e.getValue().stream().map(this::toResponse).toList())));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId() != null ? product.getId().value() : null,
                product.getName(),
                product.getDescription(),
                product.getPrice().amount(),
                product.getPrice().currency(),
                product.getSku(),
                product.getCategory().name(),
                product.isActive());
    }
}
