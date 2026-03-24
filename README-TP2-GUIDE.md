# Guide TP2 (7 etapes) base sur `auth_back`

Ce guide te donne une progression **etape par etape** pour passer de ton TP1 actuel vers un TP2 conforme au sujet.
Chaque etape contient:
- objectif
- fichiers a modifier/creer
- code de reference
- verification a faire
- tag Git conseille

---

## Prerequis

- Projet de depart: TP1 termine (ton dossier `auth_back` actuel)
- Java 17
- Maven Wrapper (`mvnw.cmd`)
- SonarCloud connecte au repo GitHub

---

## Etape 1 - Migration base (`v2.1-db-migration`)

### Objectif
Remplacer `password_clear` par `password_hash` en base.

### Fichiers
- `src/main/resources/schema.sql`
- `src/main/resources/data.sql`
- `src/main/java/com/example/auth/entity/User.java`

### Code de reference

`schema.sql`
```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME,
    token VARCHAR(255),
    failed_attempts INT NOT NULL DEFAULT 0,
    lock_until DATETIME NULL
);
```

`data.sql`
```sql
INSERT INTO users(email, password_hash, created_at, token, failed_attempts, lock_until)
VALUES ('toto@example.com', '$2a$10$7QJ3v6Yf9VgSxWn8v2Y5uOe8S1fD7Vv8rR6QfYf2A6j8xU5Yp3QhS', NOW(), NULL, 0, NULL);
```

`User.java` (extrait)
```java
@Column(name = "password_hash", nullable = false)
private String passwordHash;

@Column(name = "failed_attempts", nullable = false)
private int failedAttempts;

@Column(name = "lock_until")
private LocalDateTime lockUntil;
```

### Verification
- Lancer: `.\mvnw.cmd test`
- La table doit se creer avec `password_hash`.

---

## Etape 2 - Politique mot de passe (`v2.2-password-policy`)

### Objectif
Valider un mot de passe fort cote serveur:
- min 12
- 1 majuscule
- 1 minuscule
- 1 chiffre
- 1 caractere special

### Fichiers
- Creer `src/main/java/com/example/auth/security/PasswordPolicyValidator.java`
- Ajouter des tests dans `src/test/java/.../AuthServiceTest.java` (ou un test dedie)

### Code de reference

`PasswordPolicyValidator.java`
```java
package com.example.auth.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {
    public boolean isValid(String password) {
        if (password == null || password.length() < 12) return false;
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
```

### Verification
Ajouter des tests:
- KO si trop court
- KO si sans majuscule/minuscule/chiffre/special
- OK sur un mot de passe conforme

---

## Etape 3 - Hashing BCrypt (`v2.3-hashing`)

### Objectif
Ne plus stocker le mot de passe en clair. Utiliser BCrypt.

### Fichiers
- `pom.xml`
- Creer `src/main/java/com/example/auth/config/SecurityBeansConfig.java`
- Modifier `AuthService.java`
- Modifier `User.java` (getter/setter `passwordHash`)

### Code de reference

`pom.xml` (dependance)
```xml
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-crypto</artifactId>
</dependency>
```

`SecurityBeansConfig.java`
```java
package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityBeansConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

`AuthService.java` (logique)
```java
private final PasswordEncoder passwordEncoder;
private final PasswordPolicyValidator passwordPolicyValidator;

public AuthService(UserRepository userRepository,
                   PasswordEncoder passwordEncoder,
                   PasswordPolicyValidator passwordPolicyValidator) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicyValidator = passwordPolicyValidator;
}

public User register(String email, String password) {
    if (!passwordPolicyValidator.isValid(password)) {
        throw new InvalidInputException("Mot de passe non conforme a la politique TP2");
    }
    if (userRepository.findByEmail(email).isPresent()) {
        throw new ResourceConflictException("Email deja utilise");
    }
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setCreatedAt(LocalDateTime.now());
    return userRepository.save(user);
}

public User login(String email, String password) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthenticationFailedException("Email ou mot de passe incorrect"));

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
        throw new AuthenticationFailedException("Email ou mot de passe incorrect");
    }

    user.setToken(UUID.randomUUID().toString());
    return userRepository.save(user);
}
```

### Verification
- Test: `password_hash` en base commence par `$2a$` ou `$2b$`.
- Test: login OK avec bon mot de passe, KO sinon.

---

## Etape 4 - Anti brute force (`v2.4-lockout`)

### Objectif
Bloquer un compte 2 minutes apres 5 echecs consecutifs.

### Fichiers
- `User.java` (deja prepare en etape 1)
- `AuthService.java`
- `GlobalExceptionHandler.java` (+ nouvelle exception)

### Code de reference

`TooManyAttemptsException.java`
```java
package com.example.auth.exception;

