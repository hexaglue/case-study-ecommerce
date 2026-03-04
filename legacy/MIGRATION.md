# Migration vers HexaGlue - Etude de cas E-commerce

Ce document retrace la migration progressive d'une application e-commerce
Spring Boot "enterprise standard" vers une architecture hexagonale avec HexaGlue.

Chaque etape correspond a une branche Git et documente les observations,
les problemes rencontres et les resultats obtenus.

---

## Etape 0 : Application Legacy (`step/0-legacy`)

### Description

Application e-commerce classique Spring Boot avec les anti-patterns
typiques d'une application enterprise :

- **50 classes Java** reparties en packages techniques (controller, service, model, repository, dto, config, exception, util, event)
- **Spring Boot 3.2.5** avec Spring Data JPA et H2

### Anti-patterns presents

| # | Anti-pattern | Exemple |
|---|-------------|---------|
| 1 | `@Entity` sur classes domaine | `Order`, `Customer`, `Product` heritent de `BaseEntity` avec `@MappedSuperclass` |
| 2 | `@Service` partout | Services applicatifs ET "domaine" marques `@Service` |
| 3 | Pas de ports | Services dependent directement des repositories Spring Data |
| 4 | Modele anemique | Toute la logique dans les services, entites = data holders avec setters |
| 5 | Primitives au lieu de value objects | `BigDecimal` pour le montant, `String` pour l'email |
| 6 | References directes entre agregats | `Order.customer` = `Customer` (entity), pas `CustomerId` |
| 7 | `BaseEntity` technique | `@MappedSuperclass` avec `Long id`, `createdAt`, `updatedAt` |
| 8 | Events Spring | `OrderCreatedEvent extends ApplicationEvent` |
| 9 | Logique metier dans controllers | Validation de prix dans `ProductController` |
| 10 | Infrastructure = domaine | Aucune separation, tout est un bean Spring + entity JPA |

### Structure des packages

```
com.acme.shop/
├── ShopApplication.java
├── controller/         (5 classes)  OrderController, CustomerController, ProductController, PaymentController, ShippingController
├── service/            (8 classes)  OrderService, CustomerService, ProductService, InventoryService, PaymentService, ShippingService, NotificationService, PaymentGatewayClient, CatalogService
├── model/             (11 classes)  BaseEntity, Order, OrderLine, Customer, Product, Payment, Inventory, StockMovement, Shipment, ShippingRate + 3 enums
├── repository/         (6 interfaces) Spring Data JPA repositories
├── dto/                (7 records)  Request/Response records
├── config/             (2 classes)  AppConfig, SecurityConfig
├── exception/          (4 classes)  OrderNotFoundException, InsufficientStockException, PaymentFailedException, GlobalExceptionHandler
├── event/              (1 classe)   OrderCreatedEvent (Spring ApplicationEvent)
└── util/               (2 classes)  DateUtils, MoneyUtils
```

### Verification

```bash
mvn clean compile   # BUILD SUCCESS - 50 source files
```

### Observations

L'application compile et fonctionne, mais le domaine metier est
completement enfoui sous l'infrastructure Spring/JPA. Il est impossible
de distinguer ce qui est du domaine pur de ce qui est de l'infrastructure.

C'est exactement le type d'application que HexaGlue est concu pour analyser
et aider a migrer.

---

## Etape 1 : Decouverte avec HexaGlue (`step/1-discovery`)

*A venir*

---

## Etape 2 : Configuration et exclusions (`step/2-configured`)

*A venir*

---

## Etape 3 : Restructuration hexagonale (`step/3-hexagonal`)

*A venir*

---

## Etape 4 : Purification du domaine (`step/4-pure-domain`)

*A venir*

---

## Etape 5 : Generation et audit (`step/5-generated`)

*A venir*

---

## Etape 6 : Application fonctionnelle (`main`)

*A venir*
