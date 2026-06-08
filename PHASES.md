# LoyalSuit — Engineering Roadmap

> A multi-vendor commerce + POS SaaS. Single Next.js frontend, single Spring Boot
> modular-monolith backend, PostgreSQL, Redis, Cloudinary. Self-hosted identity
> (the platform issues its own JWTs). This document is the source of truth for
> *what* we build, *in what order*, and *what "done" means* for each slice.

---

## 0. Guiding principles

1. **Vertical slices, not horizontal layers.** Every phase ships a feature that a
   real user can exercise end-to-end (UI → API → DB), not a half-built layer.
2. **Multi-tenancy is non-negotiable.** Every domain row is tenant-scoped; every
   query filters by `tenant_id`; the JWT carries the tenant. No cross-tenant leak.
3. **Security is a column in every phase, not a phase at the end.** AuthZ on every
   endpoint, input validation at every boundary, secrets never logged or committed.
4. **Production-grade only.** No mock data behind real-looking UI, no broken links,
   no "TODO later" in shipped paths. Unbuilt scope is visibly marked, never faked.
5. **Tested before merged.** Unit tests for logic, integration tests for boundaries,
   the Spring context must boot. A red build blocks everything.

---

## 1. Bounded contexts (modules)

The backend is a modular monolith. Each module owns its tables; no module reaches
into another's tables — they collaborate through application services / events.

| Context | Owns | Status |
|---|---|---|
| **identity** (`auth`, `users`) | accounts, credentials, JWT issuance, sessions | ✅ live |
| **tenancy** (`tenants`) | tenant lifecycle, subscription tier, feature gates | 🟡 onboarding live; feature gates pending |
| **catalog** | products, variants, categories, media | 🟡 categories live (tenant-safe tree); products WIP |
| **inventory** | warehouses, stock, transfers, low-stock alerts | 🟡 schema only |
| **marketplace** | vendors, commissions, payouts | 🟡 entity only |
| **orders** | cart, orders, order items, returns | 🟡 schema only |
| **payments** | gateways, transactions, webhooks, refunds | ⬜ schema only |
| **dashboard/reporting** | tenant analytics, KPIs | ✅ live (read model) |
| **fulfilment** | delivery agents, pickup points, tracking | ⬜ planned |
| **hrm** | employees, attendance, payroll | ⬜ planned |
| **marketing** | coupons, flash deals, loyalty, affiliate | ⬜ planned |
| **notifications** | email, SMS, push, in-app | 🟡 transactional email live (onboarding, password reset) |

---

## 2. Cross-cutting foundations (continuous, not a phase)

These are built up incrementally and must never regress:

- **Observability** — structured logging w/ per-request trace IDs ✅; audit log ✅
  (tenant-scoped, append-only); metrics (Micrometer) ⬜; error tracking (Sentry) ⬜.
- **Security** — JWT auth ✅; method-level RBAC (`@PreAuthorize` on all endpoints,
  role-matrix tested) ✅; rate limiting ⬜;
  2FA ⬜; secret management ⬜; OWASP pass before GA ⬜.
- **Data** — Flyway migrations ✅; tenant-scoped base entities ✅; soft-delete &
  audit columns ✅ (created/updated); RLS-style guarantees enforced in repo layer.
- **Quality gates** — Checkstyle + SpotBugs (BE) ✅; ESLint + tsc (FE) ✅;
  CI pipeline (GitHub Actions) ⬜; coverage threshold ⬜.
- **Caching** — Redis wired ✅ (currently health-decoupled); cache-aside for
  catalog/permissions/settings ⬜ (re-introduce with proper DTO serialization).

---

## Phase 0 — Platform skeleton ✅ COMPLETE
**Exit criteria:** backend boots against real Postgres, frontend builds, both
talk to each other, CI-able locally.

- [x] Spring Boot 3.4 / Java 21 modular-monolith scaffold (hexagonal per module)
- [x] Next.js 15 (App Router, TS, DaisyUI) frontend
- [x] PostgreSQL (Supabase) + Flyway (V1–V6 applied & verified)
- [x] Redis configured (optional cache, health-decoupled)
- [x] Structured logging: rolling files + per-request trace IDs (`logback-spring.xml`)
- [x] Global exception handling, `ApiResponse` envelope, pagination contract
- [x] `Makefile`, lint/format tooling, `.env` hygiene

