# LoyalSuit — developer convenience commands
# Usage: `make <target>`. Run `make help` to list everything.

SHELL := /bin/bash
BACKEND_DIR := backend
FRONTEND_DIR := frontend

# Load backend/.env so `make` targets can export the vars Spring needs.
ifneq (,$(wildcard $(BACKEND_DIR)/.env))
	include $(BACKEND_DIR)/.env
	export
endif

.DEFAULT_GOAL := help

## ─── Help ────────────────────────────────────────────────────────────────
.PHONY: help
help: ## Show this help
	@echo "LoyalSuit — available commands:"
	@grep -hE '^[a-zA-Z_-]+:.*?## .*$$' $(firstword $(MAKEFILE_LIST)) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'

## ─── Backend (Spring Boot) ──────────────────────────────────────────────
.PHONY: backend
backend: ## Run the backend (Spring Boot, dev profile) with .env loaded
	cd $(BACKEND_DIR) && SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

.PHONY: backend-build
backend-build: ## Compile + package the backend (skips tests)
	cd $(BACKEND_DIR) && mvn clean package -DskipTests

.PHONY: backend-test
backend-test: ## Run backend tests
	cd $(BACKEND_DIR) && mvn test

.PHONY: backend-lint
backend-lint: ## Run Checkstyle on the backend
	cd $(BACKEND_DIR) && mvn checkstyle:check

.PHONY: backend-clean
backend-clean: ## Remove backend build artifacts
	cd $(BACKEND_DIR) && mvn clean

## ─── Frontend (Next.js) ─────────────────────────────────────────────────
.PHONY: frontend
frontend: ## Run the frontend dev server
	cd $(FRONTEND_DIR) && npm run dev

.PHONY: frontend-install
frontend-install: ## Install frontend dependencies
	cd $(FRONTEND_DIR) && npm install

.PHONY: frontend-build
frontend-build: ## Production build of the frontend
	cd $(FRONTEND_DIR) && npm run build

.PHONY: frontend-lint
frontend-lint: ## Lint + type-check the frontend
	cd $(FRONTEND_DIR) && npm run lint && npm run type-check

.PHONY: frontend-clean
frontend-clean: ## Clear the Next.js build cache (fixes "Cannot find module './N.js'")
	rm -rf $(FRONTEND_DIR)/.next $(FRONTEND_DIR)/node_modules/.cache

## ─── Combined ───────────────────────────────────────────────────────────
.PHONY: install
install: frontend-install ## Install all dependencies (frontend; backend resolves via Maven on first run)
	cd $(BACKEND_DIR) && mvn -q dependency:resolve

.PHONY: dev
dev: ## Run backend + frontend together (Ctrl+C stops both)
	@echo "Starting backend (:8080) and frontend (:3000)…"
	@trap 'kill 0' EXIT; \
	( cd $(BACKEND_DIR) && SPRING_PROFILES_ACTIVE=dev mvn -q spring-boot:run ) & \
	( cd $(FRONTEND_DIR) && npm run dev ) & \
	wait

.PHONY: test
test: backend-test ## Run all tests

.PHONY: lint
lint: backend-lint frontend-lint ## Lint backend + frontend

.PHONY: health
health: ## Curl the backend health endpoint
	@curl -s http://localhost:8080/actuator/health | python3 -m json.tool || echo "Backend not reachable on :8080"

.PHONY: smoke-login
smoke-login: ## Log in as the seeded super admin and print the token
	@curl -s -X POST http://localhost:8080/api/v1/auth/login \
		-H "Content-Type: application/json" \
		-d '{"email":"superadmin@loyalsuit.dev","password":"Admin@Test123"}' | python3 -m json.tool
