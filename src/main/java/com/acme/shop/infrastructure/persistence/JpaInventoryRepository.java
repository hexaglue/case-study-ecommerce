package com.acme.shop.infrastructure.persistence;

import com.acme.shop.domain.inventory.Inventory;
import com.acme.shop.ports.out.InventoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInventoryRepository extends JpaRepository<Inventory, Long>, InventoryRepository {
}
