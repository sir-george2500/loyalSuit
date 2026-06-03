# 🚀 Production Code Enforcer

## FAANG-Grade Engineering Standard (Next.js + Spring Boot + Modular Monolith)

This repository defines a **strict engineering standard** for building production-grade systems using:

- ⚛️ Next.js (Frontend / Fullstack)
- ☕ Spring Boot (Backend Services)
- 🧱 Modular Monolith Architecture

It enforces **FAANG-level expectations** for all code generation, review, refactoring, and system design.

# ⚠️ CORE PRINCIPLE

> Every line of code must be production-ready for a system serving millions of users.

If it is not production-ready:

- It must be improved until it is, OR
- It must be explicitly rejected with clear reasoning

No shortcuts. No prototypes. No “MVP-only” code.

---

# 🎯 PURPOSE

This standard ensures:

- Scalable system design
- Production-grade reliability
- Clean modular architecture
- Security-first implementation
- Maintainable long-term codebases

---

# 🧠 APPLICABLE SCOPE

This standard applies to ALL code-related tasks:

- Feature development
- API design
- Refactoring
- Debugging
- System design
- Test writing
- Infrastructure configuration

If code is produced → this standard is active.

---

# 🏗️ ARCHITECTURE RULES

## 🧱 Modular Monolith

- Strict bounded contexts (modules)
- No cross-module database access
- Explicit module boundaries
- Dependency direction enforced:

- No “god services” or shared dumping utilities

---

# ⚛️ NEXT.JS STANDARDS

## Frontend & Fullstack Rules

- Prefer Server Components where possible
- Use Server Actions intentionally
- Avoid unnecessary client-side rendering
- Optimize:
- bundle size
- hydration cost
- caching strategy
- No uncontrolled API calls in UI components
- Clear separation between UI and data layers

---

# ☕ SPRING BOOT STANDARDS

## Backend Rules

- Clean layered architecture or hexagonal design
- DTOs required (no entity exposure)
- Service layer must be thin and focused
- Strong validation at all input boundaries
- Structured exception handling (no generic swallowing)
- Secure logging (no sensitive data leaks)

---

# 🔐 SECURITY REQUIREMENTS

- Input validation at every boundary
- OWASP compliance mindset
- No trust in client input
- Proper auth/authz handling
- Secure defaults always

---

# ⚡ PERFORMANCE REQUIREMENTS

- Must handle high concurrency
- Avoid O(n²) patterns where avoidable
- Use caching, pagination, batching when needed
- Stateless services preferred
- Explicit timeout and retry handling (backend)

---

# 🧪 TESTING REQUIREMENTS

All production code must include:

- Unit tests (core logic)
- Integration tests (service boundaries)
- Edge case coverage
- Deterministic execution (no flaky tests)

---

# 🧾 PRE-CODE CHECKLIST

Before writing any code, verify:

## Understanding

- Do I fully understand the requirements?
- Are edge cases defined?

## Design

- Is this the simplest scalable solution?
- Is architecture clean and modular?

## System Safety

- What fails under load?
- How does it recover?
- Are dependencies safe?

If unclear → ASK before coding.

---

# 🚫 FORBIDDEN PRACTICES

Never produce:

- Incomplete implementations
- Prototype or “quick fix” code
- Missing error handling
- Unsafe input handling
- Cross-module dependency violations
- Hardcoded environment assumptions

---

# 🧯 REFUSAL RULE

If production-grade quality cannot be guaranteed:

You must:

- State why it cannot be safely implemented
- Identify missing requirements
- Request clarification

Do NOT output unsafe or incomplete code.

---

# 🏁 FINAL STANDARD

> Code must be scalable, secure, maintainable, and worthy of a FAANG production system handling millions of users.

---

# 📌 TECHNOLOGIES COVERED

- Next.js (App Router, Server Components, API routes)
- Spring Boot (REST APIs, services, modular architecture)
- Modular Monolith Systems
- RESTful API design
- Distributed-ready architecture patterns

---
