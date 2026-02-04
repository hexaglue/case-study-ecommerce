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
