# Logging Setup - Quick Reference

## Quick Start

```bash
# Start logging stack
./start-logging.sh

# Start microservices
docker compose up -d

# Stop logging stack
./stop-logging.sh
```

## Access URLs

| Service | URL | Credentials |
|---------|------|-------------|
| Grafana | http://localhost:3000 | admin/admin |
| Loki API | http://localhost:3100 | N/A |

## Common LogQL Queries

```logql
# All Docker container logs
{job="docker"}

# Logs from a specific service
{container_name="user-service"}
{container_name="travel-service"}
{container_name="payment-service"}
{container_name="apigateway-service"}

# Error logs everywhere
{job="docker"} |= "ERROR"

# Search for keyword
{container_name="travel-service"} |~ "booking"

# Filter by stream (stdout/stderr)
{container_name="user-service", stream="stderr"}
```

## Grafana Dashboard

A pre-configured dashboard is available at:
- **URL**: http://localhost:3000/d/docker-logs/docker-container-logs
- **Panels**: All services, Travel, User, Payment, API Gateway, Eureka, Frontend

## Check Status

```bash
# Check if Loki is ready
curl http://localhost:3100/ready

# Check if Grafana is ready
curl http://localhost:3000/api/health

# View all running containers
docker ps | grep -E "loki|grafana|promtail|user|travel|payment|gateway"
```

## View Logs

```bash
# View Loki logs
docker logs loki -f

# View Grafana logs
docker logs grafana -f

# View Promtail logs
docker logs promtail -f

# View service logs
docker logs user-service -f
docker logs travel-service -f
docker logs payment-service -f
docker logs apigateway-service -f
```

## Troubleshooting

**Logs not appearing in Grafana?**
```bash
# Check if Promtail is collecting logs
docker logs promtail

# Check Loki logs
docker logs loki

# Check if labels are available
curl -s http://localhost:3100/loki/api/v1/labels | jq
```

**Can't access Grafana?**
```bash
# Check if Grafana is running
docker ps | grep grafana

# Restart Grafana
docker restart grafana

# View Grafana logs
docker logs grafana -f
```

## Clean Up

```bash
# Stop all containers
docker compose -f docker-compose.logging.yml down
docker compose down

# Remove volumes (⚠️ deletes all data)
docker compose -f docker-compose.logging.yml down -v

# Remove network
docker network rm microservices-network
```
