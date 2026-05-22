-- Travels are standalone packages owned by a travel manager.
-- Rename user_id -> manager_id to reflect ownership semantics.
-- NOT NULL is preserved by the rename; every travel must have a manager.
ALTER TABLE travels RENAME COLUMN user_id TO manager_id;

ALTER INDEX IF EXISTS idx_travels_user RENAME TO idx_travels_manager;

-- Duration is redundant: it can always be derived from end_date - start_date.
ALTER TABLE travels DROP COLUMN IF EXISTS duration_days;
