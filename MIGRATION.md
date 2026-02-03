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
