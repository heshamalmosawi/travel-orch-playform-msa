#!/bin/bash
# Test script to validate replica configuration

echo "=== Testing Replica Configuration ==="
echo ""

# Check if all required files exist
echo "1. Checking files..."
FILES=(
  "ansible/roles/microservices/templates/docker-compose.j2"
  "ansible/roles/microservices/defaults/main.yml"
  "ansible/group_vars/all.yml"
  "ansible/roles/microservices/tasks/main.yml"
)

for file in "${FILES[@]}"; do
  if [ -f "$file" ]; then
    echo "  ✓ $file"
  else
    echo "  ✗ $file (NOT FOUND)"
    exit 1
  fi
done

echo ""
echo "2. Validating template structure..."
if grep -q "microservices_replicas" ansible/roles/microservices/templates/docker-compose.j2; then
  echo "  ✓ Template uses microservices_replicas variable"
else
  echo "  ✗ Template missing microservices_replicas variable"
  exit 1
fi

if grep -q "deploy:" ansible/roles/microservices/templates/docker-compose.j2; then
  echo "  ✓ Template includes deploy configuration for replicas"
else
  echo "  ✗ Template missing deploy configuration"
  exit 1
fi

echo ""
echo "3. Checking replica configuration in defaults..."
if grep -q "microservices_replicas:" ansible/roles/microservices/defaults/main.yml; then
  echo "  ✓ Defaults file includes microservices_replicas"
else
  echo "  ✗ Defaults file missing microservices_replicas"
  exit 1
fi

echo ""
echo "4. Checking replica configuration in group_vars..."
if grep -q "replicas:" ansible/group_vars/all.yml; then
  echo "  ✓ group_vars/all.yml includes replicas configuration"
else
  echo "  ✗ group_vars/all.yml missing replicas configuration"
  exit 1
fi

echo ""
echo "5. Checking playbook syntax..."
cd ansible
if ansible-playbook playbooks/deploy_services.yml --syntax-check > /dev/null 2>&1; then
  echo "  ✓ Playbook syntax is valid"
else
  echo "  ✗ Playbook has syntax errors"
  exit 1
fi
cd ..

echo ""
echo "=== All Checks Passed! ==="
echo ""
echo "Configuration Summary:"
echo "  - Template: ansible/roles/microservices/templates/docker-compose.j2"
echo "  - Defaults: ansible/roles/microservices/defaults/main.yml"
echo "  - Config:   ansible/group_vars/all.yml"
echo ""
echo "Current Replica Configuration:"
echo "  Frontend:     $(awk '/microservices_replicas:/,/^[^ ]/ { if ($1 == "frontend:") print $2 }' ansible/group_vars/all.yml)"
echo "  Eureka:       $(awk '/microservices_replicas:/,/^[^ ]/ { if ($1 == "eureka:") print $2 }' ansible/group_vars/all.yml)"
echo "  Gateway:      $(awk '/microservices_replicas:/,/^[^ ]/ { if ($1 == "gateway:") print $2 }' ansible/group_vars/all.yml)"
echo "  User Service: $(awk '/microservices_replicas:/,/^[^ ]/ { if ($1 == "user:") print $2 }' ansible/group_vars/all.yml)"
echo "  Travel:       $(awk '/microservices_replicas:/,/^[^ ]/ { if ($1 == "travel:") print $2 }' ansible/group_vars/all.yml)"
echo "  Payment:      $(awk '/microservices_replicas:/,/^[^ ]/ { if ($1 == "payment:") print $2 }' ansible/group_vars/all.yml)"
echo ""
echo "To deploy with custom replicas, run:"
echo "  ansible-playbook ansible/playbooks/deploy_services.yml -e '{\"microservices_replicas\":{\"frontend\":1,\"eureka\":1,\"gateway\":1,\"user\":5,\"travel\":5,\"payment\":5}}'"
