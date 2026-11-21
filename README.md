# TP4 Spring Cloud : Calculatrice (Eureka, Gateway, OpenFeign)

Matière : Architecture MicroServices, MPSRCC1, FSS

## Modules
| Module | Port | Rôle |
|---|---|---|
| eureka-service | 8761 | Annuaire (Eureka Server) |
| calcul-service | 8081 et 8083 | Calculs (2 instances) |
| historique-service | 8082 | Historique, appelle calcul-service par OpenFeign |
| gateway-service | 8888 | Passerelle (Spring Cloud Gateway) |

Ordre de lancement : eureka, calcul (8081 puis 8083), historique, gateway.
calculatrice-spring-cloud/
|-- pom.xml
|-- README.md
|-- eureka-service/
|   |-- pom.xml
|   |-- mvnw
|   |-- mvnw.cmd
|   |-- .mvn/
|   |-- .gitignore
|   |-- .gitattributes
|   |-- HELP.md
|   `-- src/
|       |-- main/
|       |   |-- java/com/exemple/eureka/
|       |   |   `-- EurekaServiceApplication.java
|       |   `-- resources/
|       |       `-- application.properties
|       `-- test/java/com/exemple/eureka/
|           `-- EurekaServiceApplicationTests.java
|-- calcul-service/
|   |-- pom.xml
|   `-- src/
|       `-- main/
|           |-- java/com/exemple/calcul/
|           |   |-- CalculServiceApplication.java
|           |   |-- controller/
|           |   |   `-- CalculController.java
|           |   |-- model/
|           |   |   `-- Resultat.java
|           |   `-- service/
|           |       `-- Calculatrice.java
|           `-- resources/
|               `-- application.properties
|-- historique-service/
|   |-- pom.xml
|   `-- src/
|       `-- main/
|           |-- java/com/exemple/historique/
|           |   |-- HistoriqueServiceApplication.java
|           |   |-- client/
|           |   |   `-- CalculClient.java
|           |   |-- controller/
|           |   |   `-- OperationController.java
|           |   |-- model/
|           |   |   `-- Operation.java
|           |   `-- service/
|           |       `-- OperationService.java
|           `-- resources/
|               `-- application.properties
`-- gateway-service/
    |-- pom.xml
    `-- src/
        `-- main/
            |-- java/com/exemple/gateway/
            |   `-- GatewayServiceApplication.java
            `-- resources/
                `-- application.yml

## Réponses aux questions

### Partie A
**A.4.1.** Le serveur Eureka (annuaire) fait la correspondance entre le nom et les adresses. Elle est mise à jour à chaque inscription d'une instance au démarrage, à chaque battement de cœur (toutes les 5 s ici), à la désinscription à l'arrêt, et par éviction quand une instance ne donne plus de nouvelles.

**A.4.2.** Avantage : le client ne connaît qu'une adresse (port 8888), quel que soit le nombre de microservices, et la passerelle peut centraliser filtres et sécurité. Inconvénient : la passerelle est un point unique de défaillance, et ajoute un saut réseau.

**A.4.3.** Chaque instance reçoit 3 requêtes (alternance 8081, 8083, ...). Le client ne peut pas s'en apercevoir : la réponse est la même, seules les consoles des instances le montrent.

### Partie B
**B.1.** Chaque programme écoute sur un port différent (8761, 8081, 8082...). Deux programmes ne peuvent pas partager un port, mais ils peuvent coexister s'ils utilisent des ports distincts : chaque JVM a son propre Tomcat.

### Partie C
**C.1.** Le nom (`CALCUL-SERVICE`) identifie le service : les appelants demandent ce nom et obtiennent toutes ses instances. L'identifiant (`calcul-service:8081`, `calcul-service:8083`) distingue chaque instance dans l'annuaire, ce qui permet au répartiteur d'en choisir une.

**C.2.** Sans `--server.port=8083`, la 2e instance utiliserait le port 8081, déjà occupé : le démarrage échouerait (port already in use).

### Partie D
**D.1.** Les traces alternent : 8081, 8083, 8081, 8083, 8081, 8083. La règle est le tour de rôle. Cette stratégie s'appelle **round robin**.

**D.2.** Statut **503**, avec `{"erreur":"calcul-service injoignable"}`. La réponse est produite par le second bloc `catch (FeignException e)` : aucune instance de ce nom n'existe dans l'annuaire, ce qui n'est pas une erreur 400 (`FeignException.BadRequest`).

### Partie E
**E.1.** La passerelle ajoute les en-têtes `X-Forwarded-Host` et `X-Forwarded-Port` à la requête. Avec `server.forward-headers-strategy=framework`, `historique-service` s'en sert pour reconstruire l'adresse vue par le client. `ServletUriComponentsBuilder.fromCurrentRequest()` produit donc un `Location` avec le port 8888.

**E.2.** E.3 : `/calcul/somme` ; E.5 : `/calcul-service/calcul/somme`. E.3 donne des URI propres et un contrôle fin (prédicats, filtres), mais oblige à modifier la passerelle quand on ajoute un microservice. E.5 est automatique (une route par service de l'annuaire), mais le nom du service apparaît dans l'URI.

**E.3.** Cette route existe car la passerelle s'inscrit elle-même dans Eureka (`GATEWAY-SERVICE`) : le locator crée une route par service de l'annuaire. Ce n'est pas gênant dans le TP, mais en production on filtrerait les services exposés (par exemple pour ne pas exposer les endpoints de la passerelle).

### Questions de synthèse
**1.** Nombre de programmes : B, C, E. Dépendances Maven : B.2, C.1, C.7, E.1. Adresse d'un microservice : C.2 et C.7 (properties), D.2. Appel entre microservices : D.2. Erreurs d'appel : D.3. Point d'entrée du client : E.2. Plusieurs instances : C.5, D.4, E.3. Serveur embarqué : E.1 (Netty de la passerelle). Arrêt d'un microservice : C.6. Organisation du code : B.0 et C.0. Outil de test (Swagger) : C.4. Classes de données (Lombok) : C.1 bis et C.7.

**2.** Le client envoie `POST :8888/historique-service/operations` à la passerelle (requête HTTP 1). Elle consulte sa copie locale de l'annuaire pour `HISTORIQUE-SERVICE`, choisit une instance (LoadBalancer), retire le préfixe et transmet `POST /operations` à `historique-service:8082` (requête HTTP 2). `OperationController` appelle `calculClient.calculer("somme",1,2)` : OpenFeign consulte la copie locale de l'annuaire pour `CALCUL-SERVICE`, LoadBalancer choisit 8081 ou 8083 et envoie `GET /calcul/somme?a=1&b=2` (requête HTTP 3). La réponse `3` remonte : calcul-service, historique-service (qui crée l'opération, code 201 et `Location`), passerelle, client.

**3.** Gateway-service (résout `CALCUL-SERVICE` et `HISTORIQUE-SERVICE`), historique-service (résout `CALCUL-SERVICE`), et calcul-service (le répartiteur est présent via le starter client Eureka, mais il ne résout aucun nom car calcul-service n'appelle personne).

**4.** Client Eureka : nécessaire dans les trois (calcul et historique pour s'inscrire, historique et gateway pour découvrir les instances). OpenFeign : seulement dans historique-service, le seul à appeler un autre microservice. Spring Cloud Gateway : seulement dans gateway-service, le point d'entrée.

**5.** Avec l'adresse en dur, historique-service n'appelle toujours que l'instance dont l'adresse est dans la configuration : les autres instances sont ignorées, et un changement de port ou de machine impose de modifier la configuration et de relancer. Dans ce TP : Eureka Server (annuaire) et Eureka Client (inscription et découverte) remplacent l'adresse par un nom, OpenFeign avec Spring Cloud LoadBalancer répartit les appels en round robin, et la passerelle fait de même avec `lb://`.

