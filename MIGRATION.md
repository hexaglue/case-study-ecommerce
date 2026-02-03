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

Ajout du plugin Maven HexaGlue avec le plugin d'audit (sans extensions, sans plugins de
generation). Premier lancement de `hexaglue:audit` sur le code legacy brut, sans aucun
`hexaglue.yaml`, pour obtenir une baseline mesurable de l'etat architectural.

### Modifications

- `pom.xml` : ajout du plugin `hexaglue-maven-plugin:5.0.0-SNAPSHOT` avec `basePackage=com.acme.shop`,
  `failOnError=false`, et `hexaglue-plugin-audit:2.0.0-SNAPSHOT` comme dependance plugin
- Pas de `hexaglue.yaml` : on observe le comportement brut

### Resultats

```
mvn clean compile     → BUILD SUCCESS
mvn hexaglue:audit    → BUILD SUCCESS (audit FAILED attendu : 19 violations critiques)
```

#### Verdict

| Score | Grade | Status |
|:-----:|:-----:|:------:|
| **23/100** | **F** | **FAILED** |

#### KPIs (baseline legacy)

| Dimension | Score | Seuil | Status |
|-----------|------:|------:|:------:|
| DDD Compliance | 0% | 90% | CRITICAL |
| Hexagonal Architecture | 40% | 90% | CRITICAL |
| Dependencies | 0% | 80% | CRITICAL |
| Coupling | 30% | 70% | CRITICAL |
| Cohesion | 58% | 80% | CRITICAL |

#### Inventaire architectural

| Composant | Nombre | Observation |
|-----------|-------:|-------------|
| Aggregate Roots | 0 | Aucun detecte (pas de distinction aggregate/entity) |
| Entities | 9 | Tous les @Entity JPA |
| Value Objects | 14 | 3 enums (correct) + 8 services + 3 DTOs (faux positifs) |
| Identifiers | 0 | Pas d'identifiants types |
| Domain Events | 1 | OrderCreatedEvent (Spring ApplicationEvent - faux positif) |
| Driving Ports | 0 | Aucun port driving |
| Driven Ports | 6 | Les 6 repositories Spring Data |

#### 31 violations (19 CRITICAL, 12 MAJOR)

| Contrainte | Nombre | Severite | Types concernes |
|------------|-------:|----------|-----------------|
| `ddd:entity-identity` | 9 | CRITICAL | 9 entites sans champ identite type (heritent de BaseEntity) |
| `ddd:domain-purity` | 10 | CRITICAL | 9 modeles + OrderService avec imports JPA interdits |
| `hexagonal:layer-isolation` | 12 | MAJOR | 7 services dependent directement des repositories |

#### Metriques cles

| Metrique | Valeur | Seuil | Status |
|----------|-------:|------:|:------:|
| Domain coverage | 80,00% | min 30% | OK |
| Code boilerplate ratio | 81,58% | max 50% | CRITICAL |
| Domain purity | 58,33% | min 100% | CRITICAL |
| Aggregate boundary | 0,00% | min 80% | CRITICAL |
| Code complexity | 1,11 | max 10 | OK |

#### Rapports generes

- `target/hexaglue/reports/audit/audit-report.json` : donnees structurees
- `target/hexaglue/reports/audit/audit-report.html` : tableau de bord interactif
- `target/hexaglue/reports/audit/AUDIT-REPORT.md` : rapport markdown avec diagrammes Mermaid

### Observations

