package com.acme.shop.infrastructure.persistence;

import com.acme.shop.domain.shipping.Shipment;
import com.acme.shop.ports.out.ShipmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaShipmentRepository extends JpaRepository<Shipment, Long>, ShipmentRepository {
}
