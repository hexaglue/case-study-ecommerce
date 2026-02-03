package com.acme.shop.infrastructure.web;

import com.acme.shop.domain.product.ProductId;
import com.acme.shop.ports.in.InventoryUseCases;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryUseCases inventoryUseCases;

    public InventoryController(InventoryUseCases inventoryUseCases) {
        this.inventoryUseCases = inventoryUseCases;
    }

    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeStock(
            @RequestParam UUID productId, @RequestParam int quantity) {
        inventoryUseCases.initializeStock(new ProductId(productId), quantity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{productId}/available")
    public ResponseEntity<Integer> getAvailable(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryUseCases.getAvailableQuantity(new ProductId(productId)));
    }
}
