# Migration vers HexaGlue - Etude de cas E-commerce

Ce document retrace la migration progressive d'une application e-commerce
Spring Boot "enterprise standard" vers une architecture hexagonale avec HexaGlue.

Chaque étape correspond à une branche Git et documente les observations,
les problèmes rencontrés et les résultats obtenus.

---

## Etape 0 : Application Legacy (`step/0-legacy`)

### Description

Application e-commerce classique Spring Boot avec les anti-patterns
typiques d'une application d'enterprise :

- **50 classes Java** réparties en packages techniques (controller, service, model, repository, dto, config, exception, util, event)
- **Spring Boot 3.2.5** avec Spring Data JPA et H2

### Anti-patterns présents

| # | Anti-pattern | Exemple |
|---|-------------|---------|
| 1 | `@Entity` sur classes domaine | `Order`, `Customer`, `Product` héritent de `BaseEntity` avec `@MappedSuperclass` |
| 2 | `@Service` partout | Services applicatifs ET "domaine" marqués `@Service` |
| 3 | Pas de ports | Services dépendent directement des repositories Spring Data |
| 4 | Modèle anémique | Toute la logique dans les services, entités = data holders avec setters |
| 5 | Primitives au lieu de value objects | `BigDecimal` pour le montant, `String` pour l'email |
| 6 | Références directes entre agrégats | `Order.customer` = `Customer` (entity), pas `CustomerId` |
| 7 | `BaseEntity` technique | `@MappedSuperclass` avec `Long id`, `createdAt`, `updatedAt` |
| 8 | Events Spring | `OrderCreatedEvent extends ApplicationEvent` |
| 9 | Logique métier dans controllers | Validation de prix dans `ProductController` |
| 10 | Infrastructure = domaine | Aucune séparation, tout est un bean Spring + entity JPA |

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

### Vérification

```bash
mvn clean compile   # BUILD SUCCESS - 50 source files
```

### Observations

L'application compile et fonctionne, mais le domaine métier est
complètement enfoui sous l'infrastructure Spring/JPA. Il est impossible
de distinguer ce qui est du domaine pur de ce qui est de l'infrastructure.

C’est exactement le type d’application pour lequel HexaGlue est conçu, afin d’en analyser la structure et d’en faciliter la migration.

---

## Etape 1 : Découverte avec HexaGlue (`step/1-discovery`)

### Description

Ajout du plugin Maven HexaGlue avec le plugin d'audit (sans extensions, sans plugins de
génération). Premier lancement de `hexaglue:audit` sur le code legacy brut, sans aucun
`hexaglue.yaml`, pour obtenir une baseline mesurable de l'état architectural.

### Modifications

- `pom.xml` : ajout du plugin `hexaglue-maven-plugin:5.0.0-SNAPSHOT` avec `basePackage=com.acme.shop`,
  `failOnError=false`, et `hexaglue-plugin-audit:2.0.0-SNAPSHOT` comme dependance plugin
- Pas de `hexaglue.yaml` : on observe le comportement brut

### Résultats

```
mvn clean compile     → BUILD SUCCESS
mvn hexaglue:audit    → BUILD SUCCESS (audit FAILED attendu : 19 violations critiques)
```

#### Verdict

| Score | Grade | Status |
|:-----:|:-----:|:------:|
| **15/100** | **F** | **FAILED** |

#### KPIs (baseline legacy)

| Dimension | Score | Poids | Contribution | Seuil | Status |
|-----------|------:|------:|-------------:|------:|:------:|
| DDD Compliance | 0% | 25% | 0,0 | 90% | CRITICAL |
| Hexagonal Architecture | 10% | 25% | 2,5 | 90% | CRITICAL |
| Dependencies | 0% | 20% | 0,0 | 80% | CRITICAL |
| Coupling | 30% | 15% | 4,5 | 70% | CRITICAL |
| Cohesion | 58% | 15% | 8,7 | 80% | CRITICAL |
| **TOTAL** | | 100% | **15,7** | | |

#### Inventaire architectural

| Composant | Nombre | Observation |
|-----------|-------:|-------------|
| Aggregate Roots | 0 | Aucun détecté (pas de distinction agrégat/entité) |
| Entities | 9 | Tous les @Entity JPA |
| Value Objects | 14 | 3 enums (correct) + 8 services + 3 DTOs (faux positifs) |
| Identifiers | 0 | Pas d'identifiants typés |
| Domain Events | 1 | OrderCreatedEvent (Spring ApplicationEvent - faux positif) |
| Driving Ports | 0 | Aucun port driving |
| Driven Ports | 6 | Les 6 repositories Spring Data |

#### 37 violations (19 CRITICAL, 18 MAJOR)

| Contrainte | Nombre | Sévérité | Types concernés |
|------------|-------:|----------|-----------------|
| `ddd:domain-purity` | 10 | CRITICAL | 9 modèles + OrderService avec imports JPA interdits |
| `ddd:entity-identity` | 9 | CRITICAL | 9 entités sans champ identité typé (héritent de BaseEntity) |
| `hexagonal:layer-isolation` | 12 | MAJOR | 7 services dépendent directement des repositories |
| `hexagonal:port-direction` | 6 | MAJOR | 6 driven ports non utilisés par un service applicatif |

#### Métriques clés

| Métrique | Valeur | Seuil | Status |
|----------|-------:|------:|:------:|
| Domain coverage | 80,00% | min 30% | OK |
| Code boilerplate ratio | 81,58% | max 50% | CRITICAL |
| Domain purity | 58,33% | min 100% | CRITICAL |
| Aggregate boundary | 0,00% | min 80% | CRITICAL |
| Code complexity | 1,11 | max 10 | OK |

#### Classification

| | Valeur |
|---|------:|
| Types parsés | 50 |
| Types classifiés | 30 |
| Taux de classification | 60,0% |
| Types domaine | 24 |
| Ports | 6 |
| Conflits | 0 |

10 types non classifiés (UNKNOWN) nécessitant attention, dont :
`ShopApplication`, `CustomerResponse`, `OrderResponse`, `ProductSearchRequest`,
`ShipmentResponse`, et 5 autres (DTOs, configs, utilitaires)

#### Plan de remédiation

| | Manuel | Avec HexaGlue | Économies |
|---|-------:|-------:|-------:|
| **Effort** | 26,5 jours | 26,5 jours | 0,0 jours |
| **Coût** | 13 250 EUR | 13 250 EUR | 0 EUR |

| Action | Sévérité | Effort | Violations résolues |
|--------|----------|-------:|--------------------:|
| Corriger les 19 violations DDD (pureté + identité) | CRITICAL | 19,0j | 19 |
| Corriger la direction des ports | MAJOR | 1,5j | 6 |
| Router les dépendances via les ports appropriés | MAJOR | 6,0j | 12 |
| **TOTAL** | | **26,5j** | **37** |

À cette étape, HexaGlue ne peut pas encore apporter d'économies car le code
n'est pas structuré en architecture hexagonale.

#### Analyse de stabilité des packages

| Package | Ca | Ce | I | A | D | Zone |
|---------|---:|---:|--:|--:|--:|------|
| shop.model | 18 | 0 | 0,00 | 0,08 | 0,92 | Zone of Pain |
| shop.dto | 5 | 1 | 0,17 | 0,00 | 0,83 | Zone of Pain |
| shop.service | 5 | 18 | 0,78 | 0,00 | 0,22 | Stable Core |
| shop.repository | 7 | 9 | 0,56 | 1,00 | 0,56 | Main Sequence |
| shop.controller | 0 | 15 | 1,00 | 0,00 | 0,00 | Main Sequence |
| shop.event | 1 | 1 | 0,50 | 0,00 | 0,50 | Main Sequence |

