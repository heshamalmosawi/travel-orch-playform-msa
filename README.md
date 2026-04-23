# Travel Orchestration Platform - Microservices Architecture

## Description 
**This project is still being developed.**

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
docker compose up --build

# Access the services
# Frontend: https://localhost:4200
# API Gateway: https://localhost:8443
```

### Docker Services

The following services will be started:

- **frontend-service** (port 4200): Angular frontend application
- **apigateway-service** (port 8443): API Gateway for routing requests
- **user-service** (port 8082): User management microservice
- **travel-service** (port 8083): Travel booking microservice
- **payment-service** (port 8084): Payment processing microservice

### Stop Services

```bash
# Stop all services
docker compose down

# Stop and remove volumes
docker compose down -v
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

# Payment Service
cd backend/payment-service
./mvnw spring-boot:run
```

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
├── ansible/              # Infrastructure as Code (see ANSIBLE.md)
├── backend/
│   ├── apigateway-service/
│   ├── user-service/
│   ├── travel-service/
│   └── payment-service/
├── frontend/
│   └── travel-orch/
└── docker-compose.yml
```
