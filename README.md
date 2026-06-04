# LoyalSuit

> The all-in-one platform to run, grow, and scale your commerce business — multi-vendor marketplace, POS, inventory, and back-office, on one subscription.

LoyalSuit is a multi-tenant commerce SaaS that combines a marketplace engine
(multi-vendor, orders, payments) with business operations (inventory, POS, HRM).
Each tenant runs their own store and an optional marketplace.

## Tech Stack

| Layer    | Technology |
|----------|------------|
| Backend  | Spring Boot 3.4 · Java 21 · Maven |
| Frontend | Next.js 15 (App Router) · TypeScript · Tailwind + DaisyUI |
| Database | PostgreSQL (Supabase-hosted) · Flyway migrations |
| Cache    | Redis |
| Auth     | Self-contained — backend issues & validates its own JWT (bcrypt) |
| Files    | Cloudinary |
| Docs     | OpenAPI / Swagger UI |

## Architecture

Modular monolith with a hexagonal (ports & adapters) structure per bounded
context. Each module under `backend/src/main/java/com/loyalsuit/modules/<module>`:

```
domain/          entities + repository ports (interfaces)
application/     services + DTOs
infrastructure/  JPA / JDBC adapters (implement the ports)
api/             REST controllers
```

All tenant-scoped data carries `tenant_id`. The JWT carries the tenant, resolved
per-request into `TenantContext`. Authorization is always enforced server-side;
the client is never trusted.

## Monorepo Layout

```
backend/    Spring Boot API
frontend/   Next.js app (admin, seller, storefront, POS)
PHASES.md   Roadmap, cross-cutting foundations, and current status
Makefile    Developer commands
```

## Prerequisites

- Java 21, Maven 3.9+
- Node.js 20+, npm
- A PostgreSQL database and a Redis instance (hosted is fine)

## Setup

```bash
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env.local
```

Fill in `backend/.env`:
- `POSTGRES_*`, `REDIS_*`
- `APP_JWT_SECRET` (use a long random value, ≥ 32 chars)
- `CLOUDINARY_*`
- `EMAIL_*` (SMTP — transactional mail; for Gmail use an App Password). Optional in
  dev: if unset, email is skipped (logged) and nothing breaks.

> Secrets are environment-only and git-ignored. Never commit `.env` files.
> Rotate any secret that is exposed.

## Running

```bash
make backend     # Spring Boot on :8080 (dev profile: runs migrations + seeds dev users)
make frontend    # Next.js on :3000
make dev         # both at once
make health      # backend health check
make smoke-login # log in as the seeded super admin and print the JWT
```

## Test Accounts (dev profile only)

Seeded into Postgres on first backend start. Password: `Admin@Test123`

| Role         | Email                      |
|--------------|----------------------------|
| Super Admin  | superadmin@loyalsuit.dev   |
| Tenant Admin | tenantadmin@loyalsuit.dev  |
| Vendor       | vendor@loyalsuit.dev       |
| Customer     | customer@loyalsuit.dev     |
| Staff        | staff@loyalsuit.dev        |

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Testing

```bash
make backend-test    # JUnit + Mockito (unit) + Spring contextLoads
make frontend-lint   # ESLint + tsc
make lint            # both
```

## Security

- Self-hosted JWT auth (HMAC-signed), bcrypt password hashing.
- Change-password with current-password verification, complexity rules, reuse check.
- Forgot-password / reset: single-use, time-limited tokens stored only as SHA-256
  hashes; no account enumeration; reset + confirmation emails.
- Tenant-scoped audit log of security/account activity (logins incl. failures,
  password changes/resets, onboarding); owner-only, append-only.
- Per-request trace IDs and structured logging (`logback-spring.xml`); no secrets logged.
- Roadmap: RLS/tenant filter, permission-based RBAC, audit log, 2FA, rate limiting.

## Roadmap

See [PHASES.md](PHASES.md) for the phased plan, cross-cutting engineering
foundations, and per-phase definitions of done.

## License

Proprietary — © LoyalSuit. All rights reserved.
