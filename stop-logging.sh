#!/bin/bash

# Centralized Logging Shutdown Script
# This script stops the logging stack

set -e

echo "=== Stopping Centralized Logging Stack ==="

# Stop logging stack
echo "Stopping logging stack..."
docker compose -f docker-compose.logging.yml down

echo "✓ Logging stack stopped"
echo ""
echo "Note: microservices-network is kept running for microservices"
echo "To remove the network, run: docker network rm microservices-network"
