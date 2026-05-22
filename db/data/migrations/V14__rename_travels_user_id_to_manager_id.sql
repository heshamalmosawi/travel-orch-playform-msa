-- Travels are standalone packages owned by a travel manager.
-- Rename user_id -> manager_id to reflect ownership semantics.
-- NOT NULL is preserved by the rename; every travel must have a manager.
ALTER TABLE travels RENAME COLUMN user_id TO manager_id;

ALTER INDEX IF EXISTS idx_travels_user RENAME TO idx_travels_manager;
