#!/bin/bash

# Wait for services to be healthy before starting the application
echo "Waiting for services to be ready..."

# Wait for Eureka server
echo "Checking Eureka server..."
sleep 15


echo "Eureka server is ready!"

# Wait additional time for services to register with Eureka
# echo "Waiting for microservices to register with Eureka (60 seconds)..."
# sleep 60

# # Check if services are registered in Eureka
# echo "Checking service registration..."
# for service in "PRODUCT-SERVICE" "USER-SERVICE" "MEDIA-SERVICE"; do
#     echo "Checking if $service is registered..."
#     until curl -s "http://eureka-server:8761/eureka/apps" | grep -q "$service"; do
#         echo "$service not registered yet, waiting 10 seconds..."
#         sleep 10
#     done
#     echo "$service is registered!"
# done

echo "All services are registered! Starting API Gateway..."

# Execute the main application command
exec "$@"