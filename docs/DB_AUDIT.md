# Database index & query audit (Phase 10e)

A GA-readiness pass over the schema and the application's query patterns. Goal: every hot
path is index-backed and tenant-scoped, and no accidental table scans or N+1 loops ship.

## Method

1. Enumerated every Spring Data derived query and `@Query` across the modules.
2. For each, identified the columns in its `WHERE` / `ORDER BY`.
3. Checked the Flyway migrations (`V1`–`V35`) for a covering index.
4. Reviewed service code for N+1 access patterns.

## Findings — index coverage

Coverage is **strong** (82 indexes across the schema). Every tenant-scoped list, every
foreign-key lookup, and every uniqueness rule is backed:

| Area | Representative query | Index |
|------|----------------------|-------|
| Auth | `app_users` by email | unique `app_users(email)` |
| Orders | tenant feed, idempotency, a customer's orders | `orders(tenant_id, created_at)`, unique `orders(tenant_id, idempotency_key)`, `orders(customer_id)` |
| Order items | items of an order / a vendor | `order_items(order_id)`, `order_items(vendor_id)` |
| Marketplace | commissions & payouts by vendor | `commission_entries(tenant_id, vendor_id)`, `payout_requests(tenant_id, vendor_id)` |
| Fulfilment | deliveries by agent / status | `deliveries(tenant_id, agent_id)`, `deliveries(tenant_id, status)` |
| Loyalty / affiliate | ledgers by account / affiliate | `loyalty_transactions(tenant_id, account_id, created_at)`, `affiliate_rewards(tenant_id, affiliate_id, created_at)` |
| Notifications | inbox + unread | `notifications(tenant_id, recipient_id, created_at)`, partial `… WHERE is_read = FALSE` |
| HRM (9a–9e) | roster, attendance, leave, payroll, awards | composite `(tenant_id, …)` indexes per table; unique `attendance(tenant_id, employee_id, work_date)`, `pay_runs(tenant_id, period)` |
| 2FA (10b) | recovery codes by user | `two_factor_recovery_codes(user_id)` |

### Notes / accepted trade-offs

- **Customer order export (10c)** filters `(tenant_id, customer_id)` and sorts by
  `created_at`. It is served by `orders(customer_id)`: `customer_id` is highly selective
  (one user), so Postgres uses that index and the residual `tenant_id` filter + sort is
  cheap. A composite `(tenant_id, customer_id, created_at)` would cover the sort exactly but
  duplicates an existing index for marginal gain, so it was **not** added.
- **Partial indexes** (unread notifications, single-pending payout) keep hot predicates tiny.

## Findings — N+1 and access patterns

The service layer already avoids N+1 in the places it would otherwise appear:

- **Batched name resolution** — leave/affiliate list endpoints resolve type/user names from a
  single map fetch, not per row.
- **Denormalised aggregates** — `pay_runs.payslip_count` / `total_net` are kept current so the
  runs list is one query (no per-run payslip scan).
- **Bulk payroll generation** runs in a constant number of queries regardless of headcount
  (one active-employee fetch + one tenant-wide approved-leave fetch + one `saveAll`).

## Conclusion

No missing indexes block GA. The schema is index-complete for current access patterns; the
one optional refinement (composite orders index) is documented above and deferred as
redundant. Re-run this audit when new list/report endpoints land.
