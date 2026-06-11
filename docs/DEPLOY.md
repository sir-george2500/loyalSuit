# Deploy runbook

LoyalSuit ships as two stateless container images — **backend** (Spring Boot) and
**frontend** (Next.js standalone). Postgres and Redis are **managed externally** (the
reference setup uses Supabase Postgres + RedisLabs). Any Docker host can run the stack;
this runbook uses Docker Compose, but the same images run on Fly.io, Render, ECS, Cloud
Run, or a Kubernetes cluster.

---

## 1. Prerequisites

- A reachable **Postgres** database and **Redis** instance.
- A Docker host (or a platform that runs OCI images).
- DNS for the frontend (`your-domain.com`) and backend API (`api.your-domain.com`),
  ideally behind TLS (a reverse proxy such as Caddy/Nginx/Traefik, or the platform's LB).

## 2. Configure

Copy `.env.example` to `.env` at the repo root and fill in every value:

```bash
cp .env.example .env
$EDITOR .env
```

Key points:
- **`APP_JWT_SECRET`** must be a long random string (≥ 32 chars). Rotating it invalidates
  all existing sessions.
- **`NEXT_PUBLIC_API_URL`** is **baked into the browser bundle at build time** — it must be
  the *public* backend URL, not the in-network `backend` hostname. (CI passes it from the
  `NEXT_PUBLIC_API_URL` repo **variable**; Compose passes it as a build arg.)
- The backend runs with **no Spring profile** in production: Flyway migrations apply
  automatically and the dev data seeder does **not** run.

## 3. Database migrations

Flyway runs on backend startup and brings the schema up to the latest `V__` migration.
No manual step is required. Migrations are forward-only; never edit an applied migration —
add a new one.

## 4. Run

### Option A — build locally

```bash
docker compose up -d --build
```

### Option B — pull pre-built images (published by CI to GHCR)

```bash
docker compose pull && docker compose up -d
```

CI publishes `ghcr.io/<owner>/loyalsuit-backend` and `…-frontend` on every push to `main`
(tagged `latest` + the commit SHA) and on `v*` tags.

## 5. Verify

```bash
# Backend readiness (must be UP before serving traffic):
curl -fsS http://<host>:8080/actuator/health/readiness

# Frontend:
curl -fsS http://<host>:3000 | head -c 200
```

Compose healthchecks gate the frontend on a healthy backend; `docker compose ps` shows
each service's health.

## 6. Smoke test

```bash
curl -s -X POST http://<host>:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<an-admin-email>","password":"<password>"}'
```

## 7. Upgrade

```bash
docker compose pull          # or: git pull && docker compose build
docker compose up -d         # recreates changed services; migrations run on start
```

The app is stateless, so a rolling restart is safe once the new backend reports
`readiness: UP`.

## 8. Rollback

Images are tagged by commit SHA, so roll back by pinning the previous tag:

```bash
docker compose pull
IMAGE_TAG=<previous-sha> docker compose up -d   # or edit the image tag in compose
```

Because migrations are forward-only, a rollback of code is safe **as long as the older
code tolerates the newer schema** (the standard expand/contract rule — ship additive
migrations, remove columns a release later).

## 9. Operational notes

- **Rate limiting** (10a) is in-memory and per-instance. Behind multiple replicas, limits
  apply per replica; back it with Redis for a shared view if you scale out.
- **Health probes**: `/actuator/health/liveness` and `/actuator/health/readiness` are
  public and suitable for a load balancer / Kubernetes probes.
- **Secrets** live only in the environment — never bake them into an image. `.env` is
  git-ignored.
