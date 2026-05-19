-- Add role_id column to users table
ALTER TABLE users ADD COLUMN role_id BIGINT REFERENCES roles(id);

-- Migrate existing data: assign the first role from user_roles
UPDATE users SET role_id = (
    SELECT role_id FROM user_roles WHERE user_id = users.id LIMIT 1
);

-- Make role_id NOT NULL after migration
ALTER TABLE users ALTER COLUMN role_id SET NOT NULL;

-- Drop user_roles table and indexes
DROP TABLE IF EXISTS user_roles;
DROP INDEX IF EXISTS idx_user_roles_user_id;
DROP INDEX IF EXISTS idx_user_roles_role_id;