public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
```

`AuthService.java` (extrait login)
```java
private static final int MAX_FAILED_ATTEMPTS = 5;
private static final long LOCK_MINUTES = 2;

public User login(String email, String password) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthenticationFailedException("Email ou mot de passe incorrect"));

    if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
        throw new TooManyAttemptsException("Compte temporairement bloque");
    }

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
        int nextAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(nextAttempts);
        if (nextAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            user.setFailedAttempts(0);
        }
        userRepository.save(user);
        throw new AuthenticationFailedException("Email ou mot de passe incorrect");
    }

    user.setFailedAttempts(0);
    user.setLockUntil(null);
    user.setToken(UUID.randomUUID().toString());
    return userRepository.save(user);
}
```

`GlobalExceptionHandler.java` (handler 423)
```java
@ExceptionHandler(TooManyAttemptsException.class)
public ResponseEntity<Map<String, Object>> handleTooManyAttempts(TooManyAttemptsException ex,
                                                                  HttpServletRequest request) {
    return ResponseEntity.status(423).body(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 423,
            "error", "Locked",
            "message", ex.getMessage(),
            "path", request.getRequestURI()
    ));
}
```

### Verification
- 5 mauvais logins consecutifs -> compte bloque.
- Login OK apres expiration du lock.

---

## Etape 5 - UI force mot de passe (`v2.5-ui-strength`)

### Objectif
Client: double saisie + indicateur force.

### Note
Ton repo `auth_front` est actuellement minimal. Cette etape se fait surtout cote client lourd.

### Comportement attendu
- Champs `password` + `passwordConfirm`
- Barre/label force:
  - Rouge: non conforme
  - Orange: conforme mais faible
  - Vert: bon niveau
- Bouton inscription desactive tant que invalide

### Regle de base reutilisable
```java
public enum Strength { RED, ORANGE, GREEN }
```

Pseudo-regle:
- Si non conforme policy TP2 -> RED
- Conforme mais score moyen -> ORANGE
- Conforme + longueur elevee + diversite -> GREEN

---

## Etape 6 - SonarCloud obligatoire (`v2.6-sonarcloud`)

### Objectif
Avoir analyse SonarCloud et corriger:
- bugs majeurs
- vulnerabilities majeures
- code smells prioritaires

### Fichiers
- `.github/workflows/sonar.yml` (ou `ci.yml`)
- `pom.xml` (si besoin de properties sonar)
- `README.md` (section Qualite TP2)

### Exemple workflow (bloquant)
```yaml
name: CI
on:
  push:
    branches: ["main"]
  pull_request:
    branches: ["main"]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Build and test
        run: mvn -B verify
      - name: SonarCloud
        run: mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar -Dsonar.projectKey=${{ secrets.SONAR_PROJECT_KEY }} -Dsonar.organization=${{ secrets.SONAR_ORGANIZATION }} -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=${{ secrets.SONAR_TOKEN }}
```

### Verification
- Quality Gate vert (ou justification ecrite en README si encore bloque en TP2).

---

## Etape 7 - Finalisation (`v2-tp2`)

### Objectif
Livrer une version propre et defendable.

### Checklist finale
- JavaDoc complete sur classes security/validator/service/exceptions
- Tous les tests verts
- README mis a jour:
  - objectif TP2
  - SonarCloud (config + resultat)
  - couverture approx et methode de mesure
  - limites de securite restantes (rejeu encore possible)
- Nettoyage warnings et code smells majeurs

### Commandes de validation
```bash
./mvnw test
./mvnw -DskipTests=false verify
```

### Tags Git TP2 (ordre recommande)
- `v2.0-start`
- `v2.1-db-migration`
- `v2.2-password-policy`
- `v2.3-hashing`
- `v2.4-lockout`
- `v2.5-ui-strength`
- `v2.6-sonarcloud`
- `v2-tp2`

---

## Plan rapide d'implementation sur ton repo actuel

1. Faire etape 1 + 2 + 3 d'abord (schema + validator + bcrypt)
2. Stabiliser les tests unitaires
3. Ajouter lockout (etape 4) + tests dedies
4. Traiter Sonar (etape 6)
5. Finaliser JavaDoc/README (etape 7)

Si tu veux, je peux maintenant enchaîner directement en **implémentant automatiquement l’etape 1 puis l’etape 2 dans le code** (avec tests), puis avancer etape par etape jusqu’au `v2-tp2`.