---

## Phase 1 — Identity, tenancy & the admin shell ✅ COMPLETE
**Goal:** a real account can sign up (provisioning a tenant), sign in, manage its
own security, and land in a role-appropriate admin shell with live analytics.
**Exit criteria:** all 6 roles route correctly; password lifecycle works; dashboard
reflects real tenant data; zero cross-tenant leakage in any Phase-1 endpoint.

- [x] Self-contained auth — backend issues/validates its own JWT, bcrypt creds
- [x] Register (provisions tenant + TENANT_ADMIN), login, `/me`, change-password (tested)
- [x] Frontend ↔ backend wired (axios Bearer, CORS verified, cookie session)
- [x] Cookie + middleware route protection; `AuthHydrator` rehydrates from `/me`
- [x] Role-based, sectioned admin navigation (SUPER_ADMIN ≠ TENANT_ADMIN ≠ STAFF)
- [x] Settings area (General + Security tabs); session introspection
- [x] DaisyUI design system (`loyalsuit` theme)
- [x] **Deep dashboard read-model** — period-over-period KPIs, 14-day revenue trend,
      order-status breakdown, recent orders, low-stock — all real tenant-scoped SQL
- [x] **Method-level RBAC** — `@PreAuthorize` on every controller endpoint
      (dashboard, products, categories); auth endpoints scoped correctly
      (register/login public, `/me` + change-password any authenticated). Three
      privilege tiers — read/create (store roles + VENDOR), store-write (no VENDOR),
      admin-only (delete). Role-matrix integration tests mint real JWTs and assert
      all 6 roles against each endpoint (67 cells), so any unguarded route or
      widened role set fails the build.
- [x] **Role-based routing & page-level authZ** — single source of truth
      (`lib/auth/roles.ts`); enforced at three layers: edge middleware, server-side
      layout guards (`guardArea`), and post-login redirect (`homeForRole`). A role
      can never enter another's shell (CUSTOMER → /store, VENDOR → /seller, admin → /admin).
- [x] Seller dashboard shell (VENDOR landing) — DaisyUI shell, role-scoped nav,
      honest onboarding state (no fabricated metrics; marketplace data is Phase 5)
- [x] **Tenant onboarding wizard** (company → localization → first warehouse) —
      one-time, idempotent-by-rejection (`tenants.onboarded_at`); provisions a
      default warehouse; server-gated so owners can't reach the admin shell until
      complete. Multi-step validated UI; backend role-matrix + service tested.
- [x] **Transactional email infrastructure** — `EmailService` (best-effort, no-op
      when SMTP absent), `@Async` after-commit delivery; welcome email on onboarding.
      Unblocks password-reset.
- [x] **Forgot-password / reset (email token)** — random 256-bit token, only its
      SHA-256 hash stored, single-use + time-limited (30 min), prior tokens
      invalidated on reissue. No account enumeration; generic failures. Public
      endpoints; reset + confirmation emails sent after-commit. Service + API tested.
- [x] **Audit log (who did what, when)** — append-only, tenant-scoped trail.
      Each write runs in its own transaction (`REQUIRES_NEW`) and is best-effort, so
      **failed attempts and rolled-back operations are still recorded** (e.g. bad
      logins). Captures actor, action, outcome, resource, IP + user-agent. Wired into
      register / login (success + failure) / change-password / password-reset /
      onboarding. Owner-only read endpoint + admin UI (filter, pagination). Tested.

---

## Phase 2 — Catalog & inventory (the sellable core) 🔄 IN PROGRESS
**Goal:** an admin/vendor can build a real catalog and track stock across warehouses.
**Exit criteria:** product lifecycle (draft→active→archived) works; stock decrements
are correct under concurrency; storefront can read published products.

> **Pre-Phase-2 review found** (now being addressed slice by slice): catalog
> writes accepted `parentId`/`categoryId`/`vendorId` without verifying same-tenant
> ownership (cross-tenant reference leak); no category cycle prevention; `update`
> ignored `parentId`; delete silently orphaned children/products; and stock is
> modeled twice (`product_variants.stock_quantity` vs the `stock` table) — to be
> consolidated in the multi-warehouse slice with `@Version` optimistic locking.

