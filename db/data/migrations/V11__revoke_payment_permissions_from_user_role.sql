DELETE FROM role_permissions
WHERE role_id = (SELECT id FROM roles WHERE name = 'user')
  AND permission_id IN (
    SELECT id FROM permissions WHERE name IN ('payments.read', 'payments.write')
  );
