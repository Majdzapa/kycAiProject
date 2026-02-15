-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    customer_id VARCHAR(255) UNIQUE,
    enabled BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    mfa_enabled BOOLEAN DEFAULT false,
    mfa_secret VARCHAR(255),
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User roles table
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- KYC Documents table with vector embeddings
CREATE TABLE kyc_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    content_text TEXT,
    storage_path VARCHAR(500),
    metadata JSONB,
    verification_status VARCHAR(50) DEFAULT 'PENDING',
    confidence_score float(53) ,
    extracted_data JSONB,
    risk_level VARCHAR(50),
    
    -- GDPR compliance fields
    data_retention_until TIMESTAMP,
    processing_legal_basis VARCHAR(50),
    consent_version VARCHAR(20),
    consent_timestamp TIMESTAMP,
    anonymized BOOLEAN DEFAULT false,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processed_by VARCHAR(255)
);

-- Create HNSW index for fast similarity search on embeddings
-- Note: The embedding vector is managed by LangChain4j PgVectorEmbeddingStore

-- Regular indexes for customer lookups
CREATE INDEX idx_kyc_docs_customer_id ON kyc_documents(customer_id);
CREATE INDEX idx_kyc_docs_retention_date ON kyc_documents(data_retention_until);
CREATE INDEX idx_kyc_docs_status ON kyc_documents(verification_status);
CREATE INDEX idx_kyc_docs_risk_level ON kyc_documents(risk_level);

-- Audit log for GDPR compliance
CREATE TABLE kyc_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    legal_basis VARCHAR(50),
    data_categories TEXT[],
    request_id VARCHAR(100),
    details JSONB,
    success BOOLEAN,
    error_message TEXT
);

-- Indexes for audit log queries
CREATE INDEX idx_audit_customer_id ON kyc_audit_log(customer_id);
CREATE INDEX idx_audit_performed_at ON kyc_audit_log(performed_at);
CREATE INDEX idx_audit_action ON kyc_audit_log(action);

-- RAG Knowledge Base (regulatory docs, procedures)
CREATE TABLE knowledge_base (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(100),
    title VARCHAR(500),
    content TEXT,
    source_url VARCHAR(1000),
    effective_date DATE,
    version VARCHAR(20),
    language VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ingested_by VARCHAR(255)
);

CREATE INDEX idx_kb_category ON knowledge_base(category);
CREATE INDEX idx_kb_language ON knowledge_base(language);

-- Consent management table
CREATE TABLE consent_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255) NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    version VARCHAR(20) NOT NULL,
    granted BOOLEAN NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    withdrawn_at TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    data_categories TEXT[],
    UNIQUE(customer_id, purpose, version)
);

CREATE INDEX idx_consent_customer_id ON consent_records(customer_id);
CREATE INDEX idx_consent_purpose ON consent_records(purpose);

-- Function for automatic data purging (GDPR Article 17)
CREATE OR REPLACE FUNCTION purge_expired_data()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM kyc_documents
    WHERE data_retention_until < CURRENT_TIMESTAMP
    AND anonymized = false;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    INSERT INTO kyc_audit_log (action, performed_by, legal_basis, details, success)
    VALUES ('AUTO_PURGE', 'SYSTEM', 'GDPR_ARTICLE_17', 
            jsonb_build_object('deleted_count', deleted_count), true);
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Insert default admin user (password: admin123 - change in production!)
INSERT INTO users (username, email, password_hash, first_name, last_name)
VALUES ('admin', 'admin@kyc.ai', '$2a$10$GRLdNijSQMUvl/au9ShLOmqnWQmG9tO.bX3V79o/6M6oV8e3/q6ue', 'System', 'Administrator');


INSERT INTO public.users
(id, username, email, password_hash, first_name, last_name, customer_id, enabled, email_verified, mfa_enabled, mfa_secret, last_login_at, failed_login_attempts, locked_until, created_at, updated_at)
VALUES('65096744-ca07-471d-bbaa-7981a7e228d9'::uuid, 'hedfi', 'hedfimajd@gmail.com', '$2a$10$EmCyDvAiXzhtvcfzDYm4oOZxQjlMgFZgweXbHkBHSQTFuh4Mec72G', 'Majd', 'Hedfi', 'CUST-30C2A8CD', true, false, false, NULL, '2026-02-11 19:09:19.596', 0, NULL, '2026-02-09 22:02:47.066', '2026-02-11 19:09:19.772');
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin';
