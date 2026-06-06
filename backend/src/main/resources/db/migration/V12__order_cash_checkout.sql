-- Cash checkout support. Guests can place orders without an account, so customer_id
-- becomes optional and guest contact details are captured on the order itself.
-- Payment is cash-only for now: the order records the method and a payment status
-- an admin flips to PAID on collection.
ALTER TABLE orders ALTER COLUMN customer_id DROP NOT NULL;

ALTER TABLE orders
    ADD COLUMN customer_name   VARCHAR(255),
    ADD COLUMN customer_email  VARCHAR(255),
    ADD COLUMN customer_phone  VARCHAR(40),
    ADD COLUMN payment_method  VARCHAR(30) NOT NULL DEFAULT 'CASH',
    ADD COLUMN payment_status  VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    ADD COLUMN idempotency_key VARCHAR(80);

-- Idempotent order creation: a client's idempotency key is unique per tenant, so a
-- double-submit can never create two orders. Partial index ignores NULL keys.
CREATE UNIQUE INDEX idx_orders_idem ON orders(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
