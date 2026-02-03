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

### Description

Ajout du plugin Maven HexaGlue (sans extensions, sans plugins de generation).
Premier lancement de `hexaglue:validate` sur le code legacy brut, sans aucun `hexaglue.yaml`.

### Modifications

- `pom.xml` : ajout du plugin `hexaglue-maven-plugin:5.0.0-SNAPSHOT` avec `basePackage=com.acme.shop`
- Pas de `hexaglue.yaml` : on observe le comportement brut

### Resultats

```
mvn clean compile       → BUILD SUCCESS
mvn hexaglue:validate   → BUILD SUCCESS
```

**Classification brute** (50 types parses, 24 retenus, 26 filtres automatiquement) :

| Categorie | Nombre | % |
|-----------|--------|---|
| EXPLICIT | 9 | 37,5% |
| INFERRED | 15 | 62,5% |
| UNCLASSIFIED | 0 | 0,0% |
| **Total** | **24** | 100% |

#### 9 types EXPLICIT (tous ENTITY via @Entity JPA)

| Type | Kind |
|------|------|
| `Customer` | ENTITY |
| `Inventory` | ENTITY |
| `Order` | ENTITY |
| `OrderLine` | ENTITY |
| `Payment` | ENTITY |
| `Product` | ENTITY |
| `Shipment` | ENTITY |
| `ShippingRate` | ENTITY |
| `StockMovement` | ENTITY |

#### 15 types INFERRED

| Type | Kind | Observation |
|------|------|-------------|
| `Category` | VALUE_OBJECT | Enum - correct |
| `OrderStatus` | VALUE_OBJECT | Enum - correct |
| `PaymentStatus` | VALUE_OBJECT | Enum - correct |
| `CreateCustomerRequest` | VALUE_OBJECT | DTO record - faux positif |
| `CreateOrderRequest` | VALUE_OBJECT | DTO record - faux positif |
| `PaymentRequest` | VALUE_OBJECT | DTO record - faux positif |
| `OrderCreatedEvent` | DOMAIN_EVENT | Spring ApplicationEvent - faux positif |
| `CatalogService` | VALUE_OBJECT | Service Spring - faux positif |
| `CustomerService` | VALUE_OBJECT | Service Spring - faux positif |
| `InventoryService` | VALUE_OBJECT | Service Spring - faux positif |
| `OrderService` | VALUE_OBJECT | Service Spring - faux positif |
| `PaymentGatewayClient` | VALUE_OBJECT | Service Spring - faux positif |
| `PaymentService` | VALUE_OBJECT | Service Spring - faux positif |
| `ProductService` | VALUE_OBJECT | Service Spring - faux positif |
| `ShippingService` | VALUE_OBJECT | Service Spring - faux positif |

#### 26 types filtres automatiquement

Controllers (5), Spring Data repositories (6), configs (2), exceptions (4),
utilitaires (2), ShopApplication, BaseEntity, NotificationService, 4 DTOs response/search.

### Observations

1. **HexaGlue detecte les @Entity JPA** et les classifie ENTITY (EXPLICIT) -- attendu mais trop plat (pas de distinction aggregate/entity/value object)
2. **Les 8 services Spring sont mal classifies VALUE_OBJECT** : HexaGlue les voit comme des types immutables injectes dans d'autres types. C'est le principal faux positif.
3. **3 DTOs records leakent** dans le rapport comme VALUE_OBJECT : ils devraient etre exclus.
4. **L'event Spring est detecte** comme DOMAIN_EVENT par convention de nommage (*Event) -- faux positif car c'est un ApplicationEvent Spring, pas un domain event DDD.
5. **Les 3 enums sont correctement VALUE_OBJECT** : Category, OrderStatus, PaymentStatus.
6. **Les controlleurs, repos, configs sont bien filtres** automatiquement.
7. **Conclusion** : le rapport est bruyant. Il faut exclure les services et DTOs pour avoir une vue plus claire du domaine.

---

## Etape 2 : Configuration et exclusions (`step/2-configured`)

### Description

Creation de `hexaglue.yaml` avec des exclusions minimales pour nettoyer les faux positifs
identifies a l'etape 1. **Aucune classification explicite** -- on laisse HexaGlue inferer.

### Modifications

- `hexaglue.yaml` (nouveau) : exclusion des services (`com.acme.shop.service.*`) et de l'event Spring (`com.acme.shop.event.*`)

### Resultats

```
mvn hexaglue:validate   → BUILD SUCCESS
```

