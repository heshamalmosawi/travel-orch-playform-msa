# Databases Role

Deploys PostgreSQL and Neo4j databases using Docker Compose for the Travel Management System.

## Requirements

- Ansible 2.14+
- Docker and Docker Compose v2 installed
- `community.docker` collection installed (`ansible-galaxy collection install community.docker`)
- db/docker-compose.yml exists in project root
- Vault accessible and configured (for secret retrieval)
- Write permissions for data directory

## Role Variables

### Default Variables

| Variable | Default | Description |
|-----------|----------|-------------|
| `db_docker_compose_path` | `"{{ project_root }}/db/docker-compose.yml"` | Path to Docker Compose file |
| `db_network_name` | `"{{ network_name }}"` | Docker network name |
| `db_data_root` | `"{{ project_root }}/db/data"` | Data directory for volumes |
| `postgres_container_name` | `"{{ postgres.container_name }}"` | PostgreSQL container name |
| `postgres_image` | `"{{ postgres.version }}"` | PostgreSQL Docker image |
| `postgres_port` | `"{{ postgres.port }}"` | PostgreSQL port |
| `postgres_database` | `"{{ postgres.database }}"` | PostgreSQL database name |
| `postgres_user` | `"{{ postgres.user }}"` | PostgreSQL user |
| `postgres_password_secret_path` | `"secret/data/travel-system/postgres"` | Vault secret path for PostgreSQL password |
| `neo4j_container_name` | `"{{ neo4j.container_name }}"` | Neo4j container name |
| `neo4j_image` | `"neo4j:{{ neo4j.version }}"` | Neo4j Docker image |
| `neo4j_http_port` | `"{{ neo4j.ports.http }}"` | Neo4j HTTP port |
| `neo4j_bolt_port` | `"{{ neo4j.ports.bolt }}"` | Neo4j Bolt port |
| `neo4j_auth` | `"neo4j/password123"` | Neo4j authentication |
| `neo4j_password_secret_path` | `"secret/data/travel-system/neo4j"` | Vault secret path for Neo4j password |
| `db_health_check_retries` | `10` | Number of health check retries |
| `db_health_check_delay` | `5` | Delay between health checks (seconds) |
| `db_startup_wait_timeout` | `60` | Wait time for database startup (seconds) |

### Variable Overrides

These can be overridden in `group_vars/all.yml`:

```yaml
postgres:
  port: 5432
  database: mydatabase
  user: myuser
  container_name: postgres_container

neo4j:
  ports:
    http: 7474
    bolt: 7687
  version: "5.0"
  container_name: neo4j-local
```

## Dependencies

- community.docker.docker_compose_v2 >= 2.0

## Example Usage

### Using in Playbook

```yaml
---
- hosts: localhost
  become: true
  roles:
    - databases
```

### Standalone Execution

```bash
ansible-playbook playbooks/test_databases.yml
```

## Database Connection Strings

After deployment, databases will be accessible at:

### PostgreSQL
```
Host: localhost
Port: 5432
Database: mydatabase
User: myuser
Password: [from Vault or default]

Connection String:
jdbc:postgresql://myuser:password@localhost:5432/mydatabase
```

### Neo4j
```
HTTP UI: http://localhost:7474
Bolt: bolt://localhost:7687
User: neo4j
Password: [from Vault or default]

Connection String:
bolt://neo4j:password@localhost:7687
```

## Tasks

This role performs the following tasks:

1. **Verify Docker Compose File** - Checks that db/docker-compose.yml exists
2. **Create Data Directory** - Ensures db/data/ directory exists for volume persistence
3. **Check Vault Status** - Verifies Vault is accessible for secret retrieval
4. **Retrieve PostgreSQL Password** - Gets password from Vault or uses default
5. **Retrieve Neo4j Password** - Gets password from Vault or uses default
6. **Deploy Databases** - Uses Docker Compose v2 to deploy both databases
7. **Wait for PostgreSQL** - Waits up to 60s for PostgreSQL to be ready
8. **Wait for Neo4j** - Waits up to 60s for Neo4j to be ready
9. **Verify Containers** - Confirms both containers are running
10. **Display Summary** - Shows deployment information

## Important Notes

- **Vault Integration**: Role retrieves database passwords from Vault if available
- **Data Persistence**: Named volumes (postgres_data, neo4j_data) persist data across container restarts
- **Health Checks**: Both databases have health checks configured
- **External Network**: Uses microservices-network for service communication
- **Wait Strategy**: Waits for ports to be accessible before continuing
- **Idempotent**: Can be run multiple times safely

## Security Considerations

- Database passwords are retrieved from Vault when available
- Default passwords should be changed for production
- Neo4j HTTP UI (7474) should be firewalled in production
- Only expose necessary ports (5432, 7474, 7687)
- Data volumes should be backed up regularly

## Troubleshooting

**Issue**: "db/docker-compose.yml not found"
- **Solution**: Ensure file exists in project root at: `db/docker-compose.yml`

**Issue**: "permission denied accessing data directory"
- **Solution**: Check permissions on `db/data/` directory

**Issue**: "Vault not accessible"
- **Solution**: Ensure Vault is running: `vault status`

**Issue**: "PostgreSQL connection refused"
- **Solution**: Wait longer for startup, check `docker logs postgres_container`

**Issue**: "Neo4j connection refused"
- **Solution**: Wait longer for startup, check `docker logs neo4j-local`

**Issue**: "Port already in use"
- **Solution**: Stop existing containers: `docker compose down` in db/ directory

**Issue**: "community.docker.docker_compose_v2 module not found"
- **Solution**: Install the collection: `ansible-galaxy collection install community.docker`

**Issue**: Health checks fail repeatedly
- **Solution**: Increase `db_health_check_delay` and `db_health_check_retries`

**Issue**: Containers not starting
- **Solution**: Check Docker logs: `docker logs <container_name>`

## License

MIT
