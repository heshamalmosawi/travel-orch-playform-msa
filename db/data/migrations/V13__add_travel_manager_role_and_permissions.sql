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
