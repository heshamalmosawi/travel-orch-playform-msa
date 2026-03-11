# Travel Orchestrator Database Services

Docker Compose setup for PostgreSQL and Neo4j databases.

## Services

### PostgreSQL (`db`)
- **Image**: `postgres:latest`
- **Container**: `postgres_container`
- **Port**: `5432:5432`
- **Database**: `mydatabase`
- **User**: `myuser`
- **Password**: `mypassword`
- **Volume**: `postgres_data` → `/var/lib/postgresql`

### Neo4j (`neo4j`)
- **Image**: `neo4j:latest`
- **Container**: `neo4j-local`
- **Ports**: `7474:7474` (HTTP), `7687:7687` (Bolt)
- **Auth**: `neo4j/password123`
- **Volumes**: 
  - `neo4j_data` → `/var/lib/neo4j/data`
  - `neo4j_logs` → `/var/lib/neo4j/logs`

## Usage

### Start Services
```bash
docker compose up -d
```
Runs in detached mode (background).

### Stop Services
```bash
docker compose down
```
Stops and removes containers. **Data is preserved in volumes.**

### Stop and Delete Data
```bash
docker compose down -v
```
Stops containers and removes volumes. **All data will be lost.**

### Pull Latest Images
```bash
docker compose pull
```
Downloads latest images.

### Force Fresh Image Download
```bash
docker compose down
docker rmi postgres:latest neo4j:latest
docker compose pull
docker compose up -d
```
Removes local images and pulls fresh ones.

### View Status
```bash
docker compose ps
```

### View Logs
```bash
docker compose logs -f
```
Follow logs in real-time.

## Why `docker compose build` Doesn't Work

This setup uses pre-built images from Docker Hub (`image:` directives), not custom Dockerfiles. There's nothing to build. Only `docker compose up` is needed to pull and run images.

## Connecting to Databases

### PostgreSQL
```bash
docker exec -it postgres_container psql -U myuser -d mydatabase
```

### Neo4j Browser
Access at: `http://localhost:7474`
- Username: `neo4j`
- Password: `password123`

## Fresh Start (Delete All Data)
```bash
docker compose down -v
docker rmi postgres:latest neo4j:latest
docker compose pull
docker compose up -d
```

## Troubleshooting

### Port Already in Use
If ports 5432 or 7474/7687 are occupied:
```bash
lsof -i :5432
# Kill the process or change ports in docker-compose.yml
```

### Data Persistence
Data persists in named volumes even after `docker compose down`. Use `-v` flag to delete data.

### Check Container Health
```bash
docker compose ps
docker compose logs [service_name]
```