1. **Score 23/100 (Grade F)** : baseline quantifiee de l'etat legacy. Ce score servira de reference pour mesurer la progression a chaque etape.
2. **0% DDD Compliance** : aucune structure DDD detectee (pas d'aggregats, pas d'identifiants types, pas de value objects intentionnels).
3. **9 entites sans identite** (`ddd:entity-identity`) : les entites heritent de `BaseEntity` qui porte un `Long id` via `@MappedSuperclass`, mais HexaGlue ne reconnait pas cela comme un champ identite type.
4. **10 violations de purete domaine** (`ddd:domain-purity`) : toutes les classes `model/` importent des annotations JPA (`jakarta.persistence.*`). L'infrastructure est melee au domaine.
5. **12 violations d'isolation** (`hexagonal:layer-isolation`) : les services (classifies DOMAIN) dependent directement des repositories (classifies PORT). Dans une architecture hexagonale, le domaine ne devrait pas connaitre les ports.
6. **81,58% de boilerplate** : getters, setters, constructeurs vides -- modele anemique typique.
7. **40% Hexagonal Architecture** : HexaGlue detecte bien les 6 repositories comme driven ports, mais l'absence de driving ports et les violations de couches plombent le score.
8. **Classification bruyante** : 8 services Spring classes VALUE_OBJECT et 3 DTOs records sont des faux positifs a nettoyer a l'etape 2.
9. **Conclusion** : l'audit fournit une baseline chiffree precise. Les 3 familles de violations identifient clairement les axes de travail : (1) creer des identifiants types, (2) purifier le domaine des imports JPA, (3) isoler les couches via des ports.

---

## Etape 2 : Configuration et exclusions (`step/2-configured`)

### Description

Creation de `hexaglue.yaml` avec des exclusions minimales pour nettoyer les faux positifs
identifies a l'etape 1. **Aucune classification explicite** -- on laisse HexaGlue inferer.

### Modifications

- `hexaglue.yaml` (nouveau) : exclusion des services (`com.acme.shop.service.*`) et de l'event Spring (`com.acme.shop.event.*`)
- `pom.xml` : ajout du plugin d'audit et `failOnError=false` (meme configuration que step 1)

### Resultats

```
mvn clean compile     → BUILD SUCCESS
mvn hexaglue:audit    → BUILD SUCCESS (audit FAILED attendu : 18 violations critiques)
```

#### Verdict

| | Step 1 | Step 2 | Delta |
|---|:------:|:------:|:-----:|
| **Score** | 23/100 | **29/100** | **+6** |
| **Grade** | F | **F** | = |
| **Violations** | 31 | **24** | **-7** |
| CRITICAL | 19 | 18 | -1 |
| MAJOR | 12 | 6 | -6 |

#### KPIs (progression)

| Dimension | Step 1 | Step 2 | Delta |
|-----------|-------:|-------:|:-----:|
| DDD Compliance | 0% | 0% | = |
| Hexagonal Architecture | 40% | **70%** | **+30** |
| Dependencies | 0% | 0% | = |
| Coupling | 30% | 21% | -9 |
| Cohesion | 58% | 58% | = |

#### Inventaire architectural

| Composant | Step 1 | Step 2 | Delta |
|-----------|-------:|-------:|:-----:|
| Aggregate Roots | 0 | 0 | = |
| Entities | 9 | 9 | = |
| Value Objects | 14 | **6** | **-8** |
| Identifiers | 0 | 0 | = |
| Domain Events | 1 | **0** | **-1** |
| Driving Ports | 0 | 0 | = |
| Driven Ports | 6 | 6 | = |

#### 24 violations (18 CRITICAL, 6 MAJOR)

| Contrainte | Step 1 | Step 2 | Delta | Observation |
|------------|-------:|-------:|:-----:|-------------|
| `ddd:entity-identity` | 9 | 9 | = | Memes 9 entites sans identite type |
| `ddd:domain-purity` | 10 | **9** | **-1** | OrderService exclu |
| `hexagonal:layer-isolation` | 12 | **0** | **-12** | Disparu : services exclus du perimetre |
| `hexagonal:port-coverage` | 0 | **6** | **+6** | Nouveau : 6 repositories sans adaptateur |

#### Metriques cles

| Metrique | Step 1 | Step 2 | Delta | Observation |
|----------|-------:|-------:|:-----:|-------------|
| Domain coverage | 80,00% | 71,43% | -8,57 | Moins de types dans le perimetre |
| Code boilerplate ratio | 81,58% | **100,00%** | **+18,42** | Sans services, le modele est 100% boilerplate |
| Domain purity | 58,33% | 40,00% | -18,33 | Les 9 types impurs pesent plus dans un perimetre reduit |
| Aggregate boundary | 0,00% | 0,00% | = | Toujours pas d'agregats detectes |
| Code complexity | 1,11 | 1,00 | -0,11 | OK |

### Observations

