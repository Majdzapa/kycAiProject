-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- Create ENUM types
CREATE TYPE document_type AS ENUM (
    'PASSPORT',
    'ID_CARD',
    'DRIVERS_LICENSE',
    'PROOF_OF_ADDRESS',
    'BANK_STATEMENT',
    'TAX_RETURN',
    'UTILITY_BILL',
    'INCORPORATION_DOCS',
    'OTHER'
);

CREATE TYPE verification_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'VERIFIED',
    'REJECTED',
    'EXPIRED',
    'FLAGGED'
);

CREATE TYPE transaction_type AS ENUM (
    'DEPOSIT',
    'WITHDRAWAL',
    'TRANSFER',
    'CRYPTO_PURCHASE',
    'CRYPTO_SALE',
    'PAYMENT',
    'CASH_DEPOSIT',
    'CASH_WITHDRAWAL'
);

CREATE TYPE product_type AS ENUM (
    'SAVINGS_ACCOUNT',
    'SALARY_ACCOUNT',
    'INVESTMENT_ACCOUNT',
    'BROKERAGE',
    'CRYPTO_TRADING',
    'CRYPTO_CUSTODY',
    'PRIVATE_BANKING',
    'CORRESPONDENT_BANKING',
    'TRADE_FINANCE',
    'INTERNATIONAL_WIRE',
    'FOREIGN_EXCHANGE',
    'PRECIOUS_METALS_ACCOUNT',
    'CASH_MANAGEMENT',
    'BUSINESS_LENDING',
    'PREPAID_CARD',
    'LOAN',
    'CREDIT_CARD'
);

CREATE TYPE risk_level AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
);

CREATE TYPE audit_action AS ENUM (
    'CREATE',
    'VIEW',
    'UPDATE',
    'DELETE',
    'PROCESS',
    'EXPORT',
    'DATA_EXPORT',
    'DATA_DELETION',
    'AUTO_PURGE',
    'CONSENT_GRANTED',
    'CONSENT_WITHDRAWN'
);

CREATE TYPE legal_basis AS ENUM (
    'CONSENT',
    'CONTRACT',
    'LEGAL_OBLIGATION',
    'VITAL_INTERESTS',
    'PUBLIC_TASK',
    'LEGITIMATE_INTERESTS',
    'GDPR_ARTICLE_6_1_A',
    'GDPR_ARTICLE_6_1_B',
    'GDPR_ARTICLE_6_1_C',
    'GDPR_ARTICLE_17'
);

CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(255) PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    nationality VARCHAR(255) NOT NULL,
    residence_country VARCHAR(255) NOT NULL,
    occupation VARCHAR(255),
    industry_sector VARCHAR(255),
    income_range VARCHAR(255),
    source_of_wealth VARCHAR(255),
    entity_type VARCHAR(255),
    net_worth DOUBLE PRECISION,
    account_age INTEGER,
    expected_monthly_volume DOUBLE PRECISION,
    user_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Products table
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    customer_id VARCHAR(255) REFERENCES customers(id),
    product_type product_type NOT NULL,
    base_risk_level risk_level NOT NULL,
    risk_score INTEGER NOT NULL CHECK (risk_score >= 0),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Financial Transactions table
CREATE TABLE financial_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255) NOT NULL REFERENCES customers(id),
    amount DOUBLE PRECISION NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL,
    type transaction_type NOT NULL,
    source_country VARCHAR(2),
    destination_country VARCHAR(2),
    counterparty_name VARCHAR(255),
    timestamp TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);


CREATE INDEX idx_customers_user_id ON customers(user_id);
CREATE INDEX idx_customers_full_name ON customers(full_name);

CREATE INDEX idx_products_customer_id ON products(customer_id);
CREATE INDEX idx_products_product_type ON products(product_type);
CREATE INDEX idx_products_risk_level ON products(base_risk_level);

CREATE INDEX idx_ft_customer_id ON financial_transactions(customer_id);
CREATE INDEX idx_ft_timestamp ON financial_transactions(timestamp);
CREATE INDEX idx_ft_type ON financial_transactions(type);
CREATE INDEX idx_ft_source_country ON financial_transactions(source_country);
CREATE INDEX idx_ft_destination_country ON financial_transactions(destination_country);
