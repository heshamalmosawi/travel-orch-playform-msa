INSERT INTO permissions (name, resource, action, description) VALUES 
('destinations.write', 'destinations', 'write', 'Create and update destinations'),
('destinations.delete', 'destinations', 'delete', 'Delete destinations')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'admin'
  AND p.name IN ('destinations.write', 'destinations.delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'user' 
  AND p.name IN ('destinations.write', 'destinations.delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

DELETE FROM role_permissions
WHERE role_id = (SELECT id FROM roles WHERE name = 'user')
  AND permission_id IN (
    SELECT id FROM permissions WHERE name IN ('travels.write', 'travels.delete')
  );