1. **Score +6 points** (23 → 29) : amelioration modeste. Les exclusions reduisent le bruit mais ne resolvent pas les problemes structurels.
2. **Hexagonal +30 points** (40% → 70%) : c'est le gain principal. Les 12 violations `layer-isolation` disparaissent car les services (mal classifies DOMAIN) ne sont plus dans le perimetre. En contrepartie, 6 violations `port-coverage` apparaissent : les repositories n'ont pas d'adaptateur.
3. **Swap de contraintes hexagonales** : on passe de "domaine depend des ports" (faux positif) a "ports sans adaptateur" (probleme reel). Le rapport est plus pertinent.
4. **100% boilerplate** : sans les services, il ne reste que les entites JPA -- des data holders purs avec getters/setters. Le modele anemique est expose de maniere flagrante.
5. **Domain purity en baisse** (58% → 40%) : paradoxe des exclusions. Avec moins de types dans le perimetre, les 9 classes impures (imports JPA) pesent proportionnellement plus. La metrique reflète correctement l'etat du code restant.
6. **3 DTOs records persistent** comme VALUE_OBJECT : structurellement corrects (records sans identite). Pas genant a cette etape.
7. **Classification toujours plate** : tous les types domaine restent ENTITY a cause de @Entity JPA. HexaGlue ne distingue pas agregats, entites enfants et value objects.
8. **Conclusion** : les exclusions nettoient le bruit de classification et ameliorent la pertinence du rapport hexagonal. Mais le probleme fondamental reste : le domaine est couple a JPA. Il faut reorganiser en architecture hexagonale (etape 3) et purifier le domaine (etape 4).

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
mvn clean compile     → BUILD SUCCESS (65 source files)
mvn hexaglue:audit    → BUILD SUCCESS (audit FAILED attendu : 18 violations critiques)
```

#### Verdict

| | Step 1 | Step 2 | Step 3 | Delta vs 2 |
|---|:------:|:------:|:------:|:----------:|
| **Score** | 23/100 | 29/100 | **40/100** | **+11** |
| **Grade** | F | F | **F** | = |
| **Violations** | 31 | 24 | **18** | **-6** |
| CRITICAL | 19 | 18 | 18 | = |
| MAJOR | 12 | 6 | **0** | **-6** |

#### KPIs (progression)

| Dimension | Step 1 | Step 2 | Step 3 | Delta vs 2 |
|-----------|-------:|-------:|-------:|:----------:|
| DDD Compliance | 0% | 0% | 0% | = |
| Hexagonal Architecture | 40% | 70% | **100%** | **+30** |
| Dependencies | 0% | 0% | 0% | = |
| Coupling | 30% | 21% | **37%** | **+16** |
| Cohesion | 58% | 58% | **64%** | **+6** |

#### Inventaire architectural

| Composant | Step 2 | Step 3 | Delta |
|-----------|-------:|-------:|:-----:|
| Aggregate Roots | 0 | 0 | = |
| Entities | 9 | 9 | = |
| Value Objects | 6 | **3** | **-3** |
| Identifiers | 0 | 0 | = |
| Domain Events | 0 | 0 | = |
| Driving Ports | 0 | **6** | **+6** |
| Driven Ports | 6 | **9** | **+3** |

#### Ports detectes (15)

**6 driving ports** (ports/in/) :

| Port | Methodes |
|------|:--------:|
| OrderUseCases | 6 |
| CustomerUseCases | 4 |
| ProductUseCases | 7 |
| CatalogUseCases | 2 |
| PaymentUseCases | 2 |
| ShippingUseCases | 3 |

**9 driven ports** (ports/out/) -- tous avec adaptateur :

| Port | Type | Methodes | Adaptateur |
|------|------|:--------:|:----------:|
| OrderRepository | REPOSITORY | 5 | oui |
| CustomerRepository | REPOSITORY | 5 | oui |
| ProductRepository | REPOSITORY | 6 | oui |
| InventoryRepository | REPOSITORY | 3 | oui |
| PaymentRepository | REPOSITORY | 5 | oui |
| ShipmentRepository | REPOSITORY | 4 | oui |
| PaymentGateway | GATEWAY | 3 | oui |
| NotificationSender | EVENT_PUBLISHER | 2 | oui |
| InventoryUseCases | REPOSITORY | 5 | oui |

#### 18 violations (18 CRITICAL, 0 MAJOR)

| Contrainte | Step 2 | Step 3 | Delta | Observation |
|------------|-------:|-------:|:-----:|-------------|
| `ddd:entity-identity` | 9 | 9 | = | Memes 9 entites sans identite type |
| `ddd:domain-purity` | 9 | 9 | = | 9 classes model/ avec imports JPA |
| `hexagonal:port-coverage` | 6 | **0** | **-6** | Tous les ports ont un adaptateur (dual-interface) |

#### Metriques cles

| Metrique | Step 2 | Step 3 | Delta | Observation |
|----------|-------:|-------:|:-----:|-------------|
| Domain coverage | 71,43% | 44,44% | -26,99 | Perimetre elargi avec les ports |
| Code boilerplate ratio | 100,00% | 100,00% | = | Modele toujours anemique |
| Domain purity | 40,00% | 25,00% | -15,00 | Ports purs diluent la proportion |
| Aggregate boundary | 0,00% | 0,00% | = | Toujours pas d'agregats |
| Code complexity | 1,00 | 1,00 | = | OK |

### Observations

1. **Score +11 points** (29 → 40) : la restructuration hexagonale apporte un gain significatif, principalement via l'axe Hexagonal.
2. **Hexagonal 100%** : c'est le fait marquant. Les 15 ports sont correctement detectes, tous les driven ports ont un adaptateur (via le dual-interface JpaXxxRepository). Les 6 violations `port-coverage` de l'etape 2 disparaissent.
3. **0 violations MAJOR** : il ne reste que les 18 CRITICAL -- toutes liees au domaine (identite + purete). L'architecture hexagonale est validee.
4. **6 driving ports detectes** : HexaGlue identifie les interfaces dans `ports/in/` (OrderUseCases, CustomerUseCases, etc.). Note : InventoryUseCases apparait dans les driven ports comme REPOSITORY -- une misclassification mineure.
5. **3 Value Objects** (vs 6) : les 3 DTOs records ont ete deplaces vers `infrastructure/web/dto/` et sont exclus. Il ne reste que les 3 enums (Category, OrderStatus, PaymentStatus).
6. **Domain purity en baisse** (40% → 25%) : paradoxe. L'ajout des 15 ports (purs) elargit le perimetre total, mais les 9 classes impures (model/) restent inchangees. La proportion de types impurs dans le perimetre elargi diminue le score.
7. **Coupling +16** (21% → 37%) : amelioration grace a l'inversion de dependances via les ports.
8. **Conclusion** : l'architecture hexagonale est completement en place et validee par l'audit. Les seuls problemes restants sont dans le domaine : (1) pas d'identifiants types et (2) imports JPA dans les classes domaine. La purification du domaine (etape 4) est le prochaine levier majeur.

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
mvn clean compile     → BUILD SUCCESS (68 source files)
mvn hexaglue:audit    → BUILD SUCCESS (audit PASSED with warnings)
```

