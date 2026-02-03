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

### Description

Creation de `hexaglue.yaml` avec des exclusions minimales pour nettoyer les faux positifs
identifies a l'etape 1. **Aucune classification explicite** -- on laisse HexaGlue inferer.

### Pourquoi exclure les services ?

A l'etape 1, les 8 services Spring sont classifies **VALUE_OBJECT** -- un faux positif
qui pollue l'inventaire et genere des violations trompeuses. Voici la mecanique :

1. **`@Service` n'est pas traite comme annotation d'infrastructure** par HexaGlue.
   C'est intentionnel : `AnchorDetector` exclut `@Service` des `INFRA_ANNOTATIONS`
   pour permettre la detection semantique des services applicatifs quand ils
   implementent des ports.

2. **Sans interfaces ports, les services ne sont pas `CoreAppClass`**.
   `CoreAppClassDetector` requiert que la classe implemente au moins une interface
   utilisateur (port driving potentiel) OU depende d'une interface utilisateur
   (port driven potentiel). Les services legacy ne font ni l'un ni l'autre :
   ils dependent directement de `JpaRepository` (extends Spring, pas user-code).

3. **Sans `CoreAppClass`, les criteres flexibles ne matchent pas**.
   Les criteres `APPLICATION_SERVICE`, `INBOUND_ONLY`, `OUTBOUND_ONLY`, `SAGA`
   requierent tous le flag `CoreAppClass`. Aucun ne s'applique.

4. **`EmbeddedValueObjectCriteria` matche par defaut** (priorite 70).
   Les services sont des classes concretes, sans champ identite, utilisees comme
   champs d'autres types (injection de dependances dans controllers et services).
   Cela suffit pour matcher VALUE_OBJECT.

**Consequences de cette misclassification :**
- 8 faux VALUE_OBJECT gonflent l'inventaire (14 VOs au lieu de 6)
- 1 fausse violation `ddd:domain-purity` sur OrderService (qui importe
  `jakarta.persistence.EntityNotFoundException`)
- 12 fausses violations `hexagonal:layer-isolation` (DOMAIN → PORT interdit,
  mais les services ne sont pas du domaine)

**Alternatives envisagees :**

| Option | Pour | Contre |
|--------|------|--------|
| **Exclure** (choisi) | Elimine le bruit, rapport plus lisible | Perd la visibilite sur la couche service |
| **Classifier APPLICATION_SERVICE** | Corrige le role, supprime les 12 violations layer-isolation (APPLICATION → PORT est autorise) | Premature : ces services legacy melangent logique metier, DTOs et infrastructure -- ce ne sont pas de vrais services applicatifs |
| **Ne rien faire** | Rien n'est cache | 8 faux VOs, 13 violations trompeuses, rapport inexploitable |

**Choix : exclure** comme compromis pragmatique. Les services seront correctement
restructures a l'etape 3 (ports + services applicatifs implementant les driving ports),
moment ou `CoreAppClassDetector` pourra les detecter automatiquement.

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

| Dimension | Step 1 | Step 2 | Delta | Poids | Contribution |
|-----------|-------:|-------:|:-----:|------:|-------------:|
| DDD Compliance | 0% | 0% | = | 25% | 0,0 |
| Hexagonal Architecture | 40% | **70%** | **+30** | 25% | 17,5 |
| Dependencies | 0% | 0% | = | 20% | 0,0 |
| Coupling | 30% | 21% | -9 | 15% | 3,2 |
| Cohesion | 58% | 58% | = | 15% | 8,7 |
| **TOTAL** | | | | 100% | **29,3** |

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

#### Classification

| | Step 1 | Step 2 | Delta |
|---|------:|------:|:-----:|
| Types parses | 50 | 50 | = |
| Types classifies | 30 | 21 | -9 |
| Taux de classification | 60,0% | **52,5%** | **-7,5** |
| Types domaine | 24 | 15 | -9 |
| Ports | 6 | 6 | = |
| Conflits | 0 | 0 | = |

Le taux de classification baisse car les 9 services exclus ne sont plus classifies.
10 types UNKNOWN necessitent toujours attention.

#### Plan de remediation

| | Manuel | Avec HexaGlue | Economies |
|---|-------:|-------:|-------:|
| **Effort** | 36,0 jours | 18,0 jours | **18,0 jours** |
| **Cout** | 18 000 EUR | 9 000 EUR | **9 000 EUR** |

| Action | Severite | Manuel | HexaGlue | Plugin |
|--------|----------|-------:|-------:|:------:|
| Corriger les 18 violations DDD (purete + identite) | CRITICAL | 18,0j | 18,0j | -- |
| Creer les adaptateurs pour les 6 driven ports | MAJOR | ~~18,0j~~ | **0j** | `jpa` |
| **TOTAL** | | 36,0j | **18,0j** | |

> **Fait nouveau** : pour la premiere fois, le plan de remediation montre des economies
> potentielles avec HexaGlue. Les 6 violations `hexagonal:port-coverage` (ports sans
> adaptateur) peuvent etre resolues automatiquement par le plugin JPA, economisant
> 18 jours (3 jours x 6 ports).

Les violations `port-coverage` incluent desormais des suggestions detaillees :
- 5 etapes (creer JPA entity, mapper, adapter, convertisseurs d'identite, enregistrer le bean)
- Exemple de code (`JpaOrderRepository implements OrderRepository`)
- Effort estime par port : 3 jours

#### Analyse de stabilite des packages

Inchangee par rapport a l'etape 1 (meme structure de packages, seule la classification change).

### Observations

1. **Score +6 points** (23 → 29) : amelioration modeste. Les exclusions reduisent le bruit mais ne resolvent pas les problemes structurels. Le detail des contributions montre que le gain vient de Hexagonal (17,5 vs 10,0) tandis que Coupling baisse (3,2 vs 4,5).
2. **Hexagonal +30 points** (40% → 70%) : c'est le gain principal. Les 12 violations `layer-isolation` disparaissent car les services (mal classifies DOMAIN) ne sont plus dans le perimetre. En contrepartie, 6 violations `port-coverage` apparaissent : les repositories n'ont pas d'adaptateur.
3. **Swap de contraintes hexagonales** : on passe de "domaine depend des ports" (faux positif) a "ports sans adaptateur" (probleme reel). Le rapport est plus pertinent.
4. **100% boilerplate** : sans les services, il ne reste que les entites JPA -- des data holders purs avec getters/setters. Le modele anemique est expose de maniere flagrante.
5. **Domain purity en baisse** (58% → 40%) : paradoxe des exclusions. Avec moins de types dans le perimetre, les 9 classes impures (imports JPA) pesent proportionnellement plus. La metrique reflète correctement l'etat du code restant.
6. **3 DTOs records persistent** comme VALUE_OBJECT : structurellement corrects (records sans identite). Pas genant a cette etape.
7. **Classification toujours plate** : tous les types domaine restent ENTITY a cause de @Entity JPA. HexaGlue ne distingue pas agregats, entites enfants et value objects. Taux de classification : 52,5% (21/40).
8. **Remediation actionnable** : le plan de remediation distingue 2 actions. L'action "creer les adaptateurs" (18 jours) est automatisable par le plugin JPA -- c'est la premiere apparition d'economies potentielles (50% du total). Les violations `port-coverage` incluent desormais un exemple de code et des etapes detaillees.
9. **Conclusion** : les exclusions nettoient le bruit de classification et ameliorent la pertinence du rapport hexagonal. Le plan de remediation montre deja un ROI potentiel de 50% avec HexaGlue. Mais le probleme fondamental reste : le domaine est couple a JPA. Il faut reorganiser en architecture hexagonale (etape 3) et purifier le domaine (etape 4).

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

| Dimension | Poids | Step 1 | Step 2 | Step 3 | Delta vs 2 | Contribution |
|-----------|:-----:|-------:|-------:|-------:|:----------:|-------------:|
| DDD Compliance | 25% | 0% | 0% | 0% | = | 0,0 |
| Hexagonal Architecture | 25% | 40% | 70% | **100%** | **+30** | **25,0** |
| Dependencies | 20% | 0% | 0% | 0% | = | 0,0 |
| Coupling | 15% | 30% | 21% | **37%** | **+16** | 5,6 |
| Cohesion | 15% | 58% | 58% | **64%** | **+6** | 9,6 |
| **TOTAL** | **100%** | | | **40/100** | **+11** | **40,2** |

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

#### Classification

| | Step 1 | Step 2 | Step 3 |
|---|-------:|-------:|-------:|
| Types parses | 50 | 40 | **33** |
| Types classifies | 30 | 21 | **27** |
| Taux | 60,0% | 52,5% | **81,8%** |

Repartition : 12 types domaine (9 entities, 3 value objects) + 15 ports (6 driving, 9 driven). Les 6 types non classifies sont hors perimetre DDD : ShopApplication, BaseEntity, 3 exceptions, GlobalExceptionHandler.

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

#### Adaptateurs (15)

**6 driving adapters** (application/) :

| Port | Adaptateur |
|------|------------|
| ProductUseCases | ProductApplicationService |
| CatalogUseCases | CatalogApplicationService |
| OrderUseCases | OrderApplicationService |
| CustomerUseCases | CustomerApplicationService |
| PaymentUseCases | PaymentApplicationService |
| ShippingUseCases | ShippingApplicationService |

**9 driven adapters** (infrastructure/ + application/) :

| Port | Adaptateur | Package |
|------|------------|---------|
| ProductRepository | JpaProductRepository | persistence |
| CustomerRepository | JpaCustomerRepository | persistence |
| OrderRepository | JpaOrderRepository | persistence |
| InventoryRepository | JpaInventoryRepository | persistence |
| PaymentRepository | JpaPaymentRepository | persistence |
| ShipmentRepository | JpaShipmentRepository | persistence |
| PaymentGateway | PaymentGatewayAdapter | external |
| NotificationSender | NotificationAdapter | external |
| InventoryUseCases | InventoryApplicationService | application |

> **Note** : les application services (`com.acme.shop.application.**`) sont exclus de la **classification** par `hexaglue.yaml`, mais HexaGlue les detecte comme **adaptateurs** des driving ports car ils implementent les interfaces de `ports/in/`. L'exclusion empeche l'attribution d'un ArchType (ex. APPLICATION_SERVICE), pas la detection de la relation structurelle port → implementeur. C'est le comportement attendu : l'adapter detection fonctionne par analyse d'implementation, independamment du pipeline de classification.

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

#### Plan de remediation

| | Manuel | Avec HexaGlue | Economies |
|---|-------:|-------:|-------:|
| **Effort** | 18,0 jours | 18,0 jours | 0,0 jours |
| **Cout** | 9 000 EUR | 9 000 EUR | 0 EUR |

1 seule action : "purifier le domaine" (supprimer les annotations JPA + ajouter les identifiants types). Pas d'economies HexaGlue a cette etape car les violations restantes sont purement DDD (manuelles). Le gain de 50% lie aux adaptateurs (etape 2) a disparu car les violations `port-coverage` sont resolues.

Effort total en baisse : 36 jours (etape 2) → 18 jours (etape 3). La restructuration hexagonale a divise l'effort de remediation par 2.

#### Stabilite des packages

La restructuration hexagonale transforme la topologie des packages. Les metriques de Martin revelent un layout conforme :

| Package | Ca | Ce | I | A | D | Zone |
|---------|---:|---:|----:|----:|----:|------|
| ports.out | 15 | 9 | 0,38 | 1,00 | 0,38 | Main Sequence |
| ports.in | 12 | 9 | 0,43 | 1,00 | 0,43 | Main Sequence |
| domain.shared | 9 | 0 | 0,00 | 1,00 | 0,00 | Main Sequence |
| domain.order | 7 | 3 | 0,30 | 0,00 | 0,70 | Main Sequence |
| domain.product | 9 | 1 | 0,10 | 0,00 | 0,90 | Zone of Pain |
| application | 0 | 26 | 1,00 | 0,00 | 0,00 | Main Sequence |
| infrastructure.web | 0 | 15 | 1,00 | 0,00 | 0,00 | Main Sequence |
| infrastructure.persistence | 0 | 6 | 1,00 | 1,00 | 1,00 | Main Sequence |

Points notables :
- **ports.in et ports.out** sur la Main Sequence (A=1,00) : interfaces pures, forte dependance entrante, conforme au role de contrat
- **application** (I=1,00, A=0,00) : maximum instable, aucune dependance entrante -- conforme au role d'implementation consommable
- **domain.product** en Zone of Pain (D=0,90) : package concret tres stable, difficile a modifier car tres reference (Ca=9)

#### Rapports generes

- `AUDIT-REPORT.md` : rapport complet avec diagrammes Mermaid (radar, C4 System Context, C4 Component, domain model, ports, stability quadrant)
- `audit-report.json` : donnees structurees (verdict, architecture, issues, remediation, appendix avec package metrics)
- `audit-report.html` : dashboard interactif

### Observations

1. **Score +11 points** (29 → 40) : la restructuration hexagonale apporte un gain significatif. Le detail des contributions montre que le gain vient exclusivement de Hexagonal (25,0 vs 17,5) et Coupling (5,6 vs 3,2). Les axes DDD et Dependencies restent a 0.
2. **Hexagonal 100%** : c'est le fait marquant. Les 15 ports sont detectes, tous les driven ports ont un adaptateur (dual-interface JpaXxxRepository). Les 6 violations `port-coverage` de l'etape 2 disparaissent.
3. **0 violations MAJOR** : il ne reste que les 18 CRITICAL -- toutes liees au domaine (identite + purete). L'architecture hexagonale est completement validee.
4. **15 adaptateurs detectes malgre les exclusions** : 6 driving (application services) + 9 driven (infrastructure). Les application services sont exclus de la classification (`application.**` dans `hexaglue.yaml`) mais detectes comme adaptateurs car ils implementent les interfaces de `ports/in/`. HexaGlue distingue classification (attribution d'un ArchType) et detection d'adaptateurs (analyse d'implementation). Le rapport C4 Component les represente avec les ports et les relations d'implementation.
5. **InventoryUseCases misclassifie** : cette interface dans `ports/in/` est detectee comme driven port de type REPOSITORY. Misclassification mineure liee a la presence de methodes CRUD dans le contrat.
6. **3 Value Objects** (vs 6) : les 3 DTOs records deplaces vers `infrastructure/web/dto/` sont exclus. Il reste les 3 enums (Category, OrderStatus, PaymentStatus).
7. **Taux de classification en hausse** (52,5% → 81,8%) : la restructuration hexagonale aide la classification. Les ports sont identifies grace au package naming (`ports/in/`, `ports/out/`). Les 6 types non classifies sont ShopApplication, BaseEntity, exceptions.
8. **Remediation en baisse** : 36 jours / 18 000 EUR (etape 2) → 18 jours / 9 000 EUR. La resolution des violations `port-coverage` divise l'effort par 2. Plus d'economies HexaGlue car les violations restantes (DDD) sont manuelles.
9. **Package stability conforme** : les ports (A=1,00) sont sur la Main Sequence, l'application et l'infrastructure (I=1,00) sont instables comme attendu. Le domaine concret (A=0,00) est en zone limitrophe (Distance 0,60-0,90).
10. **Conclusion** : l'architecture hexagonale est completement en place. Le prochain levier est la purification du domaine (etape 4) : supprimer les annotations JPA et ajouter des identifiants types pour resoudre les 18 violations CRITICAL restantes.

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

| Dimension | Poids | Step 1 | Step 2 | Step 3 | Step 4 | Delta vs 3 | Contribution |
|-----------|:-----:|-------:|-------:|-------:|-------:|:----------:|-------------:|
| DDD Compliance | 25% | 0% | 0% | 0% | **100%** | **+100** | **25,0** |
| Hexagonal Architecture | 25% | 40% | 70% | 100% | **70%** | -30 | 17,5 |
| Dependencies | 20% | 0% | 0% | 0% | 0% | = | 0,0 |
| Coupling | 15% | 30% | 21% | 37% | **23%** | -14 | 3,5 |
| Cohesion | 15% | 58% | 58% | 64% | **71%** | **+7** | 10,7 |
| **TOTAL** | **100%** | | | | **56/100** | **+16** | **56,6** |

#### Inventaire architectural (transformation)

| Composant | Step 3 | Step 4 | Delta | Observation |
|-----------|-------:|-------:|:-----:|-------------|
| Aggregate Roots | 0 | **6** | **+6** | Customer, Order, Product, Inventory, Payment, Shipment |
| Entities | 9 | **2** | **-7** | OrderLine, StockMovement |
| Value Objects | 3 | **7** | **+4** | Money, Address, Email, Quantity + 3 enums |
| Identifiers | 0 | **6** | **+6** | OrderId, CustomerId, ProductId, PaymentId, ShipmentId, InventoryId |
| Domain Events | 0 | **1** | **+1** | OrderPlacedEvent |
| Driving Ports | 6 | 6 | = | Inchanges |
| Driven Ports | 9 | 9 | = | 6 sans adaptateur (JPA repos supprimes) |

#### Classification

| | Step 1 | Step 2 | Step 3 | Step 4 |
|---|-------:|-------:|-------:|-------:|
| Types parses | 50 | 40 | 33 | **39** |
| Types classifies | 30 | 21 | 27 | **37** |
| Taux | 60,0% | 52,5% | 81,8% | **94,9%** |

Repartition : 22 types domaine (6 AR, 2 entities, 7 VOs, 6 identifiers, 1 event) + 15 ports (6 driving, 9 driven). Seuls 2 types non classifies : ShopApplication et ShippingRate (UNKNOWN).

#### Adaptateurs (9)

> Les 6 JPA repos dual-interface ont ete supprimes (le domaine pur n'a plus @Entity). Il ne reste que 9 adaptateurs.

**6 driving adapters** (application/) :

| Port | Adaptateur |
|------|------------|
| CustomerUseCases | CustomerApplicationService |
| ShippingUseCases | ShippingApplicationService |
| OrderUseCases | OrderApplicationService |
| ProductUseCases | ProductApplicationService |
| PaymentUseCases | PaymentApplicationService |
| CatalogUseCases | CatalogApplicationService |

**3 driven adapters** (infrastructure/ + application/) :

| Port | Adaptateur | Package |
|------|------------|---------|
| NotificationSender | NotificationAdapter | external |
| PaymentGateway | PaymentGatewayAdapter | external |
| InventoryUseCases | InventoryApplicationService | application |

> **6 driven ports sans adaptateur** : ShipmentRepository, InventoryRepository, CustomerRepository, ProductRepository, OrderRepository, PaymentRepository. C'est volontaire -- HexaGlue les generera a l'etape 5.

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

#### Plan de remediation

| | Manuel | Avec HexaGlue | Economies |
|---|-------:|-------:|-------:|
| **Effort** | 18,0 jours | **0,0 jours** | **18,0 jours** |
| **Cout** | 9 000 EUR | **0 EUR** | **9 000 EUR** |

| Action | Manuel | HexaGlue | Plugin |
|--------|-------:|-------:|:------:|
| Creer les adaptateurs infrastructure | ~~18,0d~~ | **0d** | `jpa` |
| **TOTAL** | 18,0d | **0,0d** | |

C'est le moment cle de la migration : le domaine est pur et completement classifie, le plugin JPA peut generer **100% des adaptateurs manquants** automatiquement. L'effort manuel (18 jours / 9 000 EUR) tombe a zero. Chaque violation `port-coverage` inclut un exemple de code et 5 etapes detaillees (creer l'entite JPA, le mapper, l'adaptateur, les convertisseurs d'identite, l'enregistrement DI).

#### Stabilite des packages

| Package | Ca | Ce | I | A | D | Zone |
|---------|---:|---:|----:|----:|----:|------|
| ports.out | 9 | 17 | 0,65 | 1,00 | 0,65 | Main Sequence |
| ports.in | 12 | 13 | 0,52 | 1,00 | 0,52 | Main Sequence |
| domain.order | 23 | 2 | 0,08 | 0,00 | 0,92 | Zone of Pain |
| domain.product | 12 | 1 | 0,08 | 0,00 | 0,92 | Zone of Pain |
| domain.customer | 11 | 1 | 0,08 | 0,00 | 0,92 | Zone of Pain |
| domain.shipping | 4 | 3 | 0,43 | 0,00 | 0,57 | Main Sequence |
| domain.payment | 4 | 2 | 0,33 | 0,00 | 0,67 | Main Sequence |
| application | 0 | 28 | 1,00 | 0,00 | 0,00 | Main Sequence |

Points notables :
- **domain.order en Zone of Pain** (D=0,92, Ca=23) : package domaine le plus reference du projet (Money, Address, OrderId, Quantity utilises partout). Difficile a modifier sans impact. Signe d'un sous-domaine central qui merite potentiellement un decoupage.
- **ports.out** (I=0,65 vs 0,38 a l'etape 3) : instabilite en hausse car les ports referent maintenant les types domaine enrichis (identifiants types, value objects).
- **application** (Ce=28 vs 26) : legere hausse des dependances sortantes, les services utilisent les nouveaux types domaine.

#### Rapports generes

- `AUDIT-REPORT.md` : rapport avec diagrammes Mermaid (radar, C4, domain model avec identifiants et value objects, ports avec couverture, stability quadrant)
- `audit-report.json` : donnees structurees (verdict, architecture avec aggregats enrichis, issues avec suggestions detaillees et code exemple, remediation avec economies HexaGlue)
- `audit-report.html` : dashboard interactif

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

1. **PASSED** : premier audit qui passe. Le score bondit de 40 a 56 (+16), mais surtout le status passe de FAILED a PASSED (with warnings). Les 18 violations CRITICAL de l'etape 3 sont **toutes resolues**. Le detail des contributions montre que DDD (25,0) remplace Hexagonal (17,5 vs 25,0) comme premier contributeur.
2. **DDD Compliance 100%** : le fait marquant. HexaGlue infere correctement les 22 types domaine : 6 aggregats, 6 identifiants, 2 entites, 7 value objects, 1 domain event. **Aucune classification explicite necessaire** -- tout est infere par structure (CERTAIN_BY_STRUCTURE) ou convention de nommage (INFERRED pour OrderPlacedEvent).
3. **Domain purity 100%** : plus aucun import JPA dans le domaine. Les 9 violations `ddd:domain-purity` et les 9 violations `ddd:entity-identity` sont eliminees d'un coup.
4. **Aggregate boundary 100%** : les entites (OrderLine, StockMovement) sont accessibles uniquement via leur agregat racine.
5. **Boilerplate 80%** (vs 100%) : les methodes metier (Order.place(), cancel(), markPaid(), Inventory.reserve(), etc.) ajoutent du vrai code domaine. aggregate.avgSize passe de 0 a 14,33 methodes.
6. **Hexagonal 70%** (vs 100%) : baisse attendue. Les JPA repos dual-interface ont ete supprimes (le domaine pur n'a plus @Entity). 6 driven ports sur 9 n'ont plus d'adaptateur. Les 3 restants (NotificationAdapter, PaymentGatewayAdapter, InventoryApplicationService) sont conserves.
7. **Remediation : 100% d'economies HexaGlue** : c'est le point culminant. L'effort passe de 18 jours / 9 000 EUR (manuel) a **0 jours / 0 EUR** avec le plugin JPA. Les 6 violations `port-coverage` incluent chacune un exemple de code et 5 etapes detaillees. La promesse HexaGlue se concretise.
8. **Classification rate 94,9%** (vs 81,8%) : seuls ShopApplication et ShippingRate restent non classifies (2/39). ShippingRate est un cas interessant : c'est un record avec un champ Money, mais il n'est pas detecte comme VALUE_OBJECT (probablement car il n'est pas embarque dans un agregat).
9. **domain.order en Zone of Pain** (D=0,92, Ca=23) : le sous-domaine order concentre Money, Address, OrderId, Quantity -- des types utilises par tout le projet. Cette centralite rend le package fragile aux modifications.
10. **Conclusion** : le domaine est pret pour la generation. Toutes les informations DDD sont inferees, la purete est totale. Le plugin JPA peut generer les 6 adaptateurs manquants automatiquement. L'etape 5 activera la generation pour combler les ports non couverts et viser le score maximum.

---

## Etape 5 : Generation et audit (`step/5-generated`)

*A venir*

---

## Etape 6 : Application fonctionnelle (`main`)

*A venir*
