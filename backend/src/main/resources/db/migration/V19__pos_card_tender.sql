-- Card / split tender at the POS. A sale can now be paid with cash, card, or both:
--   card_amount = the portion charged on the card terminal (NOT an online gateway).
-- Only cash reaches the drawer, so a shift now records cash_sales (the cash portion of
-- its sales) and reconciles against that: expected_cash = opening_float + cash_sales.
-- sales_total stays the gross (cash + card) for reporting.

-- Existing rows are cash-only, so card_amount defaults to 0.
ALTER TABLE pos_sales ADD COLUMN card_amount DECIMAL(10, 2) NOT NULL DEFAULT 0;

-- Nullable like the other close-time reconciliation columns (set when the shift closes).
ALTER TABLE pos_shifts ADD COLUMN cash_sales DECIMAL(10, 2);