> Ca = couplage afférent, Ce = couplage efférent, I = instabilité, A = abstraction, D = distance

**Observations stabilité** : `shop.model` est dans la "Zone of Pain" (D=0,92) : très
concret et très stable (beaucoup de dépendants, peu de dépendances). C'est attendu pour
un package modèle legacy. `shop.service` est dans le "Stable Core" (D=0,22) : correct pour
des services applicatifs. `shop.repository` est bien sur la "Main Sequence" grâce à son
abstractness de 1,00 (interfaces pures).

#### Rapports générés

- `target/hexaglue/reports/audit/audit-report.json` : données structurées (verdict, architecture, issues, remediation, package metrics)
- `target/hexaglue/reports/audit/audit-report.html` : tableau de bord interactif
- `target/hexaglue/reports/audit/AUDIT-REPORT.md` : rapport markdown incluant :
  - Score Radar (diagramme Mermaid)
  - Diagrammes C4 (System Context, Component, Full Architecture)
  - Modèle de domaine et couche des ports (diagrammes de classes Mermaid)
  - Distribution des violations (pie chart)
  - Détail de chaque violation avec localisation et suggestion de correction
  - Plan de remédiation avec estimation d'effort et coût
  - Analyse de stabilité des packages (quadrant chart Mermaid)
  - Métriques de packages (Ca, Ce, I, A, D, Zone)

### Observations

1. **Score 15/100 (Grade F)** : baseline quantifiée de l'état legacy. Ce score servira de référence pour mesurer la progression à chaque étape. Le détail des contributions (DDD 0, Hex 2,5, Deps 0, Coupling 4,5, Cohesion 8,7 = 15,7) montre que seules les dimensions Coupling et Cohesion contribuent significativement au score.
2. **0% DDD Compliance** : aucune structure DDD détectée (pas d'agrégats, pas d'identifiants typés, pas de value objects intentionnels).
3. **9 entités sans identité** (`ddd:entity-identity`) : les entités héritent de `BaseEntity` qui porte un `Long id` via `@MappedSuperclass`, mais HexaGlue ne reconnaît pas cela comme un champ identité typé.
4. **10 violations de pureté domaine** (`ddd:domain-purity`) : toutes les classes `model/` importent des annotations JPA (`jakarta.persistence.*`). L'infrastructure est mêlée au domaine.
5. **12 violations d'isolation** (`hexagonal:layer-isolation`) : les services (classifiés DOMAIN) dépendent directement des repositories (classifiés PORT). Dans une architecture hexagonale, le domaine ne devrait pas connaître les ports.
6. **6 violations de direction de ports** (`hexagonal:port-direction`) : les 6 driven ports ne sont utilisés par aucun service applicatif. C'est attendu puisqu'il n'y a pas encore de couche application structurée.
7. **81,58% de boilerplate** : getters, setters, constructeurs vides -- modèle anémique typique.
8. **10% Hexagonal Architecture** : HexaGlue détecte les 6 repositories comme driven ports, mais l'absence de driving ports, les violations de couches et les violations de direction plombent le score.
9. **Classification bruyante** : 8 services Spring classifiés VALUE_OBJECT et 3 DTOs records sont des faux positifs à nettoyer à l'étape 2. Taux de classification : 60% (30/50 types classifiés).
10. **Remédiation : 26,5 jours / 13 250 EUR** estimés, répartis en 3 actions : DDD (19j), direction des ports (1,5j), isolation des couches (6j). À cette étape, aucune économie possible avec HexaGlue (code non structuré).
11. **Stabilité des packages** : `shop.model` dans la "Zone of Pain" (D=0,92) confirme le couplage excessif du modèle legacy. `shop.repository` est bien sur la "Main Sequence" (abstractness 1,00).
12. **Rapports enrichis** : le rapport Markdown inclut des diagrammes C4 (System Context et Component), un radar des KPIs, et une analyse de stabilité des packages (quadrant chart). Ces visualisations fournissent une vue architecturale complète dès la première analyse.
13. **Conclusion** : l'audit fournit une baseline chiffrée précise. Les 4 familles de violations identifient clairement les axes de travail : (1) créer des identifiants typés, (2) purifier le domaine des imports JPA, (3) isoler les couches via des ports, (4) connecter les driven ports aux services applicatifs.

---

## Étape 2 : Configuration et exclusions (`step/2-configured`)

### Description

Création de `hexaglue.yaml` avec des exclusions minimales pour nettoyer les faux positifs
identifiés à l'étape 1. **Aucune classification explicite** -- on laisse HexaGlue inférer.

### Pourquoi exclure les services ?

À l'étape 1, les 8 services Spring sont classifiés **VALUE_OBJECT** -- un faux positif
qui pollue l'inventaire et génère des violations trompeuses. Voici la mécanique :

1. **`@Service` n'est pas traité comme annotation d'infrastructure** par HexaGlue.
   C'est intentionnel : `AnchorDetector` exclut `@Service` des `INFRA_ANNOTATIONS`
   pour permettre la détection sémantique des services applicatifs quand ils
   implémentent des ports.

2. **Sans interfaces ports, les services ne sont pas `CoreAppClass`**.
   `CoreAppClassDetector` requiert que la classe implémente au moins une interface
   utilisateur (port driving potentiel) OU dépende d'une interface utilisateur
   (port driven potentiel). Les services legacy ne font ni l'un ni l'autre :
   ils dépendent directement de `JpaRepository` (extends Spring, pas user-code).

3. **Sans `CoreAppClass`, les critères flexibles ne matchent pas**.
   Les critères `APPLICATION_SERVICE`, `INBOUND_ONLY`, `OUTBOUND_ONLY`, `SAGA`
   requièrent tous le flag `CoreAppClass`. Aucun ne s'applique.

4. **`EmbeddedValueObjectCriteria` matche par défaut** (priorité 70).
   Les services sont des classes concrètes, sans champ identité, utilisées comme
   champs d'autres types (injection de dépendances dans controllers et services).
   Cela suffit pour matcher VALUE_OBJECT.

**Conséquences de cette misclassification :**
- 8 faux VALUE_OBJECT gonflent l'inventaire (14 VOs au lieu de 6)
- 1 fausse violation `ddd:domain-purity` sur OrderService (qui importe
  `jakarta.persistence.EntityNotFoundException`)
- 12 fausses violations `hexagonal:layer-isolation` (DOMAIN → PORT interdit,
  mais les services ne sont pas du domaine)

**Alternatives envisagées :**

| Option | Pour | Contre |
|--------|------|--------|
| **Exclure** (choisi) | Élimine le bruit, rapport plus lisible | Perd la visibilité sur la couche service |
| **Classifier APPLICATION_SERVICE** | Corrige le rôle, supprime les 12 violations layer-isolation (APPLICATION → PORT est autorisé) | Prématuré : ces services legacy mélangent logique métier, DTOs et infrastructure -- ce ne sont pas de vrais services applicatifs |
| **Ne rien faire** | Rien n'est caché | 8 faux VOs, 13 violations trompeuses, rapport inexploitable |