**6.** Chaque instance a sa propre mémoire : GET renvoie seulement les opérations reçues par l'instance sollicitée (le résultat change d'un appel à l'autre). C'est le principe des services **sans état** (stateless) qui est mis en défaut. On le corrige en externalisant les données dans une base de données partagée.

**7.** À la racine : `mvn clean package -DskipTests` (ou `.\mvnw.cmd clean package -DskipTests`) construit les quatre modules. Puis, chacun dans sa console, dans l'ordre : `java -jar eureka-service/target/eureka-service-0.0.1-SNAPSHOT.jar`, `java -jar calcul-service/target/calcul-service-0.0.1-SNAPSHOT.jar`, la même avec `--server.port=8083`, `java -jar historique-service/target/historique-service-0.0.1-SNAPSHOT.jar`, puis `java -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar`.

**8.** Dans `CalculController`, `@GetMapping` décrit la requête **reçue** (route exposée par le serveur). Dans `CalculClient`, elle décrit la requête **envoyée** : OpenFeign génère l'implémentation du client à partir de l'interface.

**9.** Non. Chaque microservice garde sa propre JVM (un processus Java chacun), son port, son fichier `.jar` et son cycle de vie (lancement, arrêt, redéploiement sans toucher aux autres). Le regroupement en modules est seulement organisationnel.

**10.** L'adresse vient des en-têtes `X-Forwarded-*` ajoutés par la passerelle (et `X-Forwarded-Prefix` à l'étape E.5, car la passerelle retire le préfixe), lus grâce à `server.forward-headers-strategy=framework`. Sans cette propriété, le microservice utiliserait son propre `Host` (IP:8081) : Execute enverrait la requête directement au microservice en contournant la passerelle, et échouerait à cause de CORS (Failed to fetch).

**11.** Les méthodes générées sont dans `Operation.class` (dans `target/classes`), visibles dans la fenêtre Structure d'IntelliJ. Lombok est un processeur d'annotations qui n'agit qu'à la compilation : à l'exécution les `.class` contiennent déjà le code, donc la bibliothèque n'est pas nécessaire dans le `.jar` exécutable.

## Remarque sur Swagger UI derrière la passerelle
Dans cette configuration, les en-têtes `X-Forwarded-*` n'étaient pas transmis par la passerelle. Pour que le bouton Execute de Swagger passe par la passerelle, l'adresse du serveur OpenAPI est déclarée relative (`/`) dans un bean `OpenAPI`.
