# Guide TP3 - Authentification Forte (base `auth_back`)

Ce guide te fait passer de TP2 a TP3 avec une implementation progressive et defendable.
Objectif TP3: ne plus envoyer le mot de passe au login; le client envoie une preuve HMAC.
Tag final attendu: `v3-tp3`.

## Vue d'ensemble
- Protocole login: `message = email:nonce:timestamp`
- `hmac = HMAC_SHA256(password, message)` cote client
- Verification serveur en temps constant
- Anti-rejeu par nonce + timestamp
- Token d'acces avec expiration
- SonarCloud + couverture >= 80%

## Etape 0 - Start (`v3.0-start`)
- Mettre a jour `README.md` avec objectifs TP3
- Verifier TP2: `.\mvnw.cmd test`

## Etape 1 - Schema nonce (`v3.1-db-nonce`)
Fichiers:
- `src/main/resources/schema.sql`
- `src/main/java/com/example/auth/entity/AuthNonce.java`
- `src/main/java/com/example/auth/repository/AuthNonceRepository.java`

SQL conseille:
```sql
CREATE TABLE IF NOT EXISTS auth_nonce (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  nonce VARCHAR(128) NOT NULL,
  expires_at DATETIME NOT NULL,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_auth_nonce UNIQUE (user_id, nonce),
  CONSTRAINT fk_auth_nonce_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Etape 2 - DTO login HMAC (`v3.2-hmac-client`)
Fichier:
- `src/main/java/com/example/auth/dto/LoginRequest.java`

DTO conseille:
```java
public class LoginRequest {
  private String email;
  private String nonce;
  private long timestamp;
  private String hmac;
}
```

## Etape 3 - Verification HMAC serveur (`v3.3-hmac-server`)
Fichiers:
- `src/main/java/com/example/auth/service/AuthService.java`
- `src/main/java/com/example/auth/security/HmacService.java`

Service HMAC exemple:
```java
@Service
public class HmacService {
  public String hmacSha256Hex(String secret, String message) { /* ... */ }
  public boolean constantTimeEqualsHex(String a, String b) { /* MessageDigest.isEqual */ }
}
```

Ordre de verification login:
1. email existe sinon 401
2. timestamp dans fenetre (ex +/- 60 sec)
3. nonce non rejoue
4. recalcul hmac attendu
5. comparaison temps constant
6. consommer nonce
7. emettre token

## Etape 4 - Anti-replay (`v3.4-anti-replay`)
- nonce unique par user `(user_id, nonce)`
- TTL nonce ~120 sec
- rejet 401 si nonce deja utilise/expiré

## Etape 5 - Token (`v3.5-token`)
- login retourne:
```json
{"accessToken":"...","expiresAt":"2026-03-25T10:00:00"}
```
- `GET /api/me` accessible uniquement avec token valide non expire

## Etape 6 - Tests + couverture (`v3.6-tests-80`)
Tests obligatoires (min 15):
- login OK hmac valide
- KO hmac invalide
- KO timestamp expire/futur
- KO nonce rejoue
- KO user inconnu
- comparaison temps constant
- token emis
- `/api/me` OK/KO selon token

Commandes:
```bash
./mvnw test
./mvnw verify
```

## Etape 7 - Finalisation (`v3-tp3`)
- JavaDoc security/protocole
- SonarCloud quality gate vert
- README: limites de securite du mecanisme pedagogique

Tags recommandes:
- `v3.0-start`
- `v3.1-db-nonce`
- `v3.2-hmac-client`
- `v3.3-hmac-server`
- `v3.4-anti-replay`
- `v3.5-token`
- `v3.6-tests-80`
- `v3-tp3`
# Guide TP3 - Authentification Forte (base `auth_back`)

Ce guide te fait passer de TP2 a TP3 avec une implementation progressive et defendable.

Objectif TP3: ne plus envoyer le mot de passe au login; le client envoie une preuve HMAC signee avec `email + nonce + timestamp`.

Tag final attendu: `v3-tp3`

---

## Vue d'ensemble TP3

- Ajouter un protocole de login en 2 etapes:
  - client calcule `hmac = HMAC_SHA256(password, email:nonce:timestamp)`
  - serveur recalcule et compare en temps constant
- Anti-rejeu:
  - nonce unique par utilisateur
  - fenetre timestamp (ex: +/- 60 sec)
- Emission d'un token d'acces avec expiration
- SonarCloud + couverture >= 80%

---

## Etape 0 - Start (`v3.0-start`)

- Mettre a jour `README.md` avec objectifs TP3
- Garder TP2 vert: `.\mvnw.cmd test`

---

## Etape 1 - Schema nonce (`v3.1-db-nonce`)

### Fichiers
- `src/main/resources/schema.sql`
- `src/main/java/com/example/auth/entity/AuthNonce.java`
- `src/main/java/com/example/auth/repository/AuthNonceRepository.java`

### Schema conseille
```sql
CREATE TABLE IF NOT EXISTS auth_nonce (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  nonce VARCHAR(128) NOT NULL,
  expires_at DATETIME NOT NULL,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_auth_nonce UNIQUE (user_id, nonce),
  CONSTRAINT fk_auth_nonce_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Entite exemple
```java
@Entity
@Table(name = "auth_nonce", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "nonce"}))
public class AuthNonce {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 128)
  private String nonce;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private boolean consumed;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
```

---

## Etape 2 - DTO login HMAC cote client (`v3.2-hmac-client`)

### Fichiers
- `src/main/java/com/example/auth/dto/LoginRequest.java` (nouveau format)

### DTO conseille
```java
public class LoginRequest {
  private String email;
  private String nonce;
  private long timestamp;
  private String hmac;
}
```

### Format du message signe
`message = email + ":" + nonce + ":" + timestamp`

---

## Etape 3 - Verification HMAC cote serveur (`v3.3-hmac-server`)

### Fichiers
- `src/main/java/com/example/auth/service/AuthService.java`
- `src/main/java/com/example/auth/security/HmacService.java`

### Service HMAC (exemple)
```java
@Service
public class HmacService {
  public String hmacSha256Hex(String secret, String message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC calculation error", e);
    }
  }

  public boolean constantTimeEqualsHex(String a, String b) {
    byte[] left = HexFormat.of().parseHex(a == null ? "" : a);
    byte[] right = HexFormat.of().parseHex(b == null ? "" : b);
    return MessageDigest.isEqual(left, right);
  }
}
```

### Logique serveur login (ordre obligatoire)
1. user existe sinon 401
2. timestamp dans fenetre (ex: 60 sec)
3. nonce non reutilise
4. recalcul HMAC attendu
5. comparaison temps constant
6. marquer nonce consomme
7. generer token + expiresAt

---

## Etape 4 - Anti-replay nonce (`v3.4-anti-replay`)

### Regles
- `(user_id, nonce)` unique
- nonce TTL 120 sec
- si nonce deja vu/consomme -> 401

### A coder
- methode `isReplay(user, nonce)`
- purge des nonces expires (optionnel mais recommande)

---

## Etape 5 - Token d'acces (`v3.5-token`)

### Fichiers
- `AuthService` + `AuthController`

### Reponse login conseillee
```json
{
  "accessToken": "uuid-ou-token",
  "expiresAt": "2026-03-25T10:00:00"
}
```

### Endpoint `/api/me`
- Refuser sans token valide
- Accepter avec token non expire

---

## Etape 6 - Tests + couverture 80 (`v3.6-tests-80`)

### Tests obligatoires (minimum 15)
- login OK hmac valide
- KO hmac invalide
- KO timestamp expire
- KO timestamp futur hors fenetre
- KO nonce rejoue
- KO user inconnu
- comparaison temps constant testee
- token emis
- `/api/me` OK avec token
- `/api/me` KO sans token

### Commandes
```bash
./mvnw test
./mvnw verify
```

---

## Etape 7 - Finalisation (`v3-tp3`)

- JavaDoc security/protocole complete
- README: limites du mecanisme pedagogique
- SonarCloud quality gate vert
- Tags TP3 poses dans l'ordre

Tags recommandes:
- `v3.0-start`
- `v3.1-db-nonce`
- `v3.2-hmac-client`
- `v3.3-hmac-server`
- `v3.4-anti-replay`
- `v3.5-token`
- `v3.6-tests-80`
- `v3-tp3`

---

## Checklist rapide TP3

- [ ] login sans mot de passe transmis en clair/hash
- [ ] timestamp verifie (+/- 60 s)
- [ ] nonce anti-rejeu stocke + consomme
- [ ] comparaison HMAC en temps constant
- [ ] token avec expiration
- [ ] tests >= 15
- [ ] couverture >= 80%
- [ ] SonarCloud vert
