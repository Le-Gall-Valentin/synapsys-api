# SynapSys API

> Centralized orchestration API for managing and executing routines on remote servers.

SynapSys is a self-hosted platform that lets you trigger shell scripts and Makefile targets on remote Linux servers through a secure, centralized interface. The API is the control plane: it manages users, applications, routines, permissions, and communicates with agents via mTLS WebSocket.

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
│  • Internal CA (mTLS)           │
└────────────┬────────────────────┘
             │  WebSocket (mTLS)
     ┌───────┴────────┐
     │                │
┌────▼────┐      ┌────▼────┐
│ Agent 1 │      │ Agent 2 │   ...
│ server1 │      │ server2 │
└─────────┘      └─────────┘
```

The API acts as the central hub. Agents ([synapsys-agent](https://github.com/Le-Gall-Valentin/synapsys-agent)) run on each target Linux server and connect back to the API via a secure, mutually authenticated WebSocket connection.

---

## Features

- **User management** — `ADMIN` and `USER` roles with fine-grained permission scoping
- **Applications** — logical groupings of routines, with descriptions, statuses, and notes
- **Routines** — shell script (`.sh`) or Makefile targets, tied to specific agents
- **Permissions** — per-application and per-routine access control (launch, edit, delete, status change)
- **Execution history** — global and per-routine logs, filtered by permissions
- **Agent enrollment** — admin-approved onboarding with mTLS certificate issuance
- **Real-time streaming** — execution logs streamed live via SSE

---

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
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
| `SYNAPSYS_JWT_SECRET` | JWT signing secret (min 256 bits) |
| `SYNAPSYS_JWT_EXPIRY_HOURS` | JWT token validity in hours |
| `SYNAPSYS_CA_KEYSTORE_PATH` | Path to the internal CA keystore |
| `SYNAPSYS_CA_KEYSTORE_PASSWORD` | CA keystore password |
| `SYNAPSYS_ENROLLMENT_TOKEN_TTL_MINUTES` | Enrollment token validity |
| `SYNAPSYS_SEED_PASSWORD` | Initial admin password — required when `SYNAPSYS_SEED_ENABLED=true` |

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
