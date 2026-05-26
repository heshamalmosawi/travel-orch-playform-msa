-- The payment_transactions status check predates the 'refunded' state used by
-- the cancel/refund flow (PaymentStatus enum: pending, processing, completed,
-- failed, refunded). Without 'refunded' in the constraint, every refund commit
-- fails with payment_transactions_status_check and the endpoint returns 500.
-- Widen the constraint to match the enum.
ALTER TABLE payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_status_check;
ALTER TABLE payment_transactions ADD CONSTRAINT payment_transactions_status_check
    CHECK (status IN ('pending', 'processing', 'completed', 'failed', 'refunded'));
