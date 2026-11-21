# Revue de code — phase 2 (base de données et modèle Spring Boot)

## 1. Périmètre

- Migrations Flyway `V1`…`V9` réécrites, RLS activée sur les 15 tables.
- Entités JPA des domaines `user`, `address`, `catalog`, `promotion`, `cart`, `order`,
  `payment`, `delivery` + audit commun.
- 14 repositories, 6 services métier (`InventoryService`, `OrderService`, `CartService`,
  `PromotionService`, `DeliveryZoneService`, `PaymentService`) et le contrat
  `PaymentProvider`.
- DTO de commande, panier et paiement ; exceptions métier.
- Tests H2 et jeu de démonstration séparé (`db/demo`).

Aucun contrôleur REST n'a été ajouté : la phase 2 reste une couche modèle/service.

## 2. Vérifications réellement exécutées

| Commande | Résultat |
|---|---|
| `./mvnw -DskipTests compile` | succès |
| `./mvnw test` | **27 tests, 0 échec, BUILD SUCCESS** |

Détail : 4 catalogue (MockMvc), 4 santé, 3 stockage, 4 entités produit,
4 panier, 5 commande/stock/promotion/annulation, 3 idempotence paiement.

Non exécuté / non vérifiable ici :

- **Les migrations n'ont pas été jouées sur PostgreSQL/Supabase** : `DB_PASSWORD` est
  vide et Docker n'est pas installé. H2 ne sait pas exécuter `ENABLE ROW LEVEL SECURITY`,
  `jsonb`, les index partiels ni les `CHECK` du schéma.
- Le frontend n'a pas été reconstruit : son contrat API (`priceXaf`, `inStock`,
  `categorySlug`, `categoryName`, `featured`) est inchangé et les tests catalogue passent.
- Aucun test de concurrence multi-threads (verrou pessimiste) n'a été écrit : H2 ne
  reproduit pas fidèlement le comportement PostgreSQL.

## 3. Corrections faites pendant la revue

1. **Fuite d'entité détachée** : `PaymentResult` renvoyait `Payment`, ce qui provoquait
   une `LazyInitializationException` hors transaction. Il expose désormais un instantané
   immuable (id, référence, statut, transaction, `duplicate`).
2. **Exception non traduite** : `Payment.markSuccess` lève `IllegalArgumentException`
   lorsque l'identifiant de transaction manque ; le service la convertit en
   `BusinessException` (422) au lieu d'une 500.
3. **Objet SQL inutilisé** : la séquence `order_number_seq` était créée mais le numéro
   est généré côté application ; elle a été retirée.
4. **Traçabilité des frais** : `orders.delivery_zone_id` (FK, NOT NULL) a été ajouté pour
   savoir quelle zone a produit les frais, indépendamment de l'expédition.

## 4. Constats classés

### Bloquants avant mise en production

1. **Migrations non validées sur la cible réelle.** À faire en premier avec le profil
   `dev` sur Supabase, puis `select * from flyway_schema_history`.
2. **Aucun contrôle d'authenticité des notifications de paiement.** `PaymentService`
   suppose une notification déjà vérifiée ; `PaymentProvider.verifySuccess` n'a pas
   d'implémentation et aucun endpoint ne doit être exposé avant la phase paiement.
3. **Doublons concurrents de webhooks.** Un rejeu séquentiel est idempotent (testé), mais
   deux transactions simultanées avec le même `event_id` se heurtent à la contrainte
   unique et remontent une erreur non traduite. Prévoir la capture de
   `DataIntegrityViolationException` et une réponse 200/409.
4. **Commandes non rattachées à un utilisateur.** `OrderService` passe `user = null` :
   l'authentification n'existe pas encore. `history(userId)` ne retournera donc rien tant
   que la phase auth n'alimente pas ce champ.

### Importants

5. **Vérification de montant partielle** : `PaymentService` compare le montant du
   paiement au total actuel de la commande, mais l'API prestataire devra aussi confirmer
   devise, référence et transaction. À couvrir dans l'implémentation `PaymentProvider`.
6. **N+1** dans `OrderService.history` (une requête de lignes par commande) et accès
   paresseux au produit dans `OrderResponse.from`. Acceptable à cette échelle ; à
   corriger par `@EntityGraph` / projection si l'historique devient fréquent.
7. **Pas de service d'adresses ni d'expéditions** : les entités et tables existent, mais
   les opérations CRUD/suivi relèvent des phases suivantes. Ne pas exposer ces tables
   prématurément.
8. **Numéro de commande aléatoire**, pas séquentiel : format `LC-AAAAMMJJ-XXXXXX`,
   unicité en base et 5 tentatives en cas de collision. La séquence a été retirée pour
   éviter un objet inutilisé ; un numéro strictement séquentiel pourra revenir avec un
   `nextval` PostgreSQL testé.
9. **Panier non réservant** : le contrôle de stock à l'ajout est indicatif (pas de verrou).
   La vérité reste la création de commande sous verrou. C'est le comportement voulu, mais
   l'interface devra afficher les erreurs de stock à la validation.
10. **Aucun setters métier catalogue** (prix, stock) : l'administration devra passer par
    des méthodes dédiées avec validation et verrou optimiste, pas par des setters publics.

### Mineurs

11. `created_by` / `updated_by` restent nuls jusqu'à l'authentification (audit partiel).
12. L'index unique partiel `uk_carts_user` n'existe pas sous H2 : l'unicité du panier
    utilisateur est assurée par requête, à confirmer sur PostgreSQL.
13. `product_images` et `cart_items` n'ont pas de limite au nombre de lignes : à cadrer
    côté service lorsque l'administration sera ouverte.
14. Les tests utilisent `TRUNCATE` H2 (`DatabaseCleaner`) : utilitaire de test uniquement,
    sans impact production.
15. `OrderResponse` expose les statuts en `String` (stable pour l'API), alors que le
    domaine utilise des enums.

## 5. Points conformes au cahier des charges

- UUID en clé primaire, `NOT NULL`, `UNIQUE`, FK et index présents.
- Montants XAF en `bigint` ; aucun `FLOAT`/`DOUBLE` ; `NUMERIC` réservé aux remises.
- `TIMESTAMPTZ` partout, audit `created_at`/`updated_at`.
- Prix/stock non négatifs, quantités positives, prix figés dans `order_items`,
  totaux recalculés et cohérence vérifiée en SQL.
- Stock décrémenté dans la transaction de commande, annulation idempotente.
- Webhooks idempotents par contrainte unique + vérification applicative.
- Conservation de l'historique par snapshots et `ON DELETE SET NULL`.
- Audit JPA activé (`@EnableJpaAuditing`), Lombok non utilisé.

## 6. Recommandation

Ne pas passer à la phase 3 (contrôleurs, auth, paiement réel) avant :

1. l'application réussie de `V1`…`V9` sur Supabase et la vérification des contraintes ;
2. une revue du modèle avec les règles RLS souhaitées (aucune politique aujourd'hui) ;
3. la décision sur le rattachement des commandes aux comptes (auth) ;
4. la validation de la stratégie de numérotation des commandes.
