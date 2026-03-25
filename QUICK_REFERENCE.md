# Quick Reference: Service Replicas Configuration

## Quick Start

### Deploy with Default Configuration (3 replicas for backend services)
```bash
ansible-playbook ansible/playbooks/deploy_services.yml
```

### Deploy with Custom Replicas
```bash
ansible-playbook ansible/playbooks/deploy_services.yml \
  -e "microservices_replicas={'frontend':1,'eureka':1,'gateway':1,'user':5,'travel':5,'payment':5}"
```

### Check Configuration
```bash
./test_replica_config.sh
```

## Configuration Files

| File | Purpose |
|------|---------|
| `ansible/group_vars/all.yml` | Environment-specific replica counts |
| `ansible/roles/microservices/defaults/main.yml` | Default fallback values |
| `ansible/roles/microservices/templates/docker-compose.j2` | Template generator |
| `SERVICE_REPLICAS.md` | Full documentation |

## Default Values

**Production** (group_vars):
- Frontend: 1, Eureka: 1, Gateway: 1
- User: 3, Travel: 3, Payment: 3

**Development** (defaults):
- All services: 1 replica

## Key Concept

- **replicas = 1**: Uses fixed `container_name`
- **replicas > 1**: Uses `deploy.replicas` (scales containers dynamically)

## Monitor Replicas

```bash
# View all containers
docker ps

# View specific service replicas
docker ps | grep user-service
docker ps | grep travel-service
docker ps | grep payment-service

# Check service status in Eureka
curl http://localhost:8761/eureka/apps
```

## Scale Manually

```bash
cd /home/hesham/travel-orch-playform-msa
docker compose up -d --scale user-service=5
docker compose up -d --scale travel-service=5
docker compose up -d --scale payment-service=5
```

## Documentation

For complete details, see: `SERVICE_REPLICAS.md`

## Changes Made

1. Created Jinja2 template for docker-compose with replica support
2. Updated microservices role to generate compose file from template
3. Configured per-service replica settings in group_vars
4. Added comprehensive documentation and validation script
5. Updated README with replica configuration examples

**All changes are ready but NOT committed.**
