INSERT INTO roles (name, description) VALUES
('travel_manager', 'Travel manager — CRUD travels, manage subscribers, analytics')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (name, resource, action, description) VALUES
('subscriptions.read', 'subscriptions', 'read', 'View subscriptions'),
('subscriptions.write', 'subscriptions', 'write', 'Subscribe/unsubscribe'),
('feedbacks.read', 'feedbacks', 'read', 'View feedback'),
('feedbacks.write', 'feedbacks', 'write', 'Submit feedback'),
('reports.read', 'reports', 'read', 'View reports'),
('reports.write', 'reports', 'write', 'File reports')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'user'
  AND p.name IN (
    'subscriptions.read', 'subscriptions.write',
    'feedbacks.read', 'feedbacks.write',
    'reports.read', 'reports.write'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'travel_manager'
  AND p.name IN (
    'users.read',
    'travels.read', 'travels.write', 'travels.delete',
    'destinations.read', 'destinations.write',
    'activities.read', 'accommodations.read', 'transportation.read',
    'payments.read',
    'subscriptions.read',
    'feedbacks.read',
    'reports.read', 'reports.write'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Collapse user_roles join table into a single role_id column on users
ALTER TABLE users ADD COLUMN role_id BIGINT REFERENCES roles(id);

UPDATE users SET role_id = COALESCE(
    (SELECT role_id FROM user_roles WHERE user_id = users.id LIMIT 1),
    (SELECT id FROM roles WHERE name = 'user')
);

ALTER TABLE users ALTER COLUMN role_id SET NOT NULL;

DROP TABLE IF EXISTS user_roles;
DROP INDEX IF EXISTS idx_user_roles_user_id;
DROP INDEX IF EXISTS idx_user_roles_role_id;