**Choix : exclure** comme compromis pragmatique. Les services seront correctement
restructurés à l'étape 3 (ports + services applicatifs implémentant les driving ports),
moment où `CoreAppClassDetector` pourra les détecter automatiquement.

### Modifications

- `hexaglue.yaml` (nouveau) : exclusion des services (`com.acme.shop.service.*`) et de l'event Spring (`com.acme.shop.event.*`)
- `pom.xml` : ajout du plugin d'audit et `failOnError=false` (même configuration que step 1)

### Résultats

```
mvn clean compile     → BUILD SUCCESS
mvn hexaglue:audit    → BUILD SUCCESS (audit FAILED attendu : 18 violations critiques)
```

#### Verdict

| | Step 1 | Step 2 | Delta |
|---|:------:|:------:|:-----:|
| **Score** | 15/100 | **21/100** | **+6** |
| **Grade** | F | **F** | = |
| **Violations** | 37 | **30** | **-7** |
| CRITICAL | 19 | 18 | -1 |
| MAJOR | 18 | 12 | -6 |

#### KPIs (progression)

| Dimension | Step 1 | Step 2 | Delta | Poids | Contribution |
|-----------|-------:|-------:|:-----:|------:|-------------:|
| DDD Compliance | 0% | 0% | = | 25% | 0,0 |
| Hexagonal Architecture | 10% | **40%** | **+30** | 25% | 10,0 |
| Dependencies | 0% | 0% | = | 20% | 0,0 |
| Coupling | 30% | 21% | -9 | 15% | 3,2 |
| Cohesion | 58% | 58% | = | 15% | 8,7 |
| **TOTAL** | | | | 100% | **21,9** |

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

#### 30 violations (18 CRITICAL, 12 MAJOR)

| Contrainte | Step 1 | Step 2 | Delta | Observation |
|------------|-------:|-------:|:-----:|-------------|
| `ddd:entity-identity` | 9 | 9 | = | Mêmes 9 entités sans identité typé |
| `ddd:domain-purity` | 10 | **9** | **-1** | OrderService exclu |
| `hexagonal:layer-isolation` | 12 | **0** | **-12** | Disparu : services exclus du périmètre |
| `hexagonal:port-coverage` | 0 | **6** | **+6** | Nouveau : 6 repositories sans adaptateur |
| `hexagonal:port-direction` | 6 | 6 | = | Inchangé : 6 driven ports sans service applicatif |

#### Métriques clés

| Métrique | Step 1 | Step 2 | Delta | Observation |
|----------|-------:|-------:|:-----:|-------------|
| Domain coverage | 80,00% | 71,43% | -8,57 | Moins de types dans le périmètre |
| Code boilerplate ratio | 81,58% | **100,00%** | **+18,42** | Sans services, le modèle est 100% boilerplate |
| Domain purity | 58,33% | 40,00% | -18,33 | Les 9 types impurs pèsent plus dans un périmètre réduit |
| Aggregate boundary | 0,00% | 0,00% | = | Toujours pas d'agrégats détectés |
| Code complexity | 1,11 | 1,00 | -0,11 | OK |

#### Classification

| | Step 1 | Step 2 | Delta |
|---|------:|------:|:-----:|
| Types parsés | 50 | 50 | = |
| Types classifiés | 30 | 21 | -9 |
| Taux de classification | 60,0% | **52,5%** | **-7,5** |
| Types domaine | 24 | 15 | -9 |
| Ports | 6 | 6 | = |
| Conflits | 0 | 0 | = |

Le taux de classification baisse car les 9 services exclus ne sont plus classifiés.
10 types UNKNOWN nécessitent toujours attention.

#### Plan de remédiation

| | Manuel | Avec HexaGlue | Économies |
|---|-------:|-------:|-------:|
| **Effort** | 37,5 jours | 19,5 jours | **18,0 jours** |
| **Coût** | 18 750 EUR | 9 750 EUR | **9 000 EUR** |

| Action | Sévérité | Manuel | HexaGlue | Plugin |
|--------|----------|-------:|-------:|:------:|
| Corriger les 18 violations DDD (pureté + identité) | CRITICAL | 18,0j | 18,0j | -- |
| Créer les adaptateurs pour les 6 driven ports | MAJOR | ~~18,0j~~ | **0j** | `jpa` |
| Corriger la direction des ports | MAJOR | 1,5j | 1,5j | -- |
| **TOTAL** | | 37,5j | **19,5j** | |

> **Fait nouveau** : pour la première fois, le plan de remédiation montre des économies
> potentielles avec HexaGlue. Les 6 violations `hexagonal:port-coverage` (ports sans
> adaptateur) peuvent être résolues automatiquement par le plugin JPA, économisant
> 18 jours (3 jours × 6 ports).

