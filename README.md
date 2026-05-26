# Travel Orchestration Platform - Microservices Architecture

## Overview

Travel Orchestration Platform is a microservices-based system for discovering, booking, and
managing travel packages. Travelers browse and purchase manager-curated trips and leave reviews,
travel managers create packages and track their performance, and admins oversee users, bookings,
payments, and platform-wide analytics.

## Features

- **Authentication & RBAC** — register / login / token refresh with JWT (short-lived access token
  + long-lived refresh token, silently renewed on the frontend); role-based access for `admin`,
  `travel_manager`, and `user`.
- **Travel packages** — managers create and manage trips with multi-stop destinations and statuses
  (draft / confirmed / cancelled); travelers browse upcoming packages and view details.
- **Recommendations** — Neo4j graph-backed travel recommendations.
- **Payments** — Stripe-backed transactions and refunds, with purchase/subscriber queries.
- **Reviews & reports** — per-travel and per-manager reviews (ratings + comments); travelers can
  report managers.
- **Manager dashboard** — key stats (income, trips, travelers) plus a 6-month income trend, and
  per-package subscriber lists.
- **Profiles** — traveler statistics; travel managers also see the reviews left on their trips.
- **Admin console** — manage users, travels, bookings, payments, and reports, plus a platform
  **analytics** dashboard (income, top managers/travels, KPIs).

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 20 (standalone components, signals), SCSS |
| API Gateway | Spring Cloud Gateway (reactive) — routing, rate limiting, CORS |
| Backend | Java 21, Spring Boot WebFlux (reactive), Spring Security (JWT), Spring Data JPA; Maven multi-module with a shared `common` library |
| Data | PostgreSQL (primary), Neo4j (recommendations graph), Elasticsearch |
| Payments | Stripe |
| Observability | Grafana + Loki + Promtail |
| Quality / CI | SonarQube, Jenkins |
| Infra / Secrets | Ansible, HashiCorp Vault (optional) |

## Architecture

All client traffic goes through the API Gateway (`https://localhost:8443`), which routes by path
prefix to the backend services (each `/api/<service>` prefix is stripped before forwarding):

| Gateway path | Service | Port |
|---|---|---|
| `/api/user/**` | user-service | 8082 |
| `/api/travel/**` | travel-service | 8083 |
| `/api/payment/**` | payment-service | 8084 |

The backend services share a single PostgreSQL database through the `common` module's JPA entities
and repositories.

## Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Node.js 22 (for frontend local development)
- Maven (for backend local development)

## Quick Start with Docker Compose

The easiest way to run the entire application is using Docker Compose:

```bash
# (Optional) Customize environment variables
cp .env.example .env
# Edit .env to customize rate limiting or other settings

# Build and start all services
docker compose -f docker-compose.app.yml up --build -d

# Include Grafana/Loki/Promtail logging stack
docker compose -f docker-compose.app.yml --profile logging up --build -d

# Access the services
# Frontend: https://localhost:4200
# API Gateway: https://localhost:8443
# Grafana: http://localhost:3000 (admin/changeme)
```

### Docker Services

The following services will be started:

- **frontend-service** (port 4200): Angular frontend application
- **apigateway-service** (port 8443): API Gateway for routing requests
- **user-service** (port 8082): User management microservice
- **travel-service** (port 8083): Travel booking microservice
- **payment-service** (port 8084): Payment processing microservice
- **db** (PostgreSQL), **neo4j**, and **elasticsearch**: backing datastores
- With `--profile logging`: **grafana** (port 3000), **loki**, and **promtail**

### Stop Services

```bash
# Stop all services
docker compose -f docker-compose.app.yml down

# Stop and remove volumes
docker compose -f docker-compose.app.yml down -v
```

## Local Development

### Backend Services

Each backend service can be run independently using Maven:

```bash
# API Gateway
cd backend/apigateway-service
./mvnw spring-boot:run

# User Service
cd backend/user-service
./mvnw spring-boot:run

# Travel Service
cd backend/travel-service
./mvnw spring-boot:run

# Payment Service (requires Stripe secret key — see below)
cd backend/payment-service
STRIPE_SECRET_KEY=sk_test_your_key_here ./mvnw spring-boot:run
```

#### Payment Service — Stripe Configuration

The payment-service creates a Stripe client at startup and **will fail to start** without a valid Stripe secret key. Provide the key via one of:

| Method | How |
|---|---|
| **Environment variable** (recommended for local dev) | `export STRIPE_SECRET_KEY=sk_test_…` before running the service |
| **HashiCorp Vault** | Set `VAULT_ENABLED=true`, `VAULT_URI`, and `VAULT_TOKEN`; store the key at `secret/data/travel-system/stripe` with key `stripe.secret-key` |

The key must start with `sk_test_` (test mode) or `sk_live_` (live mode). Obtain a key from the [Stripe Dashboard](https://dashboard.stripe.com/apikeys).

All backend services communicate through the API Gateway at https://localhost:8443

### Frontend Application

```bash
cd frontend/travel-orch
npm install
npm start
```

The Angular application will be available at https://localhost:4200

## Ansible Deployment

For automated infrastructure deployment using Ansible, see [ANSIBLE.md](ANSIBLE.md).

```bash
# Quick start - deploy everything
ansible-playbook ansible/playbooks/site.yml
```

## Project Structure

```
travel-orch-playform-msa/
├── ansible/                     # Infrastructure as Code (see ANSIBLE.md)
├── backend/
│   ├── apigateway-service/      # Spring Cloud Gateway (routing, rate limiting, CORS)
│   ├── user-service/            # auth, users, RBAC
│   ├── travel-service/          # travels, destinations, feedback, reports, recommendations, analytics
│   ├── payment-service/         # Stripe payments & refunds
│   ├── common/                  # shared JPA entities, repositories, JWT utilities
│   └── pom.xml                  # Maven multi-module parent
├── frontend/
│   └── travel-orch/             # Angular 20 SPA
├── docker-compose.app.yml       # full stack: services + datastores (+ `logging` profile)
├── docker-compose.yml           # application services only
├── docker-compose.logging.yml   # Grafana / Loki / Promtail
└── docker-compose.sonarqube.yml # SonarQube
```
