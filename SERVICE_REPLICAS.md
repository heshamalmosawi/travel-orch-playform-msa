# Service Replicas Configuration Guide

## Overview

This guide explains how to configure service replicas for the Travel Management System backend services using Ansible.

## Architecture

The replica configuration uses Docker Compose's deploy mode to scale services. Services are configured as follows:

### Services with Fixed Replicas (1)
- **Frontend Service** - Single instance due to port mapping constraints
- **Eureka Service** - Single instance for service discovery
- **API Gateway** - Single instance for SSL termination and routing

### Scalable Services (>1)
- **User Service** - Can be scaled for load handling
- **Travel Service** - Can be scaled for high availability
- **Payment Service** - Can be scaled for transaction processing

## Configuration Files

### 1. Group Variables (`ansible/group_vars/all.yml`)

Defines the default replica count for all services:

```yaml
microservices:
  replicas:
    frontend: 1
    eureka: 1
    gateway: 1
    user: 3
    travel: 3
    payment: 3
```

### 2. Role Defaults (`ansible/roles/microservices/defaults/main.yml`)

Provides fallback default values if not specified in group_vars:

```yaml
microservices_replicas:
  frontend: 1
  eureka: 1
  gateway: 1
  user: 1
  travel: 1
  payment: 1
```

### 3. Docker Compose Template (`ansible/roles/microservices/templates/docker-compose.j2`)

Jinja2 template that generates the docker-compose.yml with replica configuration:

```yaml
services:
  user-service:
    build: ./backend/user-service
{% if microservices_replicas.user > 1 %}
    deploy:
      replicas: {{ microservices_replicas.user }}
{% else %}
    container_name: user-service
{% endif %}
    # ... rest of configuration
```

## How It Works

### Replica Logic

1. **Replicas = 1**: Service uses a fixed `container_name` (traditional Docker Compose)
2. **Replicas > 1**: Service uses `deploy.replicas` directive, removing `container_name`

### Template Generation

The Ansible role generates `docker-compose.yml` from the template with these steps:

1. Load replica configuration from group_vars
2. Apply defaults if not specified
3. Generate docker-compose.yml with appropriate configuration
4. Backup existing docker-compose.yml
5. Deploy services using the generated file

## Usage

### Deploy with Default Replicas

```bash
ansible-playbook ansible/playbooks/deploy_services.yml
```

This deploys with:
- Frontend: 1 replica
- Eureka: 1 replica
- Gateway: 1 replica
- User Service: 3 replicas
- Travel Service: 3 replicas
- Payment Service: 3 replicas

### Deploy with Custom Replicas

#### Option 1: Command Line

```bash
ansible-playbook ansible/playbooks/deploy_services.yml \
  -e "microservices_replicas={'user':5,'travel':5,'payment':5}"
```

#### Option 2: JSON File

Create a file `replica-config.json`:
```json
{
  "microservices_replicas": {
    "frontend": 1,
    "eureka": 1,
    "gateway": 1,
    "user": 5,
    "travel": 5,
    "payment": 5
  }
}
```

Deploy:
```bash
ansible-playbook ansible/playbooks/deploy_services.yml \
  -e "@replica-config.json"
```

#### Option 3: Modify Group Variables

Edit `ansible/group_vars/all.yml`:
```yaml
microservices:
  replicas:
    frontend: 1
    eureka: 1
    gateway: 1
    user: 5
    travel: 5
    payment: 5
```

Then deploy:
```bash
ansible-playbook ansible/playbooks/deploy_services.yml
```

## Scaling Strategy

### When to Scale

**Scale Up** (increase replicas) when:
- High CPU/memory usage on services
- Increased request volume
- Slow response times
- Need for high availability

**Scale Down** (decrease replicas) when:
- Low resource utilization
- Reduced traffic
- Cost optimization needed

### Recommended Configurations

#### Development Environment
```yaml
replicas:
  frontend: 1
  eureka: 1
  gateway: 1
  user: 1
  travel: 1
  payment: 1
```

#### Staging Environment
```yaml
replicas:
  frontend: 1
  eureka: 1
  gateway: 1
  user: 2
  travel: 2
  payment: 2
```

#### Production Environment
```yaml
replicas:
  frontend: 1
  eureka: 1
  gateway: 1
  user: 3-5
  travel: 3-5
  payment: 3-5
```

## Managing Replicated Services

### Check Replica Status

```bash
# View all running containers
docker ps

# View specific service replicas
docker ps | grep user-service
docker ps | grep travel-service
docker ps | grep payment-service
```

### Scale Services Manually

After deployment, you can scale using Docker Compose:

```bash
cd /home/hesham/travel-orch-playform-msa
docker compose up -d --scale user-service=5
docker compose up -d --scale travel-service=5
docker compose up -d --scale-payment-service=5
```

### View Logs for Replicated Services

```bash
# View logs for all replicas
docker compose logs user-service

# View logs for specific replica
docker logs <container-name>

# Follow logs in real-time
docker compose logs -f user-service
```

## Troubleshooting

### Issue: Service fails to start with replicas > 1

**Symptom**: Containers start but immediately exit

**Solution**: Check service logs for port conflicts or resource issues:
```bash
docker compose logs user-service
```

### Issue: Not all replicas are running

**Symptom**: Fewer containers than configured

**Solution**: Check resource availability:
```bash
docker stats
docker system df
```

### Issue: Services can't communicate

**Symptom**: Replicated services can't find each other

**Solution**: Ensure all services are on the same network (`microservices-network`):
```bash
docker network inspect microservices-network
```

### Issue: Port already in use

**Symptom**: Error about port conflicts

**Solution**: Check which services are using ports:
```bash
netstat -tulnp | grep :8082
netstat -tulnp | grep :8083
netstat -tulnp | grep :8084
```

## Monitoring

### Check Service Health

```bash
# View Eureka dashboard
http://localhost:8761

# Check service registration via Eureka
curl http://localhost:8761/eureka/apps

# Check health endpoints
curl https://localhost:8443/actuator/health
```

### Monitor Resource Usage

```bash
# Real-time stats
docker stats

# Specific service stats
docker stats $(docker ps -q --filter "name=user-service")
```

## Rollback

To revert to a previous configuration:

1. Restore the backup docker-compose.yml:
```bash
cd /home/hesham/travel-orch-playform-msa
mv docker-compose.yml.backup docker-compose.yml
```

2. Redeploy:
```bash
ansible-playbook ansible/playbooks/deploy_services.yml
```

## Best Practices

1. **Always test replica configurations in staging first**
2. **Monitor resource usage after scaling**
3. **Use health checks to ensure replicas are healthy**
4. **Consider load balancing requirements**
5. **Keep replica configuration in version control**
6. **Document scaling decisions and reasons**
7. **Implement monitoring and alerting for replica health**

## Security Considerations

- Replicated services still use the same network isolation
- Service-to-service communication is internal
- API Gateway provides unified entry point
- Secrets are managed through Vault
- SSL termination at Gateway level

## Performance Optimization

- Use appropriate replica counts based on load
- Consider service-specific resource limits
- Monitor and optimize database connection pools
- Configure appropriate timeouts and retries
- Use caching where appropriate

## Additional Resources

- [Docker Compose Deploy Mode](https://docs.docker.com/compose/compose-file/deploy/)
- [Docker Service Replication](https://docs.docker.com/engine/swarm/services/#replicated-and-global-services)
- [Spring Boot Microservices Scaling](https://spring.io/guides/gs/spring-boot-docker/)
- [Eureka Service Discovery](https://cloud.spring.io/spring-cloud-netflix/reference/html/#service-discovery-eureka-clients)
