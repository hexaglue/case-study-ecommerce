package com.acme.shop.infrastructure.config;

import com.acme.shop.application.CatalogApplicationService;
import com.acme.shop.application.CustomerApplicationService;
import com.acme.shop.application.InventoryApplicationService;
import com.acme.shop.application.OrderApplicationService;
import com.acme.shop.application.PaymentApplicationService;
import com.acme.shop.application.ProductApplicationService;
import com.acme.shop.application.ShippingApplicationService;
import com.acme.shop.ports.in.CatalogUseCases;
import com.acme.shop.ports.in.CustomerUseCases;
import com.acme.shop.ports.in.InventoryUseCases;
import com.acme.shop.ports.in.OrderUseCases;
import com.acme.shop.ports.in.PaymentUseCases;
import com.acme.shop.ports.in.ProductUseCases;
import com.acme.shop.ports.in.ShippingUseCases;
import com.acme.shop.ports.out.CustomerRepository;
import com.acme.shop.ports.out.InventoryRepository;
import com.acme.shop.ports.out.NotificationSender;
import com.acme.shop.ports.out.OrderRepository;
import com.acme.shop.ports.out.PaymentGateway;
import com.acme.shop.ports.out.PaymentRepository;
import com.acme.shop.ports.out.ProductRepository;
import com.acme.shop.ports.out.ShipmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationServiceConfig {

    @Bean
    public CatalogUseCases catalogUseCases(ProductRepository productRepository) {
        return new CatalogApplicationService(productRepository);
    }

    @Bean
    public CustomerUseCases customerUseCases(CustomerRepository customerRepository) {
        return new CustomerApplicationService(customerRepository);
    }

    @Bean
    public InventoryUseCases inventoryUseCases(
            InventoryRepository inventoryRepository, ProductRepository productRepository) {
        return new InventoryApplicationService(inventoryRepository, productRepository);
    }

    @Bean
    public OrderUseCases orderUseCases(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            NotificationSender notificationSender) {
        return new OrderApplicationService(
                orderRepository, customerRepository, productRepository, inventoryRepository, notificationSender);
    }

    @Bean
    public PaymentUseCases paymentUseCases(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PaymentGateway paymentGateway) {
        return new PaymentApplicationService(paymentRepository, orderRepository, paymentGateway);
    }

    @Bean
    public ProductUseCases productUseCases(ProductRepository productRepository) {
        return new ProductApplicationService(productRepository);
    }

    @Bean
    public ShippingUseCases shippingUseCases(
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            InventoryRepository inventoryRepository) {
        return new ShippingApplicationService(shipmentRepository, orderRepository, inventoryRepository);
    }
}