#### Verdict

| | Step 1 | Step 2 | Step 3 | Step 4 | Delta vs 3 |
|---|:------:|:------:|:------:|:------:|:----------:|
| **Score** | 23/100 | 29/100 | 40/100 | **56/100** | **+16** |
| **Grade** | F | F | F | **F** | = |
| **Status** | FAILED | FAILED | FAILED | **PASSED** | **!!!** |
| **Violations** | 31 | 24 | 18 | **6** | **-12** |
| CRITICAL | 19 | 18 | 18 | **0** | **-18** |
| MAJOR | 12 | 6 | 0 | **6** | +6 |

#### KPIs (progression)

| Dimension | Step 1 | Step 2 | Step 3 | Step 4 | Delta vs 3 |
|-----------|-------:|-------:|-------:|-------:|:----------:|
| DDD Compliance | 0% | 0% | 0% | **100%** | **+100** |
| Hexagonal Architecture | 40% | 70% | 100% | **70%** | -30 |
| Dependencies | 0% | 0% | 0% | 0% | = |
| Coupling | 30% | 21% | 37% | **23%** | -14 |
| Cohesion | 58% | 58% | 64% | **71%** | **+7** |

#### Inventaire architectural (transformation)

| Composant | Step 3 | Step 4 | Delta | Observation |
|-----------|-------:|-------:|:-----:|-------------|
| Aggregate Roots | 0 | **6** | **+6** | Customer, Order, Product, Inventory, Payment, Shipment |
| Entities | 9 | **2** | **-7** | OrderLine, StockMovement |
| Value Objects | 3 | **7** | **+4** | Money, Address, Email, Quantity + 3 enums |
| Identifiers | 0 | **6** | **+6** | OrderId, CustomerId, ProductId, PaymentId, ShipmentId, InventoryId |
| Domain Events | 0 | **1** | **+1** | OrderPlacedEvent |
| Driving Ports | 6 | 6 | = | Inchanges |
| Driven Ports | 9 | 9 | = | Inchanges (mais sans adaptateur) |

