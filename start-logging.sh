#!/bin/bash

# Centralized Logging Startup Script
# This script ensures the microservices network exists and starts the logging stack

set -e

echo "=== Centralized Logging Startup ==="

# Create microservices network if it doesn't exist
echo "Checking for microservices-network..."
if ! docker network inspect microservices-network >/dev/null 2>&1; then
    echo "Creating microservices-network..."
    docker network create microservices-network
else
    echo "microservices-network already exists"
fi

# Start logging stack
echo ""
echo "Starting logging stack..."
docker compose -f docker-compose.logging.yml up -d

# Wait for Loki to be ready
echo ""
echo "Waiting for Loki to be ready..."
until curl -s http://localhost:3100/ready >/dev/null 2>&1; do
    echo "Loki not ready yet... waiting..."
    sleep 2
done
echo "✓ Loki is ready"

# Wait for Grafana to be ready
echo "Waiting for Grafana to be ready..."
until curl -s http://localhost:3000/api/health >/dev/null 2>&1; do
    echo "Grafana not ready yet... waiting..."
    sleep 2
done
echo "✓ Grafana is ready"

echo ""
echo "=== Logging Stack Successfully Started ==="
echo ""
echo "Access points:"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Loki API: http://localhost:3100"
echo ""
echo "Next steps:"
echo "  1. Start microservices: docker compose up -d"
echo "  2. Generate traffic to see logs in Grafana"
echo "  3. View logs in Grafana > Explore > Select Loki datasource"
echo ""
