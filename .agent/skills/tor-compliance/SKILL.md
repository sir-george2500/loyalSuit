---
name: tor-compliance
description: Enforces strict compliance with the ProcuraX Terms of Reference (TOR) before implementing any feature, route, or business logic change. Use this skill at the START of every implementation task. Failure to check TOR compliance before coding is a critical process violation.
---

# ProcuraX TOR Compliance Enforcement

## ⚠️ Mandatory Pre-Implementation Checklist

Before writing a **single line of code**, you MUST answer all of the following questions. If any answer is "No" or "Unknown", STOP and re-read `USER_STORIES.md` and `TECHNICAL_DESIGN.md` before proceeding.

```
[ ] Does this feature exist in USER_STORIES.md or TECHNICAL_DESIGN.md?
[ ] Does this feature serve one of the 5 defined system personas?
[ ] Does the access control match the persona's defined role?
[ ] Does this introduce any public endpoint that should be restricted?
[ ] Does this violate any of the Absolute Prohibitions listed below?
```

---

## 👥 The 5 Authorized Personas (Non-Negotiable)

These are the **only** people who should interact with ProcuraX. Every route, button, and API endpoint must be traceable to exactly one of these personas.

| #   | Persona                               | Role Token       | How They Gain Access                                                         |
| --- | ------------------------------------- | ---------------- | ---------------------------------------------------------------------------- |
| 1   | **Vendor** (External Contractor)      | `ROLE_VENDOR`    | Self-registers via `/vendor-onboarding` (KYC pipeline)                       |
| 2   | **System Administrator** (CBL IT)     | `ROLE_ADMIN`     | Manually seeded in DB migration. **Never self-registers.**                   |
| 3   | **Procurement Officer** (CBL Staff)   | `ROLE_STAFF`     | **Provisioned by Admin only.** Receives invite email with set-password link. |
| 4   | **Evaluation Committee** (CBL Staff)  | `ROLE_EVALUATOR` | **Provisioned by Admin only.** Assigned to specific tenders.                 |
| 5   | **Security Auditor** (CBL Compliance) | `ROLE_AUDITOR`   | **Provisioned by Admin only.** Read-only access.                             |

---

## 🚫 Absolute Prohibitions (Hardcoded TOR Rules)

These are non-negotiable constraints derived directly from the CBL RFP. Violating any of these is a **critical defect**, not a suggestion.

### 1. No Public Self-Registration for Internal Roles

> **PROHIBITED:** Any public route that creates `ROLE_STAFF`, `ROLE_EVALUATOR`, `ROLE_ADMIN`, or `ROLE_AUDITOR` accounts without Admin authorization.

```
✗ WRONG: Public /register page creates ROLE_STAFF automatically
✓ RIGHT: Admin provisions staff via admin panel → invite email sent → user sets password
```

### 2. No Evaluation Access Before Bid Deadline

> **PROHIBITED:** Evaluation Committee members must not be able to view, access, or download any bid until the tender's `submissionDeadline` has passed.

```
✗ WRONG: EvaluationController returns bids without checking deadline
✓ RIGHT: Backend enforces: if (Instant.now().isBefore(tender.getDeadline())) throw 403
```

### 3. No Admin Access to Bids or Tender Launching

> **PROHIBITED:** The System Administrator must never be able to open bids, score vendors, or launch RFPs. Admin scope = software configuration only.

```
✗ WRONG: Admin role has access to /evaluations or /tenders/publish
✓ RIGHT: ROLE_ADMIN is blocked via @PreAuthorize from all tender/bid endpoints
```

### 4. No Vendor Access to Internal Dashboard

> **PROHIBITED:** Vendors must only access their own profile, public tender listings, and bid submission. They cannot see the procurement dashboard, plans, or other vendors.

```
✗ WRONG: Vendor JWT can access /dashboard, /planning, /vendors
✓ RIGHT: Middleware blocks ROLE_VENDOR from all internal dashboard routes
```

### 5. Immutable Audit Trail

> **PROHIBITED:** No action affecting bids, tenders, purchase orders, or vendor approvals may skip audit logging. All such mutations must pass through `GlobalAuditInterceptor`.

```
✗ WRONG: Direct repository.save() calls without audit event
✓ RIGHT: Spring Modulith events trigger audit persistence on all state changes
```

---

## 🔐 Route → Persona Mapping (Enforce Before Adding Any Route)

When adding a new route or page, you MUST place it in this table first:

| Route                                 | Allowed Personas                          | Blocked Personas               |
| ------------------------------------- | ----------------------------------------- | ------------------------------ |
| `/vendor-onboarding`                  | Unauthenticated (public)                  | N/A                            |
| `/login`, `/forgot-password`          | Unauthenticated (public)                  | N/A                            |
| `/dashboard`, `/tenders`, `/planning` | `ROLE_STAFF`, `ROLE_ADMIN`                | `ROLE_VENDOR`, unauthenticated |
| `/vendors`                            | `ROLE_STAFF`, `ROLE_ADMIN`                | `ROLE_VENDOR`                  |
| `/evaluations`                        | `ROLE_EVALUATOR`                          | ALL others                     |
| `/settings`                           | `ROLE_ADMIN` only                         | ALL others                     |
| `/documents`                          | `ROLE_STAFF`, `ROLE_ADMIN`                | `ROLE_VENDOR`                  |
| **`/register`**                       | **REMOVED — Admin provisions internally** | **Everyone**                   |

---

## 📋 Pre-Merge TOR Compliance Verification

Before every PR or commit touching auth, routing, or access control:

```markdown
### TOR Compliance Check

- [ ] Feature is documented in USER_STORIES.md
- [ ] Access controller uses correct @PreAuthorize annotation
- [ ] No new public endpoint creates ROLE_STAFF/ADMIN/EVALUATOR/AUDITOR
- [ ] Evaluation endpoints block access before tender deadline
- [ ] Admin endpoints block access to bid/tender data
- [ ] Vendor endpoints block access to internal dashboard
- [ ] All state-changing mutations produce an audit log entry
- [ ] Route is added to the Persona Mapping table above
```

---

## 🔄 How to Use This Skill

**At the start of every feature task:**

1. Open `USER_STORIES.md` and find the user story this feature implements
2. Identify which of the 5 personas it serves
3. Check the Absolute Prohibitions — does your implementation violate any?
4. Map the route(s) to the persona table
5. Only then start writing code

**When reviewing code (your own or another agent's):**

1. Run through the Absolute Prohibitions checklist
2. If any prohibition is violated, mark the change as **BLOCKED — TOR Violation**
3. Do not approve or merge until remediated

---

## Common TOR Violations Found in This Codebase (Historical)

| Public `/register` created `ROLE_STAFF` for anyone            | `AuthService.java:63`            | ✅ Fixed     |
| `/vendor-register` URL in invite email (route does not exist) | `VendorRegistrationService.java` | ✅ Fixed     |
| Hardcoded $5,200,000 dummy budget on dashboard                | `DashboardController.java`       | ✅ Fixed     |
| Settings page visible to non-Admin roles                      | `middleware.ts`                  | ✅ Enforced  |

---

## References

- [USER_STORIES.md](file:///home/delta-x/procuraX/USER_STORIES.md) — Definitive persona and feature source of truth
- [TECHNICAL_DESIGN.md](file:///home/delta-x/procuraX/TECHNICAL_DESIGN.md) — Architecture decisions
- [middleware.ts](file:///home/delta-x/procuraX/frontend/src/middleware.ts) — Route protection enforcement