#### 6 violations (0 CRITICAL, 6 MAJOR)

| Contrainte | Step 3 | Step 4 | Delta | Observation |
|------------|-------:|-------:|:-----:|-------------|
| `ddd:entity-identity` | 9 | **0** | **-9** | Identifiants types crees |
| `ddd:domain-purity` | 9 | **0** | **-9** | Annotations JPA supprimees |
| `hexagonal:port-coverage` | 0 | **6** | **+6** | 6 repos sans adaptateur (volontaire -- step 5 les generera) |

#### Metriques cles

| Metrique | Step 3 | Step 4 | Delta | Observation |
|----------|-------:|-------:|:-----:|-------------|
| Domain purity | 25,00% | **100,00%** | **+75** | Zero import d'infrastructure dans le domaine |
| Aggregate boundary | 0,00% | **100,00%** | **+100** | Entites accessibles uniquement via l'agregat |
| Code boilerplate ratio | 100,00% | **80,20%** | **-19,80** | Logique metier ajoutee (place, cancel, etc.) |
| Aggregate avg size | 0 | **14,33** | **+14,33** | Les agregats ont de vraies methodes metier |
| Domain coverage | 44,44% | **59,46%** | **+15,02** | Plus de types domaine dans le perimetre |
| Classification rate | 81,8% | **94,9%** | **+13,1** | 37/39 types classifies |

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

1. **PASSED** : premier audit qui passe. Le score bondit de 40 a 56, mais surtout le status passe de FAILED a PASSED (with warnings). Les 18 violations CRITICAL de l'etape 3 sont **toutes resolues**.
2. **DDD Compliance 100%** : le fait marquant. HexaGlue infere correctement les 22 types domaine : 6 aggregats, 6 identifiants, 2 entites, 7 value objects, 1 domain event. **Aucune classification explicite necessaire.**
3. **Domain purity 100%** : plus aucun import JPA dans le domaine. Les 9 violations `ddd:domain-purity` et les 9 violations `ddd:entity-identity` sont eliminees.
4. **Aggregate boundary 100%** : les entites (OrderLine, StockMovement) sont accessibles uniquement via leur agregat racine.
5. **Boilerplate 80%** (vs 100%) : les methodes metier (Order.place(), cancel(), markPaid(), Inventory.reserve(), etc.) ajoutent du vrai code domaine. aggregate.avgSize passe de 0 a 14.33 methodes.
6. **Hexagonal 70%** (vs 100%) : baisse attendue. Les JPA repos dual-interface ont ete supprimes (le domaine pur n'a plus @Entity). Les 6 driven ports n'ont plus d'adaptateur. C'est volontaire -- HexaGlue les generera a l'etape 5.
7. **6 violations MAJOR** (`hexagonal:port-coverage`) : les 6 repos domaine n'ont pas d'adaptateur. Ce n'est pas un bug -- c'est le prix transitoire d'un domaine pur avant la generation.
8. **Classification rate 94,9%** : seuls ShopApplication et ShippingRate restent non classifies (2/39).
9. **Conclusion** : le domaine est pret pour la generation. Toutes les informations DDD sont inferees, la purete est totale. L'etape 5 activera les plugins de generation pour combler les 6 ports non couverts et viser le score maximum.

---

## Etape 5 : Generation et audit (`step/5-generated`)

*A venir*

---

## Etape 6 : Application fonctionnelle (`main`)

*A venir*
