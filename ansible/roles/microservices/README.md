# Microservices Role

Deploys the Travel Management System microservices using Docker Compose v2.

## Requirements

- Ansible 2.14+
- Docker installed on target system
- Docker Compose v2 plugin
- `community.docker` collection installed

## Role Variables

### Default Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `microservices_project_root` | `{{ project_root }}` | Root directory containing docker-compose.yml |
| `microservices_compose_file` | `{{ project_root }}/docker-compose.yml` | Path to compose file |
| `microservices_state` | `present` | Deployment state: `present`, `absent`, `restarted`, `stopped` |
| `microservices_build` | `policy` | Build option: `always`, `never`, `policy` |
| `microservices_pull` | `policy` | Pull option: `always`, `missing`, `never`, `policy` |
| `microservices_remove_orphans` | `true` | Remove containers for undefined services |
| `microservices_remove_volumes` | `false` | Remove volumes when `state=absent` |
| `microservices_services` | `[]` | Specific services to deploy (empty = all) |
| `microservices_health_check_enabled` | `true` | Enable health checks |
| `microservices_health_check_delay` | `10` | Delay before health checks (seconds) |
| `microservices_health_check_timeout` | `120` | Health check timeout (seconds) |
| `microservices_gateway_port` | `8443` | API Gateway port |
| `microservices_frontend_port` | `4200` | Frontend service port |
| `microservices_network` | `microservices-network` | Docker network name |
| `microservices_debug` | `false` | Enable debug output |

### Service Replicas Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `microservices_replicas.frontend` | `1` | Number of frontend replicas |
| `microservices_replicas.gateway` | `1` | Number of gateway replicas |
| `microservices_replicas.user` | `1` | Number of user-service replicas |
| `microservices_replicas.travel` | `1` | Number of travel-service replicas |
| `microservices_replicas.payment` | `1` | Number of payment-service replicas |

**Note**: Services with replicas > 1 will use Docker's deploy mode and will not have a fixed container_name. Services with replicas = 1 will use a fixed container_name.

### Environment Files

| Variable | Default | Description |
|----------|---------|-------------|
| `microservices_env_files` | `["{{ project_root }}/.env"]` | Environment files to load |

## Dependencies

- `docker` role (verifies Docker installation)

## Example Usage

### Deploy All Services

```yaml
---
- hosts: localhost
  become: true
  roles:
    - microservices
```

### Deploy with Build

```yaml
---
- hosts: localhost
  become: true
  roles:
    - role: microservices
      vars:
        microservices_build: always
```

### Deploy Specific Services

```yaml
---
- hosts: localhost
  become: true
  roles:
    - role: microservices
      vars:
        microservices_services:
          - apigateway-service
```

### Stop All Services

```yaml
---
- hosts: localhost
  become: true
  roles:
    - role: microservices
      vars:
        microservices_state: absent
```

### Deploy with Service Replicas

```yaml
---
- hosts: localhost
  become: true
  roles:
    - role: microservices
      vars:
        microservices_replicas:
          frontend: 1
          gateway: 1
          user: 3
          travel: 3
          payment: 3
```

### Standalone Execution

```bash
# Deploy all services
ansible-playbook playbooks/test_microservices.yml

# Deploy with rebuild
ansible-playbook playbooks/test_microservices.yml -e "microservices_build=always"

# Deploy specific services
ansible-playbook playbooks/test_microservices.yml \
  -e "microservices_services=['apigateway-service']"

# Stop all services
ansible-playbook playbooks/test_microservices.yml -e "microservices_state=absent"

# Stop and remove volumes
ansible-playbook playbooks/test_microservices.yml \
  -e "microservices_state=absent" \
  -e "microservices_remove_volumes=true"

# Deploy with custom replica configuration
ansible-playbook playbooks/test_microservices.yml \
  -e "microservices_replicas='{\"frontend\":1,\"gateway\":1,\"user\":3,\"travel\":3,\"payment\":3}'"
```

## Tasks

This role performs the following tasks:

1. **Pre-flight Checks** - Verifies docker-compose.yml and .env files exist
2. **Deploy Services** - Uses `community.docker.docker_compose_v2` to deploy
3. **Health Checks** - Waits for Gateway and Frontend to be ready
4. **Container Status** - Displays running container status
5. **Summary** - Shows deployment summary with service URLs

## Services Deployed

| Service | Container | Port | Description |
|---------|-----------|------|-------------|
| Frontend | frontend-service | 4200 | Angular web application |
| API Gateway | apigateway-service | 8443 | API routing and SSL |
| User Service | user-service | 8082 | User management |
| Travel Service | travel-service | 8083 | Travel booking |
| Payment Service | payment-service | 8084 | Payment processing |

## State Options

| State | Description |
|-------|-------------|
| `present` | Start services (create if needed) |
| `absent` | Stop and remove services |
| `restarted` | Restart all services |
| `stopped` | Stop services without removing |

## Build Options

| Option | Description |
|--------|-------------|
| `always` | Always rebuild images before starting |
| `never` | Never rebuild images |
| `policy` | Follow build policy in compose file |

## Troubleshooting

**Issue**: Services fail to start
- **Solution**: Check Docker logs: `docker compose logs -f`

**Issue**: Health checks timeout
- **Solution**: Increase `microservices_health_check_timeout` or check service logs

**Issue**: Port already in use
- **Solution**: Stop conflicting services or change ports in docker-compose.yml

**Issue**: Build fails
- **Solution**: Check Dockerfiles and ensure dependencies are available

**Issue**: Network issues between services
- **Solution**: Verify all services use the same network: `microservices-network`

## Service URLs

After deployment, services are available at:

- **Frontend**: http://localhost:4200
- **API Gateway**: https://localhost:8443

## License

MIT
