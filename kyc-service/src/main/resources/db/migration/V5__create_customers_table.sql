-- V5: Ensure relationships and indexes exist
-- This migration ensures consistency in case V4 was applied partially

-- The tables customers, products, and financial_transactions should already be created by V4.
-- We use DO blocks or IF NOT EXISTS to ensure safety.

DO $$
BEGIN
    -- Ensure customer_id column exists in products (it should from V4)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='products' AND column_name='customer_id') THEN
        ALTER TABLE products ADD COLUMN customer_id VARCHAR(255) REFERENCES customers(id);
    END IF;

    -- Ensure customer_id column exists in financial_transactions (it should from V4)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='financial_transactions' AND column_name='customer_id') THEN
        ALTER TABLE financial_transactions ADD COLUMN customer_id VARCHAR(255) REFERENCES customers(id);
    END IF;
END $$;

-- Indexes are already handled in V4, but we can re-verify or add others if needed
CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_products_customer_id ON products(customer_id);
CREATE INDEX IF NOT EXISTS idx_ft_customer_id ON financial_transactions(customer_id);
