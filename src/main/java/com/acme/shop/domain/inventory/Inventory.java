package com.acme.shop.domain.inventory;

import com.acme.shop.domain.product.Product;
import com.acme.shop.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory")
public class Inventory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private int quantityOnHand;

    @Column(nullable = false)
    private int reservedQuantity;

    private int reorderThreshold;

    @OneToMany(mappedBy = "inventory")
    private List<StockMovement> movements = new ArrayList<>();

    public Inventory() {}

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(int quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int reorderThreshold) { this.reorderThreshold = reorderThreshold; }

    public List<StockMovement> getMovements() { return movements; }
    public void setMovements(List<StockMovement> movements) { this.movements = movements; }

    public int getAvailableQuantity() { return quantityOnHand - reservedQuantity; }
}
