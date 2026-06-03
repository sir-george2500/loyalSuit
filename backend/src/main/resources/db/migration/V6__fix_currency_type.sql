-- The `currency` columns were created as CHAR(3) but the JPA entities map them as
-- VARCHAR(3) (String with length=3). Align the DB so Hibernate schema-validation passes.
ALTER TABLE orders ALTER COLUMN currency TYPE VARCHAR(3);
ALTER TABLE payments ALTER COLUMN currency TYPE VARCHAR(3);
