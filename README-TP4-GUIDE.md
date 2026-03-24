# Guide TP4 - Master Key + CI/CD (base `auth_back`)

Objectif TP4: industrialiser sans changer le protocole TP3.
- Chiffrer les mots de passe au repos avec `APP_MASTER_KEY`
- Pipeline GitHub Actions bloquante (tests + SonarCloud)
- Quality Gate vert obligatoire

Tag final attendu: `v4-tp4`.

## Vue d'ensemble
- Pas de secret en dur
- `APP_MASTER_KEY` obligatoire au demarrage
- Chiffrement recommande: AES-GCM
- CI sur push + PR vers `main`
- Echec si test KO ou Sonar gate rouge

## Etape 0 - Start (`v4.0-start`)
- Mettre a jour `README.md` avec objectifs TP4
- Verifier TP3 vert localement

## Etape 1 - Master Key config (`v4.1-master-key-config`)
Fichiers:
- `src/main/resources/application.properties`
- `src/main/java/com/example/auth/config/CryptoConfig.java`

Exemple:
```properties
app.master-key=${APP_MASTER_KEY:}
```

```java
@ConfigurationProperties(prefix = "app")
public class CryptoConfig {
  private String masterKey;
}
```

Regle:
- si `APP_MASTER_KEY` absente/vide -> application refuse de demarrer

## Etape 2 - Service chiffrement (`v4.2-aes-gcm`)
Fichier:
- `src/main/java/com/example/auth/security/AesGcmCryptoService.java`

Format conseille:
- `v1:Base64(iv):Base64(ciphertext)`

Interdictions:
- pas ECB
- pas IV fixe
- pas log de secret/mot de passe

## Etape 3 - Migration data model (`v4.3-password-encrypted`)
Fichiers:
- `schema.sql`
- `User.java`
- services/repositories

Base:
- supprimer `password_clear`
- utiliser `password_encrypted`

Inscription:
- `password_plain -> encrypt(APP_MASTER_KEY) -> password_encrypted`

Login TP3:
- lire `password_encrypted`
- dechiffrer
- recalculer HMAC

## Etape 4 - Tests crypto (`v4.4-crypto-tests`)
Tests obligatoires:
- demarrage KO si `APP_MASTER_KEY` absente
- encryption/decryption OK
- ciphertext != plaintext
- decryption KO si ciphertext altere

## Etape 5 - CI GitHub Actions (`v4.5-ci`)
Fichier requis:
- `.github/workflows/ci.yml`

Declencheurs:
- push sur `main`
- pull_request vers `main`

Pipeline minimale:
1. checkout
2. setup JDK 17
3. cache maven
4. tests
5. sonar scan
6. echec auto si test KO
7. echec auto si quality gate rouge

Exemple etape build:
```yaml
- name: Build and test
  run: mvn -B verify
```

## Etape 6 - Secrets Sonar + master key CI (`v4.6-secrets-sonar`)
Configurer dans GitHub > Secrets and variables > Actions:
- `SONAR_TOKEN`
- `SONAR_PROJECT_KEY`
- `SONAR_ORGANIZATION`
- `APP_MASTER_KEY` (fictive en CI si besoin)

Notes:
- ne jamais commiter de valeurs reelles
- H2 en memoire pour les tests CI

## Etape 7 - Finalisation (`v4-tp4`)
- JavaDoc complete crypto + pipeline
- README final (securite + industrialisation)
- SonarCloud vert
- Branch protection pour bloquer merge si CI rouge

Tags recommandes:
- `v4.0-start`
- `v4.1-master-key-config`
- `v4.2-aes-gcm`
- `v4.3-password-encrypted`
- `v4.4-crypto-tests`
- `v4.5-ci`
- `v4.6-secrets-sonar`
- `v4-tp4`

## Checklist TP4
- [ ] APP_MASTER_KEY obligatoire et non hardcodee
- [ ] AES-GCM avec IV aleatoire
- [ ] password_encrypted en base
- [ ] tests crypto passes
- [ ] CI blocante sur tests et Sonar
- [ ] secrets uniquement via variables/secrets
