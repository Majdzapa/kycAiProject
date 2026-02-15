-- Renaming text columns to match LangChain4j defaults
ALTER TABLE kyc_documents 
RENAME COLUMN content_text TO text;

ALTER TABLE knowledge_base 
RENAME COLUMN content TO text;

-- Renaming ID columns to match LangChain4j defaults (PgVectorEmbeddingStore expects embedding_id)
ALTER TABLE kyc_documents 
RENAME COLUMN id TO embedding_id;

ALTER TABLE knowledge_base 
RENAME COLUMN id TO embedding_id;

-- Adding embedding columns to kyc_documents and knowledge_base
-- Dimensions: 768 for nomic-embed-text
ALTER TABLE kyc_documents 
ADD COLUMN IF NOT EXISTS embedding vector(768);

ALTER TABLE knowledge_base 
ADD COLUMN IF NOT EXISTS embedding vector(768);

-- Add metadata column to knowledge_base if it doesn't exist
ALTER TABLE knowledge_base 
ADD COLUMN IF NOT EXISTS metadata JSONB;
