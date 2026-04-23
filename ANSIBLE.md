# Ansible Playbooks

Automated infrastructure and service deployment for the Travel Orchestration Platform.

## Prerequisites

- Ansible 2.9+
- Docker installed on the target machine

## Available Playbooks

| Playbook | Purpose | Command |
|----------|---------|---------|
| `site.yml` | Complete system deployment | `ansible-playbook ansible/playbooks/site.yml` |
| `setup_infrastructure.yml` | Infrastructure only (Docker, SSL, Vault, Databases) | `ansible-playbook ansible/playbooks/setup_infrastructure.yml` |
| `deploy_services.yml` | Microservices only | `ansible-playbook ansible/playbooks/deploy_services.yml` |
| `deploy_databases.yml` | Databases only (PostgreSQL, Neo4j) | `ansible-playbook ansible/playbooks/deploy_databases.yml` |

## Deployment Order

The `site.yml` master playbook deploys roles in order:
1. **Docker** - Verify Docker installation
2. **SSL** - Deploy TLS certificates
3. **Vault** - Configure secrets management
4. **Databases** - Deploy PostgreSQL and Neo4j
5. **Microservices** - Deploy all services

## Quick Start

```bash
# Complete deployment
ansible-playbook ansible/playbooks/site.yml

# Dry run
ansible-playbook ansible/playbooks/site.yml --check

# Verbose output
ansible-playbook ansible/playbooks/site.yml -vvv

# Deploy specific components
ansible-playbook ansible/playbooks/site.yml --tags infrastructure
ansible-playbook ansible/playbooks/site.yml --tags services
ansible-playbook ansible/playbooks/site.yml --tags databases
```

## Two-Stage Deployment

```bash
# Stage 1: Infrastructure
ansible-playbook ansible/playbooks/setup_infrastructure.yml

# Stage 2: Microservices
ansible-playbook ansible/playbooks/deploy_services.yml
```

## Service Redeployment

```bash
# Remove and redeploy microservices
ansible-playbook ansible/playbooks/deploy_services.yml --extra-vars "microservices_state=absent"
ansible-playbook ansible/playbooks/deploy_services.yml
```

## Directory Structure

```
ansible/
├── ansible.cfg           # Ansible configuration
├── group_vars/
│   └── all.yml           # Global variables
├── inventory/            # Host inventory files
├── playbooks/
│   ├── site.yml                  # Master playbook
│   ├── setup_infrastructure.yml  # Infrastructure deployment
│   ├── deploy_services.yml       # Microservices deployment
│   └── deploy_databases.yml      # Database deployment
├── roles/
│   ├── docker/           # Docker installation
│   ├── ssl/              # TLS certificate management
│   ├── vault/            # HashiCorp Vault
│   ├── databases/        # PostgreSQL and Neo4j
│   └── microservices/    # Application services
├── files/                # Static files
└── templates/            # Jinja2 templates
```

## Service Endpoints (After Deployment)

| Service | URL |
|---------|-----|
| API Gateway | https://localhost:8443 |
| Frontend | http://localhost:4200 |
| PostgreSQL | localhost:5432 |
| Neo4j Browser | http://localhost:7474 |
| Neo4j Bolt | localhost:7687 |
| Vault | http://localhost:8200 |
