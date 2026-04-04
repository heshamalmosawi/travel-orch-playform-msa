-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create role_permissions table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Create user_roles table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

-- Insert default roles
INSERT INTO roles (name, description) VALUES 
('admin', 'Full system access'),
('user', 'Standard user access'),
('service', 'Internal service account for inter-service communication')
ON CONFLICT (name) DO NOTHING;

-- Insert default permissions
INSERT INTO permissions (name, resource, action, description) VALUES 
('users.read', 'users', 'read', 'View user information'),
('users.write', 'users', 'write', 'Create and update users'),
('users.delete', 'users', 'delete', 'Delete users'),
('travels.read', 'travels', 'read', 'View travel information'),
('travels.write', 'travels', 'write', 'Create and update travels'),
('travels.delete', 'travels', 'delete', 'Delete travels'),
('destinations.read', 'destinations', 'read', 'View destination information'),
('activities.read', 'activities', 'read', 'View activity information'),
('accommodations.read', 'accommodations', 'read', 'View accommodation information'),
('transportation.read', 'transportation', 'read', 'View transportation options'),
('payments.read', 'payments', 'read', 'View payment information'),
('payments.write', 'payments', 'write', 'Process payments'),
('payment_methods.read', 'payment_methods', 'read', 'View payment methods'),
('payment_methods.write', 'payment_methods', 'write', 'Create and update payment methods'),
('payment_methods.delete', 'payment_methods', 'delete', 'Delete payment methods'),
('admin.all', 'admin', 'all', 'Full administrative access')
ON CONFLICT (name) DO NOTHING;

-- Assign all permissions to admin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign user permissions to user role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'user' 
  AND p.name IN (
    'users.read', 'users.write',
    'travels.read', 'travels.write', 'travels.delete',
    'destinations.read', 'activities.read', 
    'accommodations.read', 'transportation.read',
    'payments.read', 'payments.write'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign service permissions (minimal, for inter-service calls)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'service' 
  AND p.name IN (
    'users.read', 'travels.read', 'payments.write'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON permissions(resource);
CREATE INDEX IF NOT EXISTS idx_permissions_action ON permissions(action);
