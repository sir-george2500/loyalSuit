---
name: procurax-frontend-design
description: ProcuraX frontend design specifications. Use when building any UI component, page, or layout for the ProcuraX platform to ensure consistency with the CBL institutional design system.
---

# ProcuraX Frontend Design System — CBL Institutional Edition

## Mandate

ProcuraX is an official procurement management system for the Central Bank of Liberia. Every design decision must communicate **authority, permanence, and regulatory credibility**. The interface should feel like a Bloomberg terminal or a World Bank portal — not a SaaS startup. Information density, trust, and precision take priority over visual flair.

---

## Color Tokens

```css
/* Primary — Deep Institutional Navy */
--color-primary: #0E3A6E;        /* CBL navy, used for CTAs, active states */
--color-primary-hover: #0a2d58;
--color-primary-content: #ffffff;

/* Sidebar */
--sidebar-bg: #0B1F3A;           /* Near-black navy */
--sidebar-text: #94A3B8;         /* Muted slate */
--sidebar-active: #1E3A5F;
--sidebar-active-text: #ffffff;
--sidebar-border: rgba(255,255,255,0.06);

/* Surfaces — Light Mode */
--color-base-100: #F4F6F9;       /* Warm off-white page background */
--color-base-200: #EAECF0;       /* Dividers, table alternates */
--color-base-300: #D1D5DB;       /* Input borders, card borders */
--color-base-content: #111827;   /* Primary text */
--color-text-muted: #6B7280;     /* Secondary text */

/* Cards */
--card-bg: #FFFFFF;              /* Pure white cards on off-white page */
--card-border: #E5E7EB;
--card-shadow: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);

/* Status colors — institutional, desaturated */
--color-success: #166534;        /* Deep green for approved/active */
--color-success-bg: #DCFCE7;
--color-warning: #92400E;        /* Amber-brown for pending/warn */
--color-warning-bg: #FEF3C7;
--color-error: #991B1B;          /* Dark red for blocked/rejected */
--color-error-bg: #FEE2E2;
--color-info: #1E40AF;           /* Deep blue for in-progress */
--color-info-bg: #DBEAFE;

/* Dark Mode — solid, no glassmorphism */
--color-dark-base: #0F172A;
--color-dark-surface: #1E293B;
--color-dark-border: #334155;
--color-dark-text: #F1F5F9;
--color-dark-muted: #94A3B8;
```

**Rules:**
- NEVER use `#4358F6` (the old startup blue) anywhere
- NEVER use glassmorphism (`backdrop-filter: blur`) — solid surfaces only
- Status badges use the desaturated palette above, never bright saturated colors

---

## Typography

```
Body / UI:      IBM Plex Sans (Google Fonts)
Display / H1:   IBM Plex Sans SemiBold (no decorative display font)
Monospace:      IBM Plex Mono — used for: reference numbers, hashes, amounts, dates in tables
```

**Import:**
```
IBM Plex Sans: weights 400, 500, 600, 700
IBM Plex Mono: weights 400, 500
```

**Rules:**
- Drop `Outfit` — it reads casual/consumer
- Drop `Inter` — overused in SaaS, no institutional weight
- `IBM Plex Sans` was designed for data-dense government/enterprise interfaces
- Reference numbers (e.g. `CBL-2026-0042`) are always `font-mono text-sm`
- Hash values are always `font-mono text-xs text-muted truncate`
- Dollar amounts in tables are `font-mono tabular-nums`

---

## Layout & Spacing

- **Page background**: `bg-[#F4F6F9]` (never pure white for the page itself)
- **Cards**: white `#FFFFFF` on the off-white background — creates clear card lift without shadow theatrics
- **Card border-radius**: `rounded-lg` (8px) — professional, not startup-rounded
- **Table rows**: `py-3 px-4` — dense but readable
- **Section headers**: `text-xs font-semibold uppercase tracking-widest text-slate-500` — the institutional label style
- **No large hero padding** — this is a working tool, not a marketing page

---

## Sidebar Structure

The sidebar is organized into **named groups** (not a flat list):

