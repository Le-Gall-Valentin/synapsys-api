# Architecture

## Backend — Architecture Hexagonale

Le backend suit une **architecture hexagonale** (Ports & Adapters). La règle fondamentale : les dépendances ne pointent que vers l'intérieur.

```
Infrastructure  →  Application  →  Domain
     (jamais l'inverse)
```

### Structure des packages

```
com.synapsys.api/
├── auth/
│   ├── domain/              ← Cœur métier pur (zéro dépendance Spring)
│   │   ├── model/           ← Records immuables : User, RefreshToken, AuthTokens…
│   │   ├── port/
│   │   │   ├── in/          ← Ports entrants (UseCase interfaces) : LoginUseCase, RegisterUseCase…
│   │   │   └── out/         ← Ports sortants : UserRepository, AccessTokenPort…
│   │   └── model/AuthException.java  ← Sealed class, 9 sous-types typés
│   │
│   ├── application/         ← Handlers (use cases) : @ApplicationService
│   │   ├── LoginHandler.java
│   │   ├── RegisterHandler.java
│   │   ├── RefreshTokenHandler.java
│   │   ├── LogoutHandler.java
│   │   └── GetCurrentUserHandler.java
│   │
│   └── infrastructure/      ← Adapters (Spring, JPA, JWT, BCrypt…)
│       ├── persistence/      ← Entités JPA, adapters repository
│       ├── security/         ← JwtService, BcryptPasswordHasher, CookieService…
│       └── web/              ← AuthController, LoginRateLimitFilter, DTOs
│
└── infrastructure/
    └── config/              ← SecurityConfig, TransactionConfig, DataSeeder…
```

### Ports & Adapters

| Port (interface domain) | Adapter (infrastructure) |
|---|---|
| `UserRepository` | `UserRepositoryAdapter` + `UserJpaRepository` |
| `RefreshTokenRepository` / `RefreshTokenPort` | `RefreshTokenRepositoryAdapter` |
| `AccessTokenPort` | `JwtService` |
| `PasswordHasherPort` | `BcryptPasswordHasher` |
| `TokenHashPort` | `Sha256TokenHasher` |

### Règles vérifiées automatiquement (ArchUnit)

- Le domaine ne dépend pas de Spring
- L'application ne dépend pas de l'infrastructure
- Les controllers ne dépendent pas de l'application (passent par les ports)
- Tout `@ApplicationService` est dans le package `application`
- Tout handler implémente un port entrant

### Transactions

Les transactions sont gérées par AOP via `TransactionConfig` : toute méthode d'un bean `@ApplicationService` s'exécute dans une transaction. Les `@Transactional` sur les méthodes `@Modifying` des JPA repositories sont conservés — ils sont requis par Spring Data JPA pour les opérations de modification en dehors d'un contexte transactionnel.

### Sécurité

- **JWT** (access token) : HttpOnly cookie, 15 min, HMAC SHA-256
- **Refresh token** : rotation à chaque usage, hash SHA-256 en base, révocation cascade si réutilisation détectée
- **Rate limiting** : 10 tentatives / 60 s / IP via Caffeine (`LoginRateLimitFilter`)
- **Cookies** : HttpOnly, Secure, SameSite=Strict, paths séparés (`/api` vs `/api/auth`)

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
├── pages/            ← Assemblage de features : LoginPage, ProfilePage
├── features/
│   └── auth/
│       ├── api/      ← IAuthApi (interface), authApi.ts (impl)
│       ├── model/    ← authStore.ts (Zustand), useAuth.ts, types.ts
│       ├── ui/       ← LoginForm.tsx
│       ├── locales/  ← Traductions EN/FR
│       └── index.ts  ← Exports publics uniquement
├── entities/
│   └── user/         ← Type UserDTO
└── shared/
    ├── api/          ← client axios, refreshInterceptor
    ├── lib/          ← sessionHint, logoutCallback
    ├── ui/           ← Button, Input, Spinner (composants génériques)
    └── types/
```

### Règles enforced par ESLint (`eslint-plugin-boundaries`)

- Chaque couche ne peut importer que les couches en dessous
- Les imports internes d'un slice (hors `index.ts`) sont interdits depuis un autre slice
- Toute feature exporte via son `index.ts`

### Injection de dépendances

`createAuthStore(api: IAuthApi)` accepte n'importe quelle implémentation de `IAuthApi`. En production, `authApi` (axios). En test, un mock. Aucun composant n'importe `authApi` directement.

### Authentification

```
AuthProvider (app)
  └── initialize()           → GET /api/auth/me (vérification session au reload)

login(credentials)           → POST /api/auth/login → retourne UserDTO + set cookies
logout()                     → POST /api/auth/logout → clear cookies + store

refreshInterceptor (axios)
  └── 401 détecté            → POST /api/auth/refresh → retry requête originale
  └── refresh échoue         → triggerLogout()
```

### Tests

| Fichier | Couverture |
|---|---|
| `authStore.test.ts` | login, logout (fail graceful), initialize (success + failure) |
| `authApi.test.ts` | login (single POST), logout, getMe |
| `refreshInterceptor.test.ts` | retry 401, cascade logout, exclusions /auth/* |
| `sessionHint.test.ts` | persist/clear, localStorage unavailable |
| `ProfilePage.test.tsx` | null user guard, render avec user |
| `Button.test.tsx` | aria-hidden spinner, sr-only label |