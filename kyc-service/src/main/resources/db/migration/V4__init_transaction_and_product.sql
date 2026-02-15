CREATE EXTENSION IF NOT EXISTS "pgcrypto";


CREATE TYPE transaction_type AS ENUM (
    'DEPOSIT',
    'WITHDRAWAL',
    'TRANSFER',
    'CRYPTO_PURCHASE',
    'CRYPTO_SALE',
    'PAYMENT'
);

CREATE TABLE financial_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    customer_id VARCHAR(255) NOT NULL,

    amount float(53) NOT NULL CHECK (amount >= 0),

    currency varchar(3) NOT NULL,

    type transaction_type NOT NULL,

    source_country varchar(2),

    destination_country varchar(2),

    counterparty_name VARCHAR(255),

    timestamp TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);


CREATE INDEX idx_ft_customer_id ON financial_transactions(customer_id);
CREATE INDEX idx_ft_timestamp ON financial_transactions(timestamp);
CREATE INDEX idx_ft_type ON financial_transactions(type);
CREATE INDEX idx_ft_source_country ON financial_transactions(source_country);
CREATE INDEX idx_ft_destination_country ON financial_transactions(destination_country);

CREATE TYPE product_type AS ENUM (
    'SAVINGS_ACCOUNT',
    'SALARY_ACCOUNT',
    'INVESTMENT_ACCOUNT',
    'BROKERAGE',
    'CRYPTO_TRADING',
    'INTERNATIONAL_WIRE',
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



CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(255) NOT NULL UNIQUE,

    type product_type NOT NULL,

    base_risk_level risk_level NOT NULL,

    risk_score INTEGER NOT NULL CHECK (risk_score >= 0),

    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);


