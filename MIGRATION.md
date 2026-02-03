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

| Dimension | Score | Poids | Contribution | Seuil | Status |
|-----------|------:|------:|-------------:|------:|:------:|
| DDD Compliance | 0% | 25% | 0,0 | 90% | CRITICAL |
| Hexagonal Architecture | 40% | 25% | 10,0 | 90% | CRITICAL |
| Dependencies | 0% | 20% | 0,0 | 80% | CRITICAL |
| Coupling | 30% | 15% | 4,5 | 70% | CRITICAL |
| Cohesion | 58% | 15% | 8,7 | 80% | CRITICAL |
| **TOTAL** | | 100% | **23,2** | | |

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

#### Classification

| | Valeur |
|---|------:|
| Types parses | 50 |
| Types classifies | 30 |
| Taux de classification | 60,0% |
| Types domaine | 24 |
| Ports | 6 |
| Conflits | 0 |

10 types non classifies (UNKNOWN) necessitant attention, dont :
`ShopApplication`, `CustomerResponse`, `OrderResponse`, `ProductSearchRequest`,
`ShipmentResponse`, et 5 autres (DTOs, configs, utilitaires)

#### Plan de remediation

| | Manuel | Avec HexaGlue | Economies |
|---|-------:|-------:|-------:|
| **Effort** | 31,0 jours | 31,0 jours | 0,0 jours |
| **Cout** | 15 500 EUR | 15 500 EUR | 0 EUR |

A cette etape, HexaGlue ne peut pas encore apporter d'economies car le code
n'est pas structure en architecture hexagonale.

#### Analyse de stabilite des packages

| Package | Ca | Ce | I | A | D | Zone |
|---------|---:|---:|--:|--:|--:|------|
| shop.model | 18 | 0 | 0,00 | 0,08 | 0,92 | Zone of Pain |
| shop.dto | 5 | 1 | 0,17 | 0,00 | 0,83 | Zone of Pain |
| shop.service | 5 | 18 | 0,78 | 0,00 | 0,22 | Stable Core |
| shop.repository | 7 | 9 | 0,56 | 1,00 | 0,56 | Main Sequence |
| shop.controller | 0 | 15 | 1,00 | 0,00 | 0,00 | Main Sequence |
| shop.event | 1 | 1 | 0,50 | 0,00 | 0,50 | Main Sequence |

> Ca = couplage afferent, Ce = couplage efferent, I = instabilite, A = abstraction, D = distance

**Observations stabilite** : `shop.model` est dans la "Zone of Pain" (D=0,92) : tres
concret et tres stable (beaucoup de dependants, peu de dependances). C'est attendu pour
un package modele legacy. `shop.service` est dans le "Stable Core" (D=0,22) : correct pour
des services applicatifs. `shop.repository` est bien sur la "Main Sequence" grace a son
abstractness de 1,00 (interfaces pures).

#### Rapports generes

- `target/hexaglue/reports/audit/audit-report.json` : donnees structurees (verdict, architecture, issues, remediation, package metrics)
- `target/hexaglue/reports/audit/audit-report.html` : tableau de bord interactif
- `target/hexaglue/reports/audit/AUDIT-REPORT.md` : rapport markdown incluant :
  - Score Radar (diagramme Mermaid)
  - Diagrammes C4 (System Context, Component, Full Architecture)
  - Modele de domaine et couche des ports (diagrammes de classes Mermaid)
  - Distribution des violations (pie chart)
  - Detail de chaque violation avec localisation et suggestion de correction
  - Plan de remediation avec estimation d'effort et cout
  - Analyse de stabilite des packages (quadrant chart Mermaid)
  - Metriques de packages (Ca, Ce, I, A, D, Zone)

### Observations

1. **Score 23/100 (Grade F)** : baseline quantifiee de l'etat legacy. Ce score servira de reference pour mesurer la progression a chaque etape. Le detail des contributions (DDD 0, Hex 10, Deps 0, Coupling 4,5, Cohesion 8,7 = 23,2) montre que seules les dimensions Coupling et Cohesion contribuent au score.
2. **0% DDD Compliance** : aucune structure DDD detectee (pas d'aggregats, pas d'identifiants types, pas de value objects intentionnels).
3. **9 entites sans identite** (`ddd:entity-identity`) : les entites heritent de `BaseEntity` qui porte un `Long id` via `@MappedSuperclass`, mais HexaGlue ne reconnait pas cela comme un champ identite type.
4. **10 violations de purete domaine** (`ddd:domain-purity`) : toutes les classes `model/` importent des annotations JPA (`jakarta.persistence.*`). L'infrastructure est melee au domaine.
5. **12 violations d'isolation** (`hexagonal:layer-isolation`) : les services (classifies DOMAIN) dependent directement des repositories (classifies PORT). Dans une architecture hexagonale, le domaine ne devrait pas connaitre les ports.
6. **81,58% de boilerplate** : getters, setters, constructeurs vides -- modele anemique typique.
7. **40% Hexagonal Architecture** : HexaGlue detecte bien les 6 repositories comme driven ports, mais l'absence de driving ports et les violations de couches plombent le score.
8. **Classification bruyante** : 8 services Spring classes VALUE_OBJECT et 3 DTOs records sont des faux positifs a nettoyer a l'etape 2. Taux de classification : 60% (30/50 types classifies).
9. **Remediation : 31 jours / 15 500 EUR** estimes. A cette etape, aucune economie possible avec HexaGlue (code non structure).
10. **Stabilite des packages** : `shop.model` dans la "Zone of Pain" (D=0,92) confirme le couplage excessif du modele legacy. `shop.repository` est bien sur la "Main Sequence" (abstractness 1,00).
11. **Rapports enrichis** : le rapport Markdown inclut des diagrammes C4 (System Context et Component), un radar des KPIs, et une analyse de stabilite des packages (quadrant chart). Ces visualisations fournissent une vue architecturale complete des la premiere analyse.
12. **Conclusion** : l'audit fournit une baseline chiffree precise. Les 3 familles de violations identifient clairement les axes de travail : (1) creer des identifiants types, (2) purifier le domaine des imports JPA, (3) isoler les couches via des ports.

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