Les violations `port-coverage` incluent désormais des suggestions détaillées :
- 5 étapes (créer JPA entity, mapper, adapter, convertisseurs d'identité, enregistrer le bean)
- Exemple de code (`JpaOrderRepository implements OrderRepository`)
- Effort estimé par port : 3 jours

#### Analyse de stabilité des packages

Inchangée par rapport à l'étape 1 (même structure de packages, seule la classification change).

### Observations

1. **Score +6 points** (15 → 21) : amélioration modeste. Les exclusions réduisent le bruit mais ne résolvent pas les problèmes structurels. Le détail des contributions montre que le gain vient de Hexagonal (10,0 vs 2,5) tandis que Coupling baisse (3,2 vs 4,5).
2. **Hexagonal +30 points** (10% → 40%) : c'est le gain principal. Les 12 violations `layer-isolation` disparaissent car les services (mal classifiés DOMAIN) ne sont plus dans le périmètre. En contrepartie, 6 violations `port-coverage` apparaissent : les repositories n'ont pas d'adaptateur. Les 6 violations `port-direction` persistent (driven ports sans service applicatif).
3. **Swap de contraintes hexagonales** : on passe de "domaine dépend des ports" (faux positif) à "ports sans adaptateur" (problème réel). Le rapport est plus pertinent.
4. **100% boilerplate** : sans les services, il ne reste que les entités JPA -- des data holders purs avec getters/setters. Le modèle anémique est exposé de manière flagrante.
5. **Domain purity en baisse** (58% → 40%) : paradoxe des exclusions. Avec moins de types dans le périmètre, les 9 classes impures (imports JPA) pèsent proportionnellement plus. La métrique reflète correctement l'état du code restant.
6. **3 DTOs records persistent** comme VALUE_OBJECT : structurellement corrects (records sans identité). Pas gênant à cette étape.
7. **Classification toujours plate** : tous les types domaine restent ENTITY à cause de @Entity JPA. HexaGlue ne distingue pas agrégats, entités enfants et value objects. Taux de classification : 52,5% (21/40).
8. **Remédiation actionnable** : le plan de remédiation distingue 3 actions. L'action "créer les adaptateurs" (18 jours) est automatisable par le plugin JPA -- c'est la première apparition d'économies potentielles (48% du total). Les violations `port-coverage` incluent désormais un exemple de code et des étapes détaillées. L'action "corriger la direction des ports" (1,5j) est nouvelle par rapport à l'étape 1.
9. **Conclusion** : les exclusions nettoient le bruit de classification et améliorent la pertinence du rapport hexagonal. Le plan de remédiation montre déjà un ROI potentiel de 48% avec HexaGlue. Mais le problème fondamental reste : le domaine est couplé à JPA. Il faut réorganiser en architecture hexagonale (étape 3) et purifier le domaine (étape 4).

---

## Étape 3 : Restructuration hexagonale (`step/3-hexagonal`)

### Description

Réorganisation complète des packages en architecture hexagonale.
Création des ports (driving/driven), services applicatifs, adapters infrastructure.
Les annotations JPA restent sur les classes domaine. hexaglue.yaml : seulement des exclusions.

### Modifications

- **15 port interfaces créées** : 7 driving (ports/in/) + 8 driven (ports/out/)
- **7 application services** : implémentent les driving ports, dépendent des driven ports
- **6 Spring Data repos dual-interface** : JpaXxxRepository extends JpaRepository + XxxRepository port
- **2 adapters infrastructure** : PaymentGatewayAdapter, NotificationAdapter
- **13 classes domaine** déplacées vers domain/ (par sous-domaine)
- **5 controllers** déplacés vers infrastructure/web/
- **7 DTOs** déplacés vers infrastructure/web/dto/
- **Configs, utils, event** déplacés vers infrastructure/
- `hexaglue.yaml` mis à jour : exclusion infrastructure + application

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

### Résultats

```
mvn clean compile     → BUILD SUCCESS (65 source files)
mvn hexaglue:audit    → BUILD SUCCESS (audit FAILED attendu : 18 violations critiques)
```

#### Verdict

| | Step 1 | Step 2 | Step 3 | Delta vs 2 |
|---|:------:|:------:|:------:|:----------:|
| **Score** | 15/100 | 21/100 | **40/100** | **+19** |
| **Grade** | F | F | **F** | = |
| **Violations** | 37 | 30 | **18** | **-12** |
| CRITICAL | 19 | 18 | 18 | = |
| MAJOR | 18 | 12 | **0** | **-12** |

#### KPIs (progression)

| Dimension | Poids | Step 1 | Step 2 | Step 3 | Delta vs 2 | Contribution |
|-----------|:-----:|-------:|-------:|-------:|:----------:|-------------:|
| DDD Compliance | 25% | 0% | 0% | 0% | = | 0,0 |
| Hexagonal Architecture | 25% | 10% | 40% | **100%** | **+60** | **25,0** |
| Dependencies | 20% | 0% | 0% | 0% | = | 0,0 |
| Coupling | 15% | 30% | 21% | **37%** | **+16** | 5,6 |
| Cohesion | 15% | 58% | 58% | **64%** | **+6** | 9,6 |
| **TOTAL** | **100%** | | | **40/100** | **+19** | **40,2** |

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
| Types parsés | 50 | 40 | **33** |
| Types classifiés | 30 | 21 | **27** |
| Taux | 60,0% | 52,5% | **81,8%** |

Répartition : 12 types domaine (9 entities, 3 value objects) + 15 ports (6 driving, 9 driven). Les 6 types non classifiés sont hors périmètre DDD : ShopApplication, BaseEntity, 3 exceptions, GlobalExceptionHandler.

#### Ports détectés (15)

**6 driving ports** (ports/in/) :

| Port | Méthodes |
|------|:--------:|
| OrderUseCases | 6 |
| CustomerUseCases | 4 |
| ProductUseCases | 7 |
| CatalogUseCases | 2 |
| PaymentUseCases | 2 |
| ShippingUseCases | 3 |

**9 driven ports** (ports/out/) -- tous avec adaptateur :

| Port | Type | Méthodes | Adaptateur |
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

> **Note** : les application services (`com.acme.shop.application.**`) sont exclus de la **classification** par `hexaglue.yaml`, mais HexaGlue les détecte comme **adaptateurs** des driving ports car ils implémentent les interfaces de `ports/in/`. L'exclusion empêche l'attribution d'un ArchType (ex. APPLICATION_SERVICE), pas la détection de la relation structurelle port → implémenteur. C'est le comportement attendu : l'adapter detection fonctionne par analyse d'implémentation, indépendamment du pipeline de classification.

#### 18 violations (18 CRITICAL, 0 MAJOR)

| Contrainte | Step 2 | Step 3 | Delta | Observation |
|------------|-------:|-------:|:-----:|-------------|
| `ddd:entity-identity` | 9 | 9 | = | Mêmes 9 entités sans identité typé |
| `ddd:domain-purity` | 9 | 9 | = | 9 classes model/ avec imports JPA |
| `hexagonal:port-coverage` | 6 | **0** | **-6** | Tous les ports ont un adaptateur (dual-interface) |
| `hexagonal:port-direction` | 6 | **0** | **-6** | Tous les driven ports sont utilisés par un adaptateur |

#### Métriques clés

| Métrique | Step 2 | Step 3 | Delta | Observation |
|----------|-------:|-------:|:-----:|-------------|
| Domain coverage | 71,43% | 44,44% | -26,99 | Périmètre élargi avec les ports |
| Code boilerplate ratio | 100,00% | 100,00% | = | Modèle toujours anémique |
| Domain purity | 40,00% | 25,00% | -15,00 | Ports purs diluent la proportion |
| Aggregate boundary | 0,00% | 0,00% | = | Toujours pas d'agrégats |
| Code complexity | 1,00 | 1,00 | = | OK |

#### Plan de remédiation

| | Manuel | Avec HexaGlue | Économies |
|---|-------:|-------:|-------:|
| **Effort** | 18,0 jours | 18,0 jours | 0,0 jours |
| **Coût** | 9 000 EUR | 9 000 EUR | 0 EUR |

1 seule action : "purifier le domaine" (supprimer les annotations JPA + ajouter les identifiants typés). Pas d'économies HexaGlue à cette étape car les violations restantes sont purement DDD (manuelles). Le gain de 48% lié aux adaptateurs (étape 2) a disparu car les violations `port-coverage` sont résolues.

Effort total en baisse : 37,5 jours (étape 2) → 18 jours (étape 3). La restructuration hexagonale a divisé l'effort de remédiation par 2.

#### Stabilité des packages

La restructuration hexagonale transforme la topologie des packages. Les métriques de Martin révèlent un layout conforme :

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
- **ports.in et ports.out** sur la Main Sequence (A=1,00) : interfaces pures, forte dépendance entrante, conforme au rôle de contrat
- **application** (I=1,00, A=0,00) : maximum instable, aucune dépendance entrante -- conforme au rôle d'implémentation consommable
- **domain.product** en Zone of Pain (D=0,90) : package concret très stable, difficile à modifier car très référencé (Ca=9)

#### Rapports générés

- `AUDIT-REPORT.md` : rapport complet avec diagrammes Mermaid (radar, C4 System Context, C4 Component, domain model, ports, stability quadrant)
- `audit-report.json` : données structurées (verdict, architecture, issues, remediation, appendix avec package metrics)
- `audit-report.html` : dashboard interactif

### Observations

1. **Score +19 points** (21 → 40) : la restructuration hexagonale apporte un gain très significatif. Le détail des contributions montre que le gain vient principalement de Hexagonal (25,0 vs 10,0) et Coupling (5,6 vs 3,2). Les axes DDD et Dependencies restent à 0.
2. **Hexagonal 100%** : c'est le fait marquant. Les 15 ports sont détectés, tous les driven ports ont un adaptateur (dual-interface JpaXxxRepository). Les 6 violations `port-coverage` et les 6 violations `port-direction` de l'étape 2 disparaissent.
3. **0 violations MAJOR** : il ne reste que les 18 CRITICAL -- toutes liées au domaine (identité + pureté). L'architecture hexagonale est complètement validée.
4. **15 adaptateurs détectés malgré les exclusions** : 6 driving (application services) + 9 driven (infrastructure). Les application services sont exclus de la classification (`application.**` dans `hexaglue.yaml`) mais détectés comme adaptateurs car ils implémentent les interfaces de `ports/in/`. HexaGlue distingue classification (attribution d'un ArchType) et détection d'adaptateurs (analyse d'implémentation). Le rapport C4 Component les représente avec les ports et les relations d'implémentation.
5. **InventoryUseCases misclassifié** : cette interface dans `ports/in/` est détectée comme driven port de type REPOSITORY. Misclassification mineure liée à la présence de méthodes CRUD dans le contrat.
6. **3 Value Objects** (vs 6) : les 3 DTOs records déplacés vers `infrastructure/web/dto/` sont exclus. Il reste les 3 enums (Category, OrderStatus, PaymentStatus).
7. **Taux de classification en hausse** (52,5% → 81,8%) : la restructuration hexagonale aide la classification. Les ports sont identifiés grâce au package naming (`ports/in/`, `ports/out/`). Les 6 types non classifiés sont ShopApplication, BaseEntity, exceptions.
8. **Remédiation en baisse** : 37,5 jours / 18 750 EUR (étape 2) → 18 jours / 9 000 EUR. La résolution des violations `port-coverage` et `port-direction` divise l'effort par 2. Plus d'économies HexaGlue car les violations restantes (DDD) sont manuelles.
9. **Package stability conforme** : les ports (A=1,00) sont sur la Main Sequence, l'application et l'infrastructure (I=1,00) sont instables comme attendu. Le domaine concret (A=0,00) est en zone limitrophe (Distance 0,60-0,90).
10. **Conclusion** : l'architecture hexagonale est complètement en place. Le prochain levier est la purification du domaine (étape 4) : supprimer les annotations JPA et ajouter des identifiants typés pour résoudre les 18 violations CRITICAL restantes.

---

## Étape 4 : Purification du domaine (`step/4-pure-domain`)

### Description

Domaine pur sans annotations JPA. Value objects (records). Identifiants typés (records wrappant Long).
Logique métier dans les agrégats. Références inter-agrégats par ID uniquement.
Pas d'infrastructure manuelle -- l'application compile mais ne peut pas démarrer (pas de persistence).

**Changement majeur** : les services applicatifs (`com.acme.shop.application.**`) ne sont plus exclus
de la classification. HexaGlue les détecte désormais comme `APPLICATION_SERVICE` grâce au
`CoreAppClassDetector` qui identifie les classes implémentant les ports driving.

### Modifications

- **6 identifiants typés** (records) : `OrderId`, `CustomerId`, `ProductId`, `PaymentId`, `ShipmentId`, `InventoryId` -- wrappent `Long`
- **4 value objects** (records) : `Money(BigDecimal, String)`, `Address(String, String, String, String)`, `Email(String)`, `Quantity(int)`
- **1 domain event** (record) : `OrderPlacedEvent(OrderId, CustomerId, Money, Instant)`
- **9 classes domaine purifiées** : suppression de toutes les annotations JPA, suppression de `extends BaseEntity`, remplacement des setters par des méthodes métier
- **`ShippingRate` transformé en record** (value object)
- **`BaseEntity` supprimée** (plus nécessaire)
- **6 JPA repositories supprimés** (plus d'@Entity = plus de Spring Data direct)
- **`OrderCreatedEvent` Spring supprimé** (remplacé par `OrderPlacedEvent` domaine)
- **15 port interfaces mises à jour** avec identifiants typés
- **7 application services refactorés** : utilisent les factory methods et méthodes métier
- **5 controllers adaptés** : mapping DTO <-> types domaine
- **2 adapters infrastructure mis à jour** : `PaymentGatewayAdapter`, `NotificationAdapter`
- **`hexaglue.yaml`** : ajout exclusion `com.acme.shop.exception.**`, suppression de l'exclusion `com.acme.shop.application.**`

### Structure des packages (domaine purifié)

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
│   └── out/          (8 driven ports avec identifiants typés)
├── application/      (7 services -- classifiés APPLICATION_SERVICE)
├── exception/        (4 classes -- exclus de HexaGlue)
└── infrastructure/   (web, external -- exclus de HexaGlue)
```

### Résultats

```
mvn clean compile     → BUILD SUCCESS (68 source files)
mvn hexaglue:audit    → BUILD SUCCESS (audit PASSED with warnings : 7 violations MAJOR)
```

#### Verdict

| | Step 1 | Step 2 | Step 3 | Step 4 | Delta vs 3 |
|---|:------:|:------:|:------:|:------:|:----------:|
| **Score** | 15/100 | 21/100 | 40/100 | **56/100** | **+16** |
| **Grade** | F | F | F | **F** | = |
| **Status** | FAILED | FAILED | FAILED | **PASSED_WITH_WARNINGS** | **↑** |
| **Violations** | 37 | 30 | 18 | **7** | **-11** |
| CRITICAL | 19 | 18 | 18 | **0** | **-18** |
| MAJOR | 18 | 12 | 0 | **7** | **+7** |

#### KPIs (progression)

| Dimension | Poids | Step 1 | Step 2 | Step 3 | Step 4 | Delta vs 3 | Contribution |
|-----------|:-----:|-------:|-------:|-------:|-------:|:----------:|-------------:|
| DDD Compliance | 25% | 0% | 0% | 0% | **100%** | **+100** | **25,0** |
| Hexagonal Architecture | 25% | 10% | 40% | 100% | **65%** | **-35** | 16,3 |
| Dependencies | 20% | 0% | 0% | 0% | 0% | = | 0,0 |
| Coupling | 15% | 30% | 21% | 37% | **31%** | -6 | 4,7 |
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
| Driving Ports | 6 | 6 | = | Inchangés |
| Driven Ports | 9 | 9 | = | 6 sans adaptateur (JPA repos supprimés) |
| Application Services | 0 | **7** | **+7** | Classifiés APPLICATION_SERVICE |

#### Classification

| | Step 1 | Step 2 | Step 3 | Step 4 |
|---|-------:|-------:|-------:|-------:|
| Types en périmètre | 50 | 40 | 33 | **46** |
| Types classifiés | 30 | 21 | 27 | **44** |
| Taux | 60,0% | 52,5% | 81,8% | **95,7%** |

Répartition : 22 types domaine (6 AR, 2 entities, 7 VOs, 6 identifiers, 1 event) + 15 ports (6 driving, 9 driven) + **7 application services**. Seuls 2 types non classifiés : ShopApplication et ShippingRate (UNKNOWN).

> **Changement vs étapes précédentes** : les 7 application services sont désormais dans le périmètre de classification. Ils étaient auparavant exclus par `com.acme.shop.application.**` dans `hexaglue.yaml`. Le nombre de types en périmètre passe de 39 à 46 (+7), le taux de classification monte à 95,7% (44/46).

#### Couche applicative (7 APPLICATION_SERVICE)

Le rapport inclut désormais un diagramme **Application Layer** :

| Application Service | Méthodes | Port implémenté |
|---------------------|:--------:|-----------------|
| CustomerApplicationService | 4 | CustomerUseCases |
| CatalogApplicationService | 2 | CatalogUseCases |
| OrderApplicationService | 7 | OrderUseCases |
| ProductApplicationService | 8 | ProductUseCases |
| PaymentApplicationService | 2 | PaymentUseCases |
| ShippingApplicationService | 3 | ShippingUseCases |
| InventoryApplicationService | 6 | InventoryUseCases |

#### Adaptateurs (9)

> Les 6 JPA repos dual-interface ont été supprimés (le domaine pur n'a plus @Entity). Il ne reste que 9 adaptateurs.

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

> **6 driven ports sans adaptateur** : ShipmentRepository, InventoryRepository, CustomerRepository, ProductRepository, OrderRepository, PaymentRepository. C'est volontaire -- HexaGlue les générera à l'étape 5.

#### 7 violations (0 CRITICAL, 7 MAJOR)

| Contrainte | Step 3 | Step 4 | Delta | Observation |
|------------|-------:|-------:|:-----:|-------------|
| `ddd:entity-identity` | 9 | **0** | **-9** | Identifiants typés créés |
| `ddd:domain-purity` | 9 | **0** | **-9** | Annotations JPA supprimées |
| `hexagonal:application-purity` | 0 | **7** | **+7** | 7 services avec imports Spring interdits |

> **18 violations CRITICAL résolues** : les violations `ddd:entity-identity` et `ddd:domain-purity` de l'étape 3 sont toutes résolues par la purification du domaine. En revanche, 7 nouvelles violations MAJOR apparaissent via la contrainte `hexagonal:application-purity` : chacun des 7 services applicatifs importe `org.springframework.stereotype.Service` et `org.springframework.transaction.annotation.Transactional`, détectés comme des imports d'infrastructure interdits dans la couche application. C'est le nouveau plugin qui introduit cette contrainte -- l'ancienne version tolérait ces annotations dans la couche application.
>
> Les 6 driven ports sans adaptateur infrastructure (repos JPA supprimés) ne génèrent pas de violation `hexagonal:port-coverage` -- ils seront générés à l'étape 5. Les driving ports sont correctement reconnus comme implémentés par les APPLICATION_SERVICE (pas de faux positif `hexagonal:port-direction`).

#### Métriques clés

| Métrique | Step 3 | Step 4 | Delta | Observation |
|----------|-------:|-------:|:-----:|-------------|
| Domain purity | 25,00% | **100,00%** | **+75** | Zéro import d'infrastructure dans le domaine |
| Aggregate boundary | 0,00% | **100,00%** | **+100** | Entités accessibles uniquement via l'agrégat |
| Code boilerplate ratio | 100,00% | **80,20%** | **-19,80** | Logique métier ajoutée (place, cancel, etc.) |
| Aggregate avg size | 0 | **14,33** | **+14,33** | Les agrégats ont de vraies méthodes métier |
| Domain coverage | 44,44% | **50,00%** | **+5,56** | Plus de types domaine dans le périmètre |
| Classification rate | 81,8% | **95,7%** | **+13,9** | 44/46 types classifiés |

#### Plan de remédiation

| | Manuel | Avec HexaGlue | Économies |
|---|-------:|-------:|-------:|
| **Effort** | 7,0 jours | 7,0 jours | 0,0 jours |
| **Coût** | 3 500 EUR | 3 500 EUR | 0 EUR |

| Action | Sévérité | Effort | Violations résolues |
|--------|----------|-------:|--------------------:|
| Supprimer les imports Spring des services applicatifs | MAJOR | 7,0j | 7 |

> Les 7 violations `hexagonal:application-purity` nécessitent de retirer `@Service` et `@Transactional` des services applicatifs. Cela implique un mécanisme alternatif d'enregistrement des beans Spring et de gestion transactionnelle (ex : configuration Java `@Bean`, ou AOP déclaratif). Pas d'économies HexaGlue car ces violations sont purement manuelles.

#### Stabilité des packages

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
- **domain.order en Zone of Pain** (D=0,92, Ca=23) : package domaine le plus référencé du projet (Money, Address, OrderId, Quantity utilisés partout). Difficile à modifier sans impact. Signe d'un sous-domaine central qui mériterait potentiellement un découpage.
- **ports.out** (I=0,65 vs 0,38 à l'étape 3) : instabilité en hausse car les ports réfèrent maintenant les types domaine enrichis (identifiants typés, value objects).
- **application** (Ce=28 vs 26) : légère hausse des dépendances sortantes, les services utilisent les nouveaux types domaine.

#### Rapports générés

- `AUDIT-REPORT.md` : rapport avec diagrammes Mermaid (radar, C4, **application layer**, domain model avec identifiants et value objects, ports avec couverture, stability quadrant)
- `audit-report.json` : données structurées (verdict, architecture avec agrégats enrichis, issues, remédiation)
- `audit-report.html` : dashboard interactif

#### Classification par rôle DDD (22 types domaine, 100% INFERRED)

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

1. **PASSED_WITH_WARNINGS (+16 points)** : le score passe de 40 à 56, le status de FAILED à PASSED_WITH_WARNINGS. Les 18 violations CRITICAL de l'étape 3 sont **toutes résolues**, mais 7 nouvelles violations MAJOR apparaissent. Le grade reste F car le score est sous 60.
2. **DDD Compliance 100%** : le fait marquant. HexaGlue infère correctement les 22 types domaine : 6 agrégats, 6 identifiants, 2 entités, 7 value objects, 1 domain event. **Aucune classification explicite nécessaire** -- tout est inféré par structure (CERTAIN_BY_STRUCTURE) ou convention de nommage (INFERRED pour OrderPlacedEvent).
3. **7 APPLICATION_SERVICE classifiés** : les services applicatifs sont désormais dans le périmètre de classification. Le rapport inclut un diagramme "Application Layer" montrant les 7 services avec leurs méthodes. Cela porte le taux de classification à 95,7% (44/46).
4. **Domain purity 100%** : plus aucun import JPA dans le domaine. Les 9 violations `ddd:domain-purity` et les 9 violations `ddd:entity-identity` sont éliminées d'un coup.
5. **Aggregate boundary 100%** : les entités (OrderLine, StockMovement) sont accessibles uniquement via leur agrégat racine.
6. **Boilerplate 80%** (vs 100%) : les méthodes métier (Order.place(), cancel(), markPaid(), Inventory.reserve(), etc.) ajoutent du vrai code domaine. aggregate.avgSize passe de 0 à 14,33 méthodes.
7. **Hexagonal Architecture 65%** (vs 100% à l'étape 3) : c'est la régression principale. La nouvelle contrainte `hexagonal:application-purity` pénalise le score hexagonal car les 7 services applicatifs importent des annotations Spring. À l'étape 3, les services étaient exclus du périmètre, donc cette contrainte ne s'appliquait pas. Classifier plus de types expose des problèmes de pureté architecturale.
8. **Nouvelle contrainte `hexagonal:application-purity`** : le nouveau plugin vérifie que les types `APPLICATION_SERVICE` n'importent pas de dépendances d'infrastructure. Les 2 imports incriminés sont `org.springframework.stereotype.Service` et `org.springframework.transaction.annotation.Transactional`. C'est un changement de philosophie : l'ancienne version du plugin tolérait ces annotations dans la couche application ; la nouvelle les considère comme des violations de la pureté hexagonale.
9. **Plus de faux positif `hexagonal:port-direction`** : le nouveau plugin détecte correctement les APPLICATION_SERVICE comme implémenteurs valides des driving ports. Les 6 faux positifs qui existaient dans l'ancienne version sont éliminés.
10. **Plus de `hexagonal:layer-isolation`** : l'ancienne version signalait des violations quand les services dépendaient de types UNCLASSIFIED (InventoryUseCases CONFLICTING). Le nouveau plugin ne produit plus ces violations. InventoryUseCases est toujours détecté comme driven port de type REPOSITORY (en raison de ses méthodes CRUD), mais cela ne génère plus de conflit bloquant.
11. **Classification rate 95,7%** (vs 81,8%) : seuls ShopApplication et ShippingRate restent non classifiés (2/46). ShippingRate est un cas intéressant : c'est un record avec un champ Money, mais il n'est pas détecté comme VALUE_OBJECT (probablement car il n'est pas embarqué dans un agrégat).
12. **domain.order en Zone of Pain** (D=0,92, Ca=23) : le sous-domaine order concentre Money, Address, OrderId, Quantity -- des types utilisés par tout le projet. Cette centralité rend le package fragile aux modifications.
13. **Remédiation : 7 jours / 3 500 EUR** : les 7 violations `application-purity` sont toutes manuelles. La correction implique de remplacer `@Service` par une déclaration `@Bean` dans une classe `@Configuration`, et `@Transactional` par un mécanisme AOP déclaratif ou une configuration XML. C'est un effort réel mais optionnel à ce stade -- l'audit passe (PASSED_WITH_WARNINGS).
14. **Conclusion** : le domaine est prêt pour la génération. Toutes les informations DDD sont inférées, la pureté du domaine est totale. Les 7 violations `application-purity` pointent un problème de pureté de la couche application (annotations Spring) qui ne bloque pas la génération. Le plugin JPA peut générer les 6 adaptateurs manquants automatiquement. L'étape 5 activera la génération pour combler les driven ports sans adaptateur.

---

## Étape 5 : Génération et audit (`step/5-generated`)

### Description

Activation des plugins HexaGlue (JPA, living-doc, audit) avec `extensions=true`.
Toute l'infrastructure de persistence est générée automatiquement : JPA entities, embeddables,
Spring Data repositories, MapStruct mappers, et driven port adapters.
Les services applicatifs sont purifiés (suppression des annotations Spring) et enregistrés
via une classe `@Configuration` dans la couche infrastructure.

### Modifications

- **`pom.xml`** : ajout `<extensions>true</extensions>`, `<failOnError>false</failOnError>`,
  et 3 plugins en dependencies (hexaglue-plugin-jpa, hexaglue-plugin-living-doc, hexaglue-plugin-audit)
- **`hexaglue.yaml`** : ajout section `plugins:` avec clés fully-qualified
  (`io.hexaglue.plugin.jpa`, `io.hexaglue.plugin.livingdoc`, `io.hexaglue.plugin.audit.ddd`)
  et `enableAuditing: false` pour éviter les champs d'audit dupliqués.
  L'exclusion `com.acme.shop.application.**` reste supprimée (comme à l'étape 4).
- **7 services applicatifs purifiés** : suppression de `@Service`, `@Transactional` et
  `@Transactional(readOnly = true)` (annotations de classe et de méthode) + suppression des
  imports `org.springframework.stereotype.Service` et `org.springframework.transaction.annotation.Transactional`.
  Les services sont désormais des POJOs purs sans dépendance Spring.
- **`ApplicationServiceConfig.java`** (nouveau) : classe `@Configuration` dans
  `infrastructure/config/` qui déclare les 7 services applicatifs comme beans Spring
  via des méthodes `@Bean`, avec injection des ports driven par constructeur.

### Code généré (29 fichiers par HexaGlue + 6 MapStruct impl)

| Type | Fichiers | Exemples |
|------|----------|----------|
| JPA Entities | 8 | `CustomerJpaEntity`, `OrderJpaEntity`, `OrderLineJpaEntity`, `StockMovementJpaEntity` |
| Embeddables | 2 | `MoneyEmbeddable`, `AddressEmbeddable` |
| Spring Data Repos | 6 | `CustomerJpaRepository`, `OrderJpaRepository` |
| MapStruct Mappers | 6 (+6 impl) | `CustomerMapper` -> `CustomerMapperImpl` |
| Port Adapters | 7 | `CustomerRepositoryAdapter`, `OrderRepositoryAdapter` |

### Résultats

```
mvn clean compile       → BUILD SUCCESS (98 source files : 69 manuels + 29 générés)
mvn clean verify        → BUILD SUCCESS
```

#### Audit d'architecture

**Verdict** :

| Score | Grade | Status |
|-------|-------|--------|
| **60/100** | **D** | **PASSED_WITH_WARNINGS** |

**KPIs** :

| KPI | Valeur | Seuil | Status | Delta vs step 4 |
|-----|--------|-------|--------|-----------------|
| DDD Compliance | 95% | 90% | OK | -5% (100% → 95%) |
| Hexagonal Architecture | 90% | 90% | OK | +25% (65% → 90%) |
| Dependencies | 0% | 80% | CRITICAL | = |
| Coupling | 31% | 70% | CRITICAL | = |
| Cohesion | 63% | 80% | CRITICAL | -8% (71% → 63%) |

**Progression du score** :

| | Step 1 | Step 2 | Step 3 | Step 4 | **Step 5** |
|--|--------|--------|--------|--------|-----------|
| Score | 15/100 | 21/100 | 40/100 | 56/100 | **60/100** |
| Grade | F | F | F | F | **D** |
| Status | FAILED | FAILED | FAILED | PASSED | **PASSED** |
| Violations | 37 | 30 | 18 | 7 | **3** |

> **Progression du score** : 56 → 60 (+4 points). La suppression des annotations Spring dans les services applicatifs élimine les 7 violations `hexagonal:application-purity` de l'étape 4. Hexagonal Architecture remonte de 65% à 90%. En contrepartie, l'analyse élargie aux fichiers générés fait apparaître 2 violations `hexagonal:layer-isolation` et 1 violation `ddd:aggregate-boundary`. Le grade passe de F à D.

**Inventaire architectural** (43 types classifiés sur 81 types totaux, taux 93,5%) :

| Element | Nombre |
|---------|--------|
| Aggregates | 6 |
| Entities | 2 |
| Value Objects | 7 |
| Identifiers | 6 |
| Domain Events | 1 |
| Driving Ports | 6 |
| Driven Ports | 8 |
| Application Services | 7 |

> 104 types analysés (incluant les fichiers générés), 81 types dans le périmètre de classification,
> 43 classifiés (93,5%). 3 types non classifiés : InventoryUseCases (CONFLICTING), ShopApplication
> et ShippingRate (UNKNOWN).

**Violations** : 3 (0 CRITICAL, 3 MAJOR)

| Contrainte | Step 4 | Step 5 | Delta | Observation |
|------------|-------:|-------:|:-----:|-------------|
| `hexagonal:application-purity` | 7 | **0** | **-7** | Annotations Spring supprimées |
| `hexagonal:layer-isolation` | 0 | **2** | **+2** | OrderApplicationService et ShippingApplicationService dépendent de InventoryUseCases (UNCLASSIFIED) |
| `ddd:aggregate-boundary` | 0 | **1** | **+1** | OrderLine accessible hors de son agrégat Product |

> **Disparition des 7 violations `application-purity`** : la suppression de `@Service` et `@Transactional` des 7 services applicatifs élimine toutes les violations de pureté. Les services sont désormais des POJOs purs enregistrés via `@Configuration`.
>
> **`hexagonal:layer-isolation`** : `InventoryUseCases` est CONFLICTING car elle est implémentée par un `CoreAppClass` (driving) ET détectée comme driven port (REPOSITORY). La conséquence : `OrderApplicationService` et `ShippingApplicationService` qui l'injectent sont en violation d'isolation de couche.
>
> **`ddd:aggregate-boundary`** : `OrderLine` est détecté comme accessible hors de son agrégat `Product` (référence croisée inter-agrégats). L'audit au périmètre élargi (104 types incluant les générés) détecte cette violation qui n'était pas visible à l'étape 4 (68 types seulement).

**Couverture des ports** :

| Port | Type | Adapter | Source |
|------|------|---------|--------|
| OrderUseCases | DRIVING | OrderApplicationService | manuel |
| CustomerUseCases | DRIVING | CustomerApplicationService | manuel |
| ProductUseCases | DRIVING | ProductApplicationService | manuel |
| CatalogUseCases | DRIVING | CatalogApplicationService | manuel |
| PaymentUseCases | DRIVING | PaymentApplicationService | manuel |
| ShippingUseCases | DRIVING | ShippingApplicationService | manuel |
| OrderRepository | DRIVEN (REPOSITORY) | OrderRepositoryAdapter | **généré** |
| CustomerRepository | DRIVEN (REPOSITORY) | CustomerRepositoryAdapter | **généré** |
| ProductRepository | DRIVEN (REPOSITORY) | ProductRepositoryAdapter | **généré** |
| InventoryRepository | DRIVEN (REPOSITORY) | InventoryRepositoryAdapter | **généré** |
| ShipmentRepository | DRIVEN (REPOSITORY) | ShipmentRepositoryAdapter | **généré** |
| PaymentRepository | DRIVEN (REPOSITORY) | PaymentRepositoryAdapter | **généré** |
| PaymentGateway | DRIVEN (GATEWAY) | PaymentGatewayAdapter | manuel |
| NotificationSender | DRIVEN (EVENT_PUBLISHER) | NotificationAdapter | manuel |

> **14/14 ports couverts** (100%) -- les 6 repository adapters sont générés par le plugin JPA.

**Métriques** :

| Métrique | Valeur | Seuil | Status | Delta vs step 4 |
|----------|--------|-------|--------|-----------------|
| domain.purity | 100% | ≥ 100% | OK | = |
| aggregate.boundary | 0,00% | ≥ 80% | CRITICAL | -100% (100% → 0%) |
| aggregate.repository.coverage | 100% | ≥ 100% | OK | = |
| aggregate.coupling.efferent | 0.41 | ≤ 0.7 | OK | +0.06 |
| code.complexity.average | 1.12 | ≤ 10.0 | OK | = |
| domain.coverage | 51.16% | ≥ 30% | OK | +1.16% |
| aggregate.avgSize | 14.33 | ≤ 20.0 | OK | = |
| code.boilerplate.ratio | 80.20% | ≤ 50% | CRITICAL | = |

> **aggregate.boundary à 0%** : c'est la régression principale dans les métriques. L'analyse au
> périmètre élargi (104 types incluant les générés) détecte que OrderLine est accessible hors de
> son agrégat, ce qui fait chuter la métrique de 100% à 0%. Cette violation n'était pas détectée
> à l'étape 4 car le périmètre ne couvrait que 68 types manuels.

**Rapports générés** :

- `target/hexaglue/reports/audit/audit-report.html` (rapport HTML interactif)
- `target/hexaglue/reports/audit/AUDIT-REPORT.md` (rapport Markdown avec diagramme Application Layer)
- `target/hexaglue/reports/living-doc/README.md` (documentation architecture)
- `target/hexaglue/reports/living-doc/domain.md` (modèle domaine)
- `target/hexaglue/reports/living-doc/ports.md` (ports driving/driven)
- `target/hexaglue/reports/living-doc/diagrams.md` (diagrammes)

**Plan de remédiation** :

| | Effort | Coût |
|---|-------:|-------:|
| **Total** | 2,0 jours | 1 000 EUR |

| Action | Effort | Impact |
|--------|-------:|--------|
| Corriger la frontière d'agrégat (OrderLine) | 1,0d | 1 violation `ddd:aggregate-boundary` |
| Corriger l'isolation de couche | 1,0d | 2 violations `hexagonal:layer-isolation` |

### Observations

1. **29 fichiers générés automatiquement** remplacent les ~31 fichiers d'infrastructure manuelle
   que l'on aurait dû écrire à la main (cf. step 4 de l'ancienne migration)
2. **0 fichier d'infrastructure manuelle** pour la persistence -- tout est généré par le plugin JPA
3. **Score en progression** (56 → 60, grade F → D) : la purification des services applicatifs
   (suppression `@Service` + `@Transactional`) élimine les 7 violations `application-purity`.
   Hexagonal Architecture remonte de 65% à 90%. Le score net progresse malgré 3 nouvelles violations.
4. **Services applicatifs purs** : les 7 services sont désormais des POJOs sans aucune dépendance
   Spring. L'enregistrement des beans est externalisé dans `ApplicationServiceConfig.java`
   (couche infrastructure). C'est la remédiation recommandée par l'audit de l'étape 4.
5. **InventoryUseCases CONFLICTING** : c'est le nœud du problème. Cette interface dans `ports/in/`
   est détectée comme driven port de type REPOSITORY (à cause des méthodes CRUD) ET implémentée par
   un `CoreAppClass` (InventoryApplicationService). Le conflit rend le type UNCLASSIFIED, ce qui
   déclenche les violations `layer-isolation` sur les 2 services qui l'injectent.
6. **aggregate.boundary à 0% (CRITICAL)** : OrderLine est détecté comme accessible hors de son
   agrégat Product. Cette violation n'apparaissait pas à l'étape 4 car le périmètre d'analyse
   était limité aux 68 fichiers manuels. Avec les 104 types (dont les générés), la frontière
   d'agrégat est évaluée plus strictement.
7. **code.boilerplate.ratio 80.20% (CRITICAL)** : le ratio de boilerplate reste élevé car les
   accesseurs getter/setter représentent une large part du code
8. **`enableAuditing: false`** : évite la génération de champs `createdAt`/`updatedAt` dupliqués
   (les classes domaine n'ont pas de champs d'audit, c'est intentionnel)
9. **MapStruct intégré automatiquement** : le lifecycle participant de HexaGlue injecte MapStruct
   quand le plugin JPA est présent + `extensions=true`
10. **Important : `mvn clean verify`** (et non `hexaglue:audit` seul) est nécessaire pour que l'audit
    voie les fichiers générés. Le goal `hexaglue:audit` seul ne voit que les 69 sources manuelles.
11. **Plus de faux positif `hexagonal:port-direction`** : comme à l'étape 4, le nouveau plugin
    détecte correctement les APPLICATION_SERVICE comme implémenteurs valides des driving ports.
12. **Conclusion** : l'infrastructure est entièrement générée et les services applicatifs sont
    architecturalement purs. Il reste 3 violations (2 `layer-isolation` liées à InventoryUseCases
    CONFLICTING, 1 `aggregate-boundary` pour OrderLine). Il reste à vérifier que l'application
    fonctionne de bout en bout (étape 6).

---

## Etape 6 : Application fonctionnelle (`main`)

*A venir*