**Rapport apres exclusions** (15 types, contre 24 a l'etape 1) :

| Categorie | Nombre | % | Delta vs step 1 |
|-----------|--------|---|------------------|
| EXPLICIT | 9 | 60,0% | = |
| INFERRED | 6 | 40,0% | -9 |
| UNCLASSIFIED | 0 | 0,0% | = |
| **Total** | **15** | 100% | -9 |

#### Types retenus

| Type | Kind | Certainty | Observation |
|------|------|-----------|-------------|
| `Customer` | ENTITY | EXPLICIT | @Entity JPA |
| `Inventory` | ENTITY | EXPLICIT | @Entity JPA |
| `Order` | ENTITY | EXPLICIT | @Entity JPA |
| `OrderLine` | ENTITY | EXPLICIT | @Entity JPA |
| `Payment` | ENTITY | EXPLICIT | @Entity JPA |
| `Product` | ENTITY | EXPLICIT | @Entity JPA |
| `Shipment` | ENTITY | EXPLICIT | @Entity JPA |
| `ShippingRate` | ENTITY | EXPLICIT | @Entity JPA |
| `StockMovement` | ENTITY | EXPLICIT | @Entity JPA |
| `Category` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Enum - correct |
| `OrderStatus` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Enum - correct |
| `PaymentStatus` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Enum - correct |
| `CreateCustomerRequest` | VALUE_OBJECT | INFERRED | DTO record - faux positif |
| `CreateOrderRequest` | VALUE_OBJECT | INFERRED | DTO record - faux positif |
| `PaymentRequest` | VALUE_OBJECT | INFERRED | DTO record - faux positif |

### Observations

1. **Les 8 services et 1 event sont exclus** -- le rapport est plus propre
2. **3 DTOs records persistent** : les records du package `dto` sont vus comme VALUE_OBJECT par HexaGlue. Ce sont des records sans identite, donc structurellement des value objects. Pas genant pour l'instant.
3. **La classification reste plate** : tous les types domaine sont ENTITY (a cause de @Entity JPA). HexaGlue ne peut pas distinguer les agregats des entites enfants.
4. **Conclusion** : les exclusions nettoient le bruit mais le probleme structurel reste -- il faut reorganiser en architecture hexagonale (etape 3) et purifier le domaine (etape 4) pour que l'inference DDD fonctionne.

---

## Etape 3 : Restructuration hexagonale (`step/3-hexagonal`)

### Description

Reorganisation complete des packages en architecture hexagonale.
Creation des ports (driving/driven), services applicatifs, adapters infrastructure.
Les annotations JPA restent sur les classes domaine. hexaglue.yaml : seulement des exclusions.

### Modifications

- **15 port interfaces creees** : 7 driving (ports/in/) + 8 driven (ports/out/)
- **7 application services** : implementent les driving ports, dependent des driven ports
- **6 Spring Data repos dual-interface** : JpaXxxRepository extends JpaRepository + XxxRepository port
- **2 adapters infrastructure** : PaymentGatewayAdapter, NotificationAdapter
- **13 classes domaine** deplacees vers domain/ (par sous-domaine)
- **5 controllers** deplaces vers infrastructure/web/
- **7 DTOs** deplaces vers infrastructure/web/dto/
- **Configs, utils, event** deplaces vers infrastructure/
- `hexaglue.yaml` mis a jour : exclusion infrastructure + application

### Structure des packages

```
com.acme.shop/
├── ShopApplication.java
├── domain/
│   ├── shared/            (BaseEntity)
│   ├── order/             (Order, OrderLine, OrderStatus)
│   ├── customer/          (Customer)
│   ├── product/           (Product, Category)
│   ├── inventory/         (Inventory, StockMovement)
│   ├── payment/           (Payment, PaymentStatus)
│   └── shipping/          (Shipment, ShippingRate)
├── ports/
│   ├── in/                (OrderUseCases, CustomerUseCases, ProductUseCases, CatalogUseCases, InventoryUseCases, PaymentUseCases, ShippingUseCases)
│   └── out/               (OrderRepository, CustomerRepository, ProductRepository, InventoryRepository, PaymentRepository, ShipmentRepository, PaymentGateway, NotificationSender)
├── application/           (7 *ApplicationService)
├── exception/             (3 exceptions + GlobalExceptionHandler)
└── infrastructure/
    ├── web/               (5 controllers + dto/)
    ├── persistence/       (6 Jpa*Repository dual-interface)
    ├── external/          (PaymentGatewayAdapter, NotificationAdapter)
    ├── event/             (OrderCreatedEvent)
    ├── config/            (AppConfig, SecurityConfig)
    └── util/              (DateUtils, MoneyUtils)
```

### Resultats

```
mvn clean compile       → BUILD SUCCESS (65 source files)
mvn hexaglue:validate   → BUILD SUCCESS
```

| Categorie | Nombre | % | Delta vs step 2 |
|-----------|--------|---|------------------|
| EXPLICIT | 9 | 75,0% | = |
| INFERRED | 3 | 25,0% | -3 |
| UNCLASSIFIED | 0 | 0,0% | = |
| **Total** | **12** | 100% | -3 |
| **Ports** | **15** | - | +15 (nouveau) |
| **Conflicts** | **0** | - | - |

#### Ports detectes (15)

- 7 driving ports (ports/in/) : OrderUseCases, CustomerUseCases, ProductUseCases, CatalogUseCases, InventoryUseCases, PaymentUseCases, ShippingUseCases
- 8 driven ports (ports/out/) : OrderRepository, CustomerRepository, ProductRepository, InventoryRepository, PaymentRepository, ShipmentRepository, PaymentGateway, NotificationSender

### Observations

1. **Les DTOs sont maintenant exclus** (dans infrastructure) : le rapport passe de 15 a 12 types
2. **15 ports detectes** correctement : HexaGlue identifie les interfaces dans ports/ comme DRIVING_PORT et DRIVEN_PORT
3. **0 conflit** : HexaGlue distingue correctement driving/driven grace a la structure des packages (in/ vs out/)
4. **La classification domaine reste plate** : tous les types domaine sont ENTITY (a cause de @Entity JPA). La distinction aggregate/entity/value object n'est pas encore possible.
5. **L'architecture hexagonale est en place** : ports clairement definis, services dependent des ports, controllers dependent des driving ports
6. **Conclusion** : la structure hexagonale apporte les ports, mais le domaine doit etre purifie (suppression @Entity) pour que HexaGlue infere les roles DDD (etape 4)

---

## Etape 4 : Purification du domaine (`step/4-pure-domain`)

### Description

Domaine pur sans annotations JPA. Value objects (records). Identifiants types (records wrappant Long).
Logique metier dans les agregats. References inter-agregats par ID uniquement.
Pas d'infrastructure manuelle -- l'application compile mais ne peut pas demarrer (pas de persistence).

### Modifications

- **6 identifiants types** (records) : `OrderId`, `CustomerId`, `ProductId`, `PaymentId`, `ShipmentId`, `InventoryId` -- wrappent `Long`
- **4 value objects** (records) : `Money(BigDecimal, String)`, `Address(String, String, String, String)`, `Email(String)`, `Quantity(int)`
- **1 domain event** (record) : `OrderPlacedEvent(OrderId, CustomerId, Money, Instant)`
- **9 classes domaine purifiees** : suppression de toutes les annotations JPA, suppression de `extends BaseEntity`, remplacement des setters par des methodes metier
- **`ShippingRate` transforme en record** (value object)
- **`BaseEntity` supprimee** (plus necessaire)
- **6 JPA repositories supprimes** (plus d'@Entity = plus de Spring Data direct)
- **`OrderCreatedEvent` Spring supprime** (remplace par `OrderPlacedEvent` domaine)
- **15 port interfaces mises a jour** avec identifiants types
- **7 application services refactores** : utilisent les factory methods et methodes metier
- **5 controllers adaptes** : mapping DTO <-> types domaine
- **2 adapters infrastructure mis a jour** : `PaymentGatewayAdapter`, `NotificationAdapter`
- **`hexaglue.yaml`** : ajout exclusion `com.acme.shop.exception.**`

### Structure des packages (domaine purifie)

```
com.acme.shop/
├── domain/
│   ├── order/        (Order, OrderLine, OrderId, OrderStatus, Money, Address, Quantity, OrderPlacedEvent)
│   ├── customer/     (Customer, CustomerId, Email)
│   ├── product/      (Product, ProductId, Category)
│   ├── inventory/    (Inventory, InventoryId, StockMovement)
│   ├── payment/      (Payment, PaymentId, PaymentStatus)
│   └── shipping/     (Shipment, ShipmentId, ShippingRate)
├── ports/
│   ├── in/           (7 driving ports avec types domaine)
│   └── out/          (8 driven ports avec identifiants types)
├── application/      (7 services -- exclus de HexaGlue)
├── exception/        (4 classes -- exclus de HexaGlue)
└── infrastructure/   (web, external -- exclus de HexaGlue)
```

### Resultats

```
mvn clean compile       → BUILD SUCCESS (68 source files)
mvn hexaglue:validate   → BUILD SUCCESS
```

**Classification** (22 types domaine + 15 ports) :

| Categorie | Nombre | % | Delta vs step 3 |
|-----------|--------|---|------------------|
| EXPLICIT | 0 | 0,0% | -9 |
| INFERRED | 22 | 100,0% | +19 |
| UNCLASSIFIED | 0 | 0,0% | = |
| **Total** | **22** | 100% | +10 |
| **Ports** | **15** | - | = |
| **Conflicts** | **0** | - | = |

#### Classification par role DDD (22 types, 100% INFERRED)

| Type | Kind | Certainty | Reasoning |
|------|------|-----------|-----------|
| `Customer` | AGGREGATE_ROOT | CERTAIN_BY_STRUCTURE | Dominant type in repository [CustomerRepository], has identity field 'id' |
| `Inventory` | AGGREGATE_ROOT | CERTAIN_BY_STRUCTURE | Dominant type in repository [InventoryRepository], has identity field 'id' |
| `Order` | AGGREGATE_ROOT | CERTAIN_BY_STRUCTURE | Dominant type in repository [OrderRepository], has identity field 'id' |
| `Payment` | AGGREGATE_ROOT | CERTAIN_BY_STRUCTURE | Dominant type in repository [PaymentRepository], has identity field 'id' |
| `Product` | AGGREGATE_ROOT | CERTAIN_BY_STRUCTURE | Dominant type in repository [ProductRepository], has identity field 'id' |
| `Shipment` | AGGREGATE_ROOT | CERTAIN_BY_STRUCTURE | Dominant type in repository [ShipmentRepository], has identity field 'id' |
| `CustomerId` | IDENTIFIER | CERTAIN_BY_STRUCTURE | Record with single component wrapping Long |
| `InventoryId` | IDENTIFIER | CERTAIN_BY_STRUCTURE | Record with single component wrapping Long |
| `OrderId` | IDENTIFIER | CERTAIN_BY_STRUCTURE | Record with single component wrapping Long |
| `PaymentId` | IDENTIFIER | CERTAIN_BY_STRUCTURE | Record with single component wrapping Long |
| `ProductId` | IDENTIFIER | CERTAIN_BY_STRUCTURE | Record with single component wrapping Long |
| `ShipmentId` | IDENTIFIER | CERTAIN_BY_STRUCTURE | Record with single component wrapping Long |
| `OrderLine` | ENTITY | CERTAIN_BY_STRUCTURE | Class with identity contained in aggregate Order |
| `StockMovement` | ENTITY | CERTAIN_BY_STRUCTURE | Class with identity contained in aggregate Inventory |
| `Address` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Immutable type embedded in aggregates |
| `Email` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Immutable type embedded in aggregates |
| `Money` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Immutable type embedded in aggregates |
| `Quantity` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Immutable type embedded in aggregates |
| `Category` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Enum type |
| `OrderStatus` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Enum type |
| `PaymentStatus` | VALUE_OBJECT | CERTAIN_BY_STRUCTURE | Enum type |
| `OrderPlacedEvent` | DOMAIN_EVENT | INFERRED | Naming convention (*Event) |

### Observations

1. **0% EXPLICIT, 100% INFERRED** : le domaine pur permet a HexaGlue d'inferer correctement tous les roles DDD sans aucune classification explicite
2. **6 AGGREGATE_ROOT detectes** via la correlation types-repositories : chaque type dominant d'un repository est un aggregate root
3. **6 IDENTIFIER detectes** via la structure des records wrappant Long
4. **2 ENTITY detectees** (OrderLine, StockMovement) : classes avec identite contenues dans un agregat
5. **7 VALUE_OBJECT detectes** : 4 records immutables (Address, Email, Money, Quantity) + 3 enums
6. **1 DOMAIN_EVENT detecte** par convention de nommage (*Event) -- cette fois c'est un vrai domain event record, pas un Spring ApplicationEvent
7. **La puissance de l'inference** : aucun `hexaglue.yaml` explicite n'est necessaire. Les exclusions suffisent pour nettoyer le bruit.
8. **L'app compile mais ne demarre pas** : il n'y a plus de persistence (les JPA repos ont ete supprimes, les classes domaine n'ont plus @Entity). C'est volontaire -- l'etape 5 generera toute l'infrastructure.
9. **Conclusion** : le domaine est pret pour la generation. HexaGlue a toutes les informations necessaires pour generer les JPA entities, mappers, et adapters.

---

## Etape 5 : Generation et audit (`step/5-generated`)

*A venir*

---

## Etape 6 : Application fonctionnelle (`main`)

*A venir*
