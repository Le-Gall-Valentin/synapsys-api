# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Applications, routines, permissions, agent enrollment (planned)

---

## [0.3.0] — 2026-06-03

### Added — BC Split (authentication / identity / mfa)
- Bounded context `authentication` : login, refresh token, JWT, cookies, TOTP challenge
- Bounded context `identity` : user profiles, registration, seed, deactivation
- Bounded context `mfa` : TOTP setup/confirm/disable, admin reset, per-user AES-256/GCM encryption
- `ArchRulesTest` généralisé : isolation BC par `@ParameterizedTest`, whitelist cross-BC centralisée
- `ProblemDetailFactory` partagé (élimine la duplication dans les 5 exception handlers)
- `TotpPolicy.MAX_ATTEMPTS` source de vérité unique pour les deux BCs
- TTL du challenge TOTP configurable via `synapsys.security.challenge-ttl-minutes`

### Changed
- `TotpMaxAttemptsExceeded` retourne HTTP 429 (cohérent avec le flow `/confirm`)
- `TotpChallengeExpired` retourne HTTP 401 + `error_code: "totp_challenge_expired"` stable (découplé des noms de classes Java)
- `UserCommandPort.createProfile()` retourne `User` directement (supprime le SELECT post-INSERT)
- `isTotpEnabled()` déplacé de l'adapter de persistance vers `LoginHandler` (après validation du mot de passe)
- Cache `ConcurrentHashMap` sur `TotpEncryptorFactory` (évite de recalculer PBKDF2 à chaque appel)

### Fixed
- Bug production : `TotpChallengeExpiredError` et `TotpMaxAttemptsError` n'étaient jamais lancés côté frontend (`verify()` comparait sur les titres Java qui ne correspondaient pas)
- Vulnérabilité TOTP replay → verrouillage victime : `REPLAYED` désormais distinct de `INVALID`, n'incrémente pas le compteur d'échecs

---

## [0.2.0] — 2026-05-30

### Added — TOTP / 2FA
- Enrollment TOTP : `POST /api/auth/2fa/setup` + `POST /api/auth/2fa/confirm`
- Désactivation : `DELETE /api/auth/2fa`
- Reset admin : `POST /api/admin/users/{id}/2fa/reset` (SUPER_ADMIN ou ADMIN selon la cible)
- Challenge login : cookie `totp_challenge` HttpOnly → `POST /api/auth/2fa/verify`
- Anti-replay Redis SETNX atomique + TTL 150 s
- Lockout après 5 échecs, invalidation du challenge
- Chiffrement AES-256/GCM du secret TOTP (clé maître + sel par userId via PBKDF2)
- `TotpDigitInput` : 6 champs séparés, auto-avance, paste, backspace, aria-labels
- `TotpVerifyStep` : step TOTP dans le flow login
- `TotpEnrollProposal` : proposition d'enrollment skippable après login
- `TotpSetupFlow` : QR code + secret masqué + confirmation + redémarrage automatique après max attempts

### Changed
- `authStore.login()` retourne `LoginOutcome` (`totp_required` | `enrollment_proposed`)
- `finalizeLogin(user)` : seul point de commit session + `notifyLoginSuccess`
- `parseRetryAfter()` extrait dans `shared/lib` (élimine la duplication dans 3 callsites)

---

## [0.1.0] — 2026-05-27

### Added — Migration Redis (rate limiting)
- Remplacement Caffeine → Bucket4j + Redis pour le rate limiting
- `@RateLimiting` annotation configurable par endpoint (max, window, mode IP/USER)
- Headers `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`

### Added — Authentification & Gestion utilisateurs
- Login / logout via cookies HttpOnly (access + refresh token)
- Rotation du refresh token à chaque usage, révocation cascade si réutilisation détectée
- JWT HMAC SHA-256, validation `iss` + `aud`
- Registration avec hiérarchie de rôles : `SUPER_ADMIN` > `ADMIN` > `USER`
- Désactivation de compte (`DELETE /api/users/{id}`)
- Seed automatique du premier `SUPER_ADMIN` au démarrage
- `GET /api/users/me` — profil de l'utilisateur connecté
- Refresh token purge planifiée (cron configurable)