- [x] **Category tree CRUD (UI + API)** — slug uniqueness per tenant; **tenant-safe
      parent validation** (same-tenant existence) with **cycle prevention**;
      re-parenting; delete refuses to orphan (blocks when subcategories/products
      exist). Admin tree UI (create/edit/delete). Service tested (12 cases, AAA).
- [x] **Product CRUD** (create/update/delete, status lifecycle
      draft→active→inactive→archived), pricing, SKU/barcode, digital flag.
      **Security-hardened**: `categoryId` validated as same-tenant; `vendorId`
      derived from the authenticated principal (never trusted from the client).
      Admin UI with status actions + pagination. Service + role-matrix tested.
- [x] **Product variants** (sub-slice 2b): per-variant name/SKU/price CRUD nested
      under a product. Tenant-safe — every op verifies the parent product belongs
      to the caller's tenant, and update/delete verify the variant belongs to that
      product (NotFound, not Forbidden, to avoid leaking existence). Variants modal
      in the product UI. Service + role-matrix tested. Stock stays out (slice 4).
- [x] **Media upload pipeline** (Cloudinary) — server-proxied so no credential or
      signature ever reaches the client. Uploads validated by **magic bytes** (not
      the client MIME type), size-capped at the multipart boundary *and* in the
      service, stored in per-tenant/per-product folders. Product image gallery
      (primary + ordering, auto-promote on delete). Mockable `MediaStorage` port;
      service + role-matrix tested. 413 handler for oversize files.
- [x] **Warehouse management** (slice 4a) — CRUD for tenant stock locations;
      single-default invariant (first is auto-default; promoting one demotes the
      rest); delete refuses to remove the default or the last warehouse. Admin UI;
      service + role-matrix tested.
- [x] **Stock levels + atomic adjustments** (slice 4b) — per-warehouse/variant
      quantities. Absolute `setLevel` uses `@Version` optimistic locking (concurrent
      edit → 409); delta `adjust` uses an atomic conditional UPDATE that is
      lost-update safe and cannot go negative. Consolidated the dual stock model
      (dropped `product_variants.stock_quantity`); added the warehouse-has-stock
      delete guard; low-stock thresholds + `/stock/low` query. Per-product Stock UI
      (set level + ± adjust). Service + role-matrix tested.
- [ ] Surface low-stock on the dashboard + (later) notifications
- [ ] Bulk CSV import/export with row-level validation + error report
- [x] **Public storefront read API** (`/store/{slug}/**`) — anonymous, resolves the
      active tenant by slug. Exposes only ACTIVE products / active categories and a
      customer-facing DTO (no SKU/barcode/status/vendor; stock as an `inStock`
      boolean, never quantities). Batch lookups avoid N+1 in listings. Next.js
      Server Components render the storefront (store home + product detail) with
      ISR caching. Read-model aggregator (`storefront` module) + public-access tests.
      (Host/subdomain resolution and server-side cache-aside deferred.)
- [ ] List/detail performance: pagination, indexes, cache-aside for hot listings

---

## Phase 3 — Orders, cart & checkout ✅ COMPLETE (cash-only; gateways deferred)
**Goal:** a customer can buy; an admin can fulfil.
**Exit criteria:** an order cannot be placed for out-of-stock items; totals are
recomputed server-side (never trusted from client); state machine is enforced.

> **Payment is cash-only for now** (Cash on Delivery / pay-on-pickup) — no online
> gateway is available yet. Checkout places the order immediately with
> `payment_method = CASH` and `payment_status = UNPAID`; an admin marks it paid when
> the money is collected. Online gateways (Phase 4) are deferred until available;
> the order already carries `payment_method`/`payment_status` so adding gateways
> later is additive, not a rewrite. Guest checkout (no account) is supported, which
> suits a local cash shop.

- [x] **Cart (Redis-backed)** (slice 3a) — anonymous, store resolved by slug, cart
      by an opaque `X-Cart-Token`. Stores only product/variant refs + quantities;
      **line prices are always recomputed server-side** from current catalog (a
      stale/tampered cart can't set the price). Only ACTIVE products addable; invalid
      lines dropped on view. Storefront add-to-cart + cart page. Service + public-
      access tested. (Guest→authenticated merge deferred to the auth-integration step.)
- [x] **Cash checkout** (slice 3b) — guest contact + shipping address; totals
      recomputed server-side from the cart; **atomic stock reservation** (decrements
      the default warehouse via the lost-update-safe `applyDelta`; a shortfall rolls
      back the whole checkout → no partial orders, can't oversell); **idempotent**
      order creation (idempotency key: DB pre-check + partial unique index). Order
      placed `PENDING` / `CASH` / `UNPAID`; cart cleared. Storefront checkout form +
      cash-on-delivery confirmation. Service + public-access tested.
      (Shipping-cost selection and multi-warehouse allocation deferred.)
- [x] **Order state machine + admin management** (slice 3c) — guarded fulfilment
      transitions (`canTransitionTo`; invalid edges rejected); **stock released on
      cancel** via a compensating `applyDelta` in the same transaction; admin marks
      cash received (`UNPAID → PAID`, idempotent). Admin orders UI: list + status
      filter, detail with valid-transition actions. Store-role gated; service +
      role-matrix tested.
- [x] **Guest order tracking** — enumeration-safe public lookup by order number +
      the email used at checkout (any mismatch → generic 404, so order numbers can't
      be probed). Storefront "track order" page. Service tested.
      (Authenticated-account order history deferred — needs the customer-account /
      storefront-login integration.)
- [x] **Returns / refunds** (slice 3d) — guest requests a return on a DELIVERED
      order (enumeration-safe, order number + email); admin approves (order +
      payment → `REFUNDED`, items restocked, all in one transaction) or rejects with
      a note. Duplicate-pending and double-resolve guarded; concurrent approval
      blocked by the order's `@Version`. Storefront request UI + admin returns page.
      Service + role-matrix tested.

**Deferred from Phase 3:** authenticated customer-account order history (needs
storefront customer login), shipping-cost selection, multi-warehouse allocation.

---

## Phase 4 — Payments (DEFERRED — no gateway available yet)
**Goal:** money moves, safely.
**Status:** Online payment gateways are deferred until one is available. For now
the platform is **cash-only**: orders are placed with `payment_method = CASH` /
`payment_status = UNPAID` in Phase 3, and an admin records payment on collection.
The order schema already carries `payment_method`/`payment_status`, so introducing
gateways later is additive.

**Exit criteria (when resumed):** every gateway path is idempotent and
webhook-verified; no order is marked paid without a verified gateway event;
refunds reconcile.

- [x] Cash on Delivery / pay-on-pickup (recorded on the order) — done in Phase 3
- [ ] PaymentService abstraction over gateways (Strategy pattern) — when a gateway exists
- [ ] Stripe (cards) sandbox → live; PayPal; manual bank transfer
- [ ] Signed webhook handlers (idempotency keys, replay protection)
- [ ] Payment ↔ order reconciliation; partial captures/refunds
- [ ] Tenant subscription billing (plan tiers gate features in `tenancy`)

---

## Phase 5 — Marketplace (multi-vendor) ✅ COMPLETE
**Goal:** third-party sellers operate inside a tenant's store.
**Exit criteria:** vendor data is isolated; commission math is auditable; payouts
never exceed settled balance.

- [x] **Vendor onboarding + admin approval** (slice 5a) — a signed-in user applies
      to sell (one application per user); admin runs a guarded status machine
      (PENDING→ACTIVE/REJECTED, ACTIVE↔SUSPENDED). **Approval grants the VENDOR role**
      (admin/owner-only, tenant-safe — only a user of the same tenant is elevated);
      admin can set the per-vendor commission rate. Admin vendors UI + a "become a
      seller" application page. Service + role-matrix tested.
- [x] **Vendor-scoped product views** (slice 5b) — vendors create/manage only
      their own products (list/get/update/publish/unpublish/archive scoped to
      `vendorId`; the service 404s a vendor on another vendor's product). Active
      status is enforced on selling — a suspended/pending vendor is blocked with
      "Your vendor account is not active". Ownership is derived from the JWT
      principal (never client-supplied). Seller products UI; service + role-matrix
      tested. _Deferred: vendor order views; vendor management of variants/media/
      stock sub-resources (still STORE_WRITE-only); suspend doesn't yet revoke the
      VENDOR role._
- [x] **Commission engine + ledger** (slice 5c) — commission is earned when an
      order's cash is collected (marked paid) and reversed if the order is later
      refunded. Each order line snapshots its selling vendor at checkout; settlement
      writes one auditable ledger entry per vendor line, snapshotting the rate and
      the computed commission/net (HALF_UP to cents). Settlement and reversal are
      both idempotent (unique on order_item; settle skips an already-settled order).
      A vendor's owed balance is the sum of EARNED net. Vendor earnings + ledger UI
      (VENDOR-only) and an owner-only tenant-wide ledger UI. Service + role-matrix
      tested. _Deferred: per-category commission rates (per-vendor only for now)._
- [x] **Payout requests + audit trail** (slice 5d) — a vendor requests a payout up
      to their available balance (earned net, less pending + paid payouts); an admin
      pays it (cash, with a reference) or rejects it with a reason. **Payouts can never
      exceed settled balance**: the request is validated against available, capped at
      one pending request per vendor (partial unique index as the concurrency
      backstop), and the pay/reject decision is version-guarded against double-payment.
      Every action (request/pay/reject) is written to the audit log. Seller payouts UI
      (balance + request + history) and an owner-only admin review UI. Service +
      role-matrix tested. Added a DataIntegrityViolation→409 handler.

**Phase 5 exit criteria met:** vendor data is isolated (5b), commission math is
auditable (5c), payouts never exceed settled balance (5d).

---

## Phase 6 — POS terminal
**Goal:** in-store sales, including offline.
**Exit criteria:** an offline sale syncs exactly once on reconnect; cash drawer
reconciles; receipts are correct.

- [x] POS catalog search + cart, barcode scanning _(slice 6a)_
- [~] Split / cash / card tender; change calc — cash tender + change done _(slice 6a)_; card/split deferred
- [x] Offline queue (IndexedDB) with idempotent sync _(slice 6b)_
- [ ] Shift open/close + cash reconciliation; receipt (PDF)

_Slice 6a (cash sale foundation): a cashier searches/scans the catalog, builds a cart,
takes cash, and completes a sale that reuses the proven commerce machinery — a paid,
delivered order (single ledger), atomic stock decrement, and vendor commission settle —
all idempotent on a terminal-supplied `clientSaleId` (the seam offline sync builds on)._

_Slice 6b (offline queue + idempotent sync): sales rung up offline (or whose network
call drops mid-flight) persist to IndexedDB and sync automatically on reconnect. The
stable `clientSaleId` + the server's unique index guarantee exactly-once — a 409 on
replay is treated as already-synced. Transient failures (offline/5xx) retry; hard 4xx
(e.g. stock changed) are flagged for the cashier to retry/discard, never silently dropped.
Status bar shows online/offline, queued count, and sync state._

---

## Phase 7 — Fulfilment & delivery
- [ ] Delivery agent management, assignment, pickup points, carrier zones
- [ ] Real-time order tracking; proof of delivery

## Phase 8 — Marketing, loyalty & engagement
- [ ] Coupons, flash deals; loyalty points; affiliate program
- [ ] Notifications (email/SMS/push/in-app) — also backfills password-reset, alerts

## Phase 9 — HRM
- [ ] Employees, attendance, leave, payroll, awards (feature-gated by plan)

---

## Phase 10 — Hardening & GA
**Exit criteria:** OWASP Top-10 reviewed; 60%+ coverage on auth/payments/orders;
load-tested; one-command reproducible deploy; runbook written.

- [ ] Rate limiting, 2FA, API key rotation, secret manager
- [ ] Sentry + metrics dashboards + health/readiness probes
- [ ] Dockerized prod images, GitHub Actions CI/CD (lint→test→build→deploy)
- [ ] Automated daily backups + tested restore; CDN; DB index/query audit
- [ ] GDPR data export/delete; cookie consent

---

## Test accounts (Phase 1, dev only)

Seeded into Postgres by `DevDataSeeder` (idempotent, `dev` profile). All belong to
the "Demo Store" tenant; password **`Admin@Test123`**.

| Role | Email |
|---|---|
| Super Admin | superadmin@loyalsuit.dev |
| Tenant Admin | tenantadmin@loyalsuit.dev |
| Vendor | vendor@loyalsuit.dev |
| Customer | customer@loyalsuit.dev |
| Staff | staff@loyalsuit.dev |

## Running locally

```bash
make backend     # Spring Boot :8080 (dev profile, applies migrations + seeds)
make frontend    # Next.js :3000
make dev         # both
make health      # backend health
make smoke-login # login as superadmin, print JWT
```
