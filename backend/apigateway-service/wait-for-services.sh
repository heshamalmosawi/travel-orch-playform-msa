#!/bin/bash

echo "Waiting for backend services to be ready..."

echo "Waiting for user-service on port 8082..."
until curl -sfS http://user-service:8082/actuator/health > /dev/null 2>&1; do
  sleep 2
done
echo "user-service is ready!"

echo "Waiting for travel-service on port 8083..."
until curl -sfS http://travel-service:8083/actuator/health > /dev/null 2>&1; do
  sleep 2
done
echo "travel-service is ready!"

echo "Waiting for payment-service on port 8084..."
until curl -sfS http://payment-service:8084/actuator/health > /dev/null 2>&1; do
  sleep 2
done
echo "payment-service is ready!"

echo "All services are ready! Starting API Gateway..."

exec "$@"