```
[Logo + entity name + cycle year]

─ Today
  Dashboard
  Approvals (badge: count)
  Complaints

─ Plan
  Procurement Plans

─ Procure
  Tenders
  Frameworks & Catalogue
  Vendors

─ Decide
  Evaluation
  Awards
  Contracts
  Purchase Orders
  Payments
  Bid Ceremony

─ Account
  Audit Log
  Reports
  Integrations
  Risk Register
  Team & Access

─ Cross-cutting
  AI Assistant
  Roles & Permissions
  State Machines

[User avatar + name + logout]
```

Group labels are `text-[10px] font-bold uppercase tracking-widest text-slate-500 px-3 mt-4 mb-1`.

---

## Component Patterns

### Status Badges
```tsx
// Always use text + background pair from the status palette
// Format: rounded-full px-2.5 py-0.5 text-xs font-semibold
<span className="rounded-full px-2.5 py-0.5 text-xs font-semibold bg-[#DCFCE7] text-[#166534]">
  Approved
</span>
```

### Reference Numbers
```tsx
// Always monospace, always a link or tooltip to the full record
<code className="font-mono text-xs text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded">
  CBL-2026-0042
</code>
```

### Data Tables
- Striped with `even:bg-slate-50` or plain white with bottom border only
- No outer border on the table itself — let the card contain it
- Sticky header on long tables
- Amounts right-aligned, monospace
- Action buttons are `text-primary text-xs font-medium hover:underline` — never full buttons in table rows

### Cards
```tsx
<div className="bg-white rounded-lg border border-[#E5E7EB] shadow-sm p-5">
```
- No glassmorphism in dark mode — use `dark:bg-slate-800 dark:border-slate-700`
- Drop the `card-saas` utility — replace with the above pattern

### Work Queue Items
Each item shows: Reference | Title + Owner | Method · Category | State badge | Value (mono) | Due (color-coded: red if ≤1d, amber ≤7d, gray otherwise)

### Rules Engine Panel
Inline compliance check list. Each rule shows:
- `PASS` (green) / `WARN` (amber) / `BLOCK` (red) pill
- Rule reference (e.g. `PPCC §27`)
- Short description
- Never collapse by default — this is critical information

---

## Animation Policy

**Keep — functional transitions only:**
- `FadeIn` component — page section entry (keep, reduce delay to max 0.1s)
- `transition-colors duration-150` on buttons and interactive elements
- Table row hover: `hover:bg-slate-50`

**Remove entirely:**
- `StaggerGrid` / `StaggerItem` on dashboard data — data should appear immediately
- `TextReveal` / `LogoReveal` — wrong context for an operational system
- All `backdrop-filter: blur` / glassmorphism
- The `mesh-background` radial gradients
- Lenis scroll momentum — unnecessary in a data-dense app, causes disorientation on long tables
- Any animation on forms or tables — users are working, not browsing

**Keep Lenis** only on the public-facing landing/login page if one exists.

---

## Copy Standards

Replace startup language with institutional language throughout:

| Old (startup)                    | New (institutional)               |
|----------------------------------|-----------------------------------|
| "Broadcast Global RFP"           | "Publish Tender Notice"           |
| "Procurement Command Center"     | "Procurement Dashboard"           |
| "Tethered Vault Link Secured"    | "Document upload confirmed"       |
| "Sealed & Drop Bid Payload"      | "Submit Bid"                      |
| "Construct Bid →"                | "Submit Bid"                      |
| "Ecosystem"                      | "Platform"                        |
| "( FIG. 02 )"                    | Remove entirely                   |
| "Active Tenders circulating"     | "No active tenders found"         |

---

## What NOT to Do

1. No glassmorphism anywhere
2. No vibrant saturated accent colors — navy and desaturated status colors only
3. No Outfit font
4. No startup copy ("ecosystem", "command center", "broadcast")
5. No decorative animations on data pages
6. No pure-white page backgrounds — use `#F4F6F9`
7. No oversized hero padding on internal dashboard pages
8. No `card-saas` utility — use the plain white card pattern
9. No `mesh-background` utility
10. No `( FIG. XX )` labels
