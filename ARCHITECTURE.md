# Architecture

## Backend — Architecture Hexagonale (Ports & Adapters)

Le backend suit une **architecture hexagonale** stricte. Les dépendances ne pointent que vers l'intérieur :

```
Infrastructure  →  Application  →  Domain
     (jamais l'inverse)
```

Les règles sont vérifiées automatiquement à chaque build par ArchUnit (voir `ArchRulesTest`).

---

### Bounded Contexts

Le domaine est découpé en trois BCs isolés. Aucun BC ne dépend du domaine ou de l'application d'un autre.

```
com.synapsys.api/
├── authentication/          ← Login, refresh token, JWT, cookies
│   ├── domain/
│   │   ├── model/           ← UserCredentials, AuthTokens, LoginResult, AuthenticationException…
│   │   └── port/out/        ← UserCredentialsPort, AccessTokenPort, TotpStatusQueryPort…
│   ├── application/
│   │   ├── handler/         ← LoginHandler, RefreshTokenHandler, VerifyTotpChallengeHandler…
│   │   └── port/in/         ← LoginUseCase, VerifyTotpChallengeUseCase, CredentialSetupUseCase…
│   └── infrastructure/
│       ├── persistence/     ← UserCredentialsRepositoryAdapter, RefreshTokenRepositoryAdapter…
│       ├── security/        ← JwtService, BcryptPasswordHasher, RedisTotpChallengeStore…
│       └── web/             ← AuthController, TotpChallengeController, AuthenticationExceptionHandler
│
├── identity/                ← Profil utilisateur, registration, seed
│   ├── domain/
│   │   ├── model/           ← User, RegisterCommand, IdentityException…
│   │   └── port/out/        ← UserCommandPort, UserRepository, CredentialSetupPort…
│   ├── application/
│   │   ├── handler/         ← RegisterHandler, SeedHandler, DeactivateUserHandler…
│   │   └── port/in/         ← RegisterUseCase, FindUserUseCase, SeedUseCase…
│   └── infrastructure/
│       ├── persistence/     ← UserIdentityRepositoryAdapter
│       ├── security/        ← CredentialSetupAdapter, TotpRecordInitAdapter…
│       └── web/             ← UserController, IdentityExceptionHandler
│
├── mfa/                     ← TOTP : setup, confirm, disable, admin reset
│   ├── domain/
│   │   ├── model/           ← UserTotpProfile, MfaException…
│   │   └── port/out/        ← TotpCodeValidatorPort, TotpCodeReplayPort, UserTotpQueryPort…
│   ├── application/
│   │   ├── handler/         ← SetupTotpHandler, ConfirmTotpHandler, DisableTotpHandler…
│   │   ├── service/         ← TotpCodeVerificationService, TotpStatusService…
│   │   ├── port/in/         ← SetupTotpUseCase, ConfirmTotpUseCase, VerifyTotpCodeUseCase…
│   │   └── dto/             ← TotpCodeVerifyResult (résultat ACL inter-BC)
│   └── infrastructure/
│       ├── persistence/     ← UserTotpRepositoryAdapter
│       ├── security/        ← TotpServiceAdapter, RedisTotpCodeReplayStore…
│       ├── config/          ← TotpEncryptionConfig (chiffrement AES-256/GCM par userId)
│       └── web/             ← TotpController, MfaExceptionHandler
│
├── shared/                  ← Types transverses uniquement
│   ├── model/               ← Role (enum), TotpPolicy (MAX_ATTEMPTS)
│   ├── annotation/          ← @ApplicationService
│   └── infrastructure/web/  ← ProblemDetailFactory
│
└── infrastructure/          ← Config Spring Boot globale
    ├── config/              ← SecurityConfig, SynapsysProperties, DataSeeder
    └── ratelimit/           ← RateLimitMethodInterceptor, Bucket4j + Redis
```

---

### Dépendances cross-BC acceptées (adaptateurs uniquement)

Les seules dépendances infra → application d'un autre BC sont documentées et contraintes par ArchUnit :

| Adaptateur | Dépendance cross-BC | Raison |
|---|---|---|
| `UserProfileAdapter` | `authentication.infra` → `identity.application.port.in` | Lecture profil sans dupliquer le store identity |
| `MfaTotpVerifierAdapter` | `authentication.infra` → `mfa.application.port.in + dto` | ACL : mappe `TotpCodeVerifyResult` → `TotpVerificationResult` |
| `TotpStatusAdapter` | `authentication.infra` → `mfa.application.port.in` | Query statut TOTP depuis LoginHandler |
| `CredentialSetupAdapter` | `identity.infra` → `authentication.application.port.in` | Création credentials après registration |
| `TotpRecordInitAdapter` | `identity.infra` → `mfa.application.port.in` | Init enregistrement TOTP à la registration |
| `MfaAdminResetTotpAdapter` | `identity.infra` → `mfa.application.port.in` | Reset TOTP admin depuis identity |

Toute autre dépendance cross-BC dans les couches infra ou application casse le build (ArchUnit).

---

### Règles ArchUnit vérifiées automatiquement

- Le domaine ne dépend ni de Spring ni de l'infrastructure
- L'application ne dépend pas de l'infrastructure
- Les controllers ne dépendent pas des handlers (passent par les ports `port/in`)
- Chaque BC : `domain + application` isolé des autres BCs
- Chaque BC : `infrastructure` ne dépend pas de l'infrastructure d'un autre BC
- Tout `@ApplicationService` est dans un package `application`
- Tout `@Component` Adapter implémente un port `domain.port.out`
- Les ports entrants (`application.port.in`) ne contiennent que des interfaces
- Les ports sortants (`domain.port.out`) ne contiennent que des interfaces

---

### Transactions

`@Transactional` est posé directement sur les méthodes des handlers qui modifient l'état. `RefreshTokenGenerator` utilise `Propagation.MANDATORY` (doit s'exécuter dans une transaction parente). Les opérations Redis (challenge store, replay store) ne participent pas à la transaction JPA — trade-off documenté.

---

### Sécurité

- **JWT** (access token) : HttpOnly cookie, 15 min par défaut, HMAC SHA-256
- **Refresh token** : rotation à chaque usage, hash SHA-256 en base, révocation cascade si réutilisation détectée
- **Rate limiting** : Bucket4j + Redis, configurable par endpoint via `@RateLimiting`
- **TOTP** : secret chiffré AES-256/GCM (clé maître `SYNAPSYS_ENCRYPTION_SECRET`, sel par userId), anti-replay Redis SETNX, lockout 429 après 5 échecs
- **Cookies** : HttpOnly, Secure, SameSite=Strict, paths séparés (`/api/auth` vs `/api/auth/2fa`)
- **Timing attack** : dummy BCrypt hash précompilé pour les usernames inconnus

---

## Frontend — Feature-Sliced Design (FSD)

Le frontend suit **FSD** avec des couches strictement ordonnées. Une couche ne peut importer que des couches inférieures.

```
app  →  pages  →  features  →  entities  →  shared
         (vers le bas uniquement)
```

### Structure

```
src/
├── app/              ← Bootstrap, providers, router, i18n
├── pages/
│   ├── login/        ← LoginPage (orchestration multi-step : credentials → TOTP → enrollment)
│   └── profile/      ← ProfilePage
├── features/
│   ├── auth/
│   │   ├── api/      ← IAuthApi (interface), authApi.ts (impl axios)
│   │   ├── model/    ← authStore.ts (Zustand), types.ts, errors.ts
│   │   ├── ui/       ← LoginForm.tsx, AuthStoreProvider.tsx
│   │   ├── locales/  ← Traductions EN/FR
│   │   └── index.ts  ← Exports publics uniquement
│   └── totp/
│       ├── api/      ← ITotpVerifyApi, ITotpEnrollApi, totpApi.ts
│       ├── model/    ← types.ts, errors.ts
│       ├── ui/       ← TotpDigitInput, TotpVerifyStep, TotpEnrollProposal, TotpSetupFlow
│       ├── locales/  ← Traductions EN/FR
│       └── index.ts
├── entities/
│   └── user/         ← Type User { id, username, role }
└── shared/
    ├── api/          ← client axios, refreshInterceptor, notifyLoginSuccess
    ├── lib/          ← sessionHint, parseRetryAfter, sessionCallbacks, apiErrors
    └── ui/           ← Button, Input, Spinner (composants génériques)
```

### Flux d'authentification

```
login(credentials)
  ├─ totp_required       → step = 'totp'    → TotpVerifyStep → finalizeLogin(user)
  └─ enrollment_proposed → step = 'enroll'  → TotpEnrollProposal
                               ├─ onActivate → step = 'setup' → TotpSetupFlow → finalizeLogin(user)
                               └─ onSkip     → finalizeLogin(pendingUser)

finalizeLogin(user)      → set store + sessionHint + notifyLoginSuccess → /profile

refreshInterceptor
  └─ 401 détecté         → POST /api/auth/refresh → retry requête originale
  └─ refresh échoue      → triggerSessionExpired()
```

### Injection de dépendances

`createAuthStore(api: IAuthApi)` et `TotpSetupFlow({ api: ITotpEnrollApi })` acceptent n'importe quelle implémentation. En test, des mocks. Aucun composant n'importe les implémentations directement.