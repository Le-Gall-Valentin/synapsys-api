# SynapSys API

> Centralized orchestration API for managing and executing routines on remote servers.

SynapSys is a self-hosted platform that lets you trigger shell scripts and Makefile targets on remote Linux servers through a secure, centralized interface. The API is the control plane: it manages users, applications, routines, permissions, and communicates with agents over an authenticated WebSocket (Ed25519 challenge-response).

---

## Architecture

```
┌─────────────────────────────────┐
│        SynapSys API             │
│   Spring Boot + React/TS UI     │
│                                 │
│  • User & permission management │
│  • Application & routine CRUD   │
│  • Execution history & logs     │
│  • Agent enrollment & registry  │
└────────────┬────────────────────┘
             │  WebSocket (Ed25519 challenge-response)
     ┌───────┴────────┐
     │                │
┌────▼────┐      ┌────▼────┐
│ Agent 1 │      │ Agent 2 │   ...
│ server1 │      │ server2 │
└─────────┘      └─────────┘
```

The API acts as the central hub. Agents ([synapsys-agent](https://github.com/Le-Gall-Valentin/synapsys-agent)) run on each target Linux server and connect back to the API over a WebSocket. There is no PKI/mTLS: each agent is enrolled with a single-use token and authenticates by signing a server-issued nonce with its pinned Ed25519 key (challenge-response). The API can revoke an agent at any time and the live connection is dropped across all instances.

---

## Features

- **User management** — `SUPER_ADMIN`, `ADMIN`, and `USER` roles with fine-grained permission scoping
- **Applications** — logical groupings of routines, with descriptions, statuses, and notes
- **Routines** — shell script (`.sh`) or Makefile targets, tied to specific agents
- **Permissions** — per-application and per-routine access control (launch, edit, delete, status change)
- **Execution history** — global and per-routine logs, filtered by permissions
- **Agent enrollment** — admin-issued single-use enrollment tokens; agents register a pinned Ed25519 public key and authenticate via WebSocket challenge-response (no certificates), with live revocation
- **Real-time streaming** — execution logs streamed live via SSE
- **Two-factor authentication** — optional TOTP/2FA (Google Authenticator, 1Password…), enrollment flow, admin reset

---

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+ (rate limiting, TOTP challenge store, agent presence/challenge store + revocation Pub/Sub)
- Node.js 20+ (for the frontend build)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/synapsys-api.git
cd synapsys-api
```

### 2. Configure the environment

Copy the example environment file and fill in your values:

```bash
cp .env.example .env
```

Key variables:

| Variable | Description |
|---|---|
| `SYNAPSYS_DB_URL` | PostgreSQL JDBC URL |
| `SYNAPSYS_DB_USERNAME` | Database user |
| `SYNAPSYS_DB_PASSWORD` | Database password |
| `SYNAPSYS_JWT_SECRET` | JWT signing secret (min 32 chars) |
| `SYNAPSYS_JWT_EXPIRY_MINUTES` | JWT token validity in minutes (default: 15) |
| `SYNAPSYS_ENCRYPTION_SECRET` | Master secret for TOTP encryption (min 32 chars) |
| `SYNAPSYS_SEED_USERNAME` | Initial super-admin username |
| `SYNAPSYS_SEED_EMAIL` | Initial super-admin email |
| `SYNAPSYS_SEED_PASSWORD` | Initial super-admin password (min 8 chars) |
| `SYNAPSYS_AGENT_TOKEN_VALIDITY_HOURS` | Enrollment token validity in hours (default: 24) |
| `SYNAPSYS_AGENT_CHALLENGE_TTL_SECONDS` | Handshake challenge (nonce) TTL in seconds (default: 30) |
| `SYNAPSYS_AGENT_PRESENCE_TTL_SECONDS` | Agent presence TTL; agent is "down" past this without a heartbeat (default: 90) |
| `SYNAPSYS_AGENT_MAX_CONNECTIONS_PER_IP` | Max concurrent agent WebSocket connections per client IP (default: 10) |
| `SYNAPSYS_AGENT_TOKEN_PREFIX` | Prefix for generated enrollment tokens (default: `synenr_`) |
| `SYNAPSYS_AGENT_WEBSOCKET_PATH` | Agent WebSocket path (default: `/ws/agents`) |

### 3. Run with Docker Compose (recommended)

```bash
docker compose up -d
```

The UI will be available at `http://localhost:8080`.

### 4. Run locally (development)

```bash
mvn spring-boot:run
```

---

## Deployment

A production-ready Docker image is available:

```bash
docker pull ghcr.io/Le-Gall-Valentin/synapsys-api:latest
```

See [docs/deployment.md](docs/deployment.md) for full deployment instructions including HTTPS, reverse proxy setup, and database migrations.

---

## Agent Setup

To connect a server to the API, deploy the SynapSys agent:
👉 [synapsys-agent](https://github.com/Le-Gall-Valentin/synapsys-agent)

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a pull request.

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
