-- Travels are standalone packages owned by a travel manager.
-- Rename user_id -> manager_id to reflect ownership semantics.
-- NOT NULL is preserved by the rename; every travel must have a manager.
ALTER TABLE travels RENAME COLUMN user_id TO manager_id;

ALTER INDEX IF EXISTS idx_travels_user RENAME TO idx_travels_manager;

-- Duration is redundant: it can always be derived from end_date - start_date.
ALTER TABLE travels DROP COLUMN IF EXISTS duration_days;

-- Reduce travel lifecycle to three states: draft, confirmed, cancelled.
-- Map removed statuses to their nearest equivalent before adding the constraint.
UPDATE travels SET status = 'draft'     WHERE status = 'planned';
UPDATE travels SET status = 'confirmed' WHERE status = 'in_progress';
UPDATE travels SET status = 'cancelled' WHERE status = 'completed';

ALTER TABLE travels DROP CONSTRAINT IF EXISTS chk_travels_status;
ALTER TABLE travels ADD CONSTRAINT chk_travels_status
    CHECK (status IN ('draft', 'confirmed', 'cancelled'));
