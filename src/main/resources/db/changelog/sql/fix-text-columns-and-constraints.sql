-- liquibase formatted sql

-- changeset kengine:fix-text-columns-and-constraints-v1
-- Fix VARCHAR columns that might be too small and convert them to TEXT for larger content

-- Business Rules table: Convert rule_name to TEXT (was VARCHAR(500))
ALTER TABLE knowledge.knowledge_business_rules
    ALTER COLUMN rule_name TYPE TEXT,
    ALTER COLUMN rule_type TYPE TEXT,
    ALTER COLUMN priority TYPE TEXT;

-- Workflow Steps table: Convert actor to TEXT (was VARCHAR(255))
ALTER TABLE knowledge.knowledge_workflow_steps
    ALTER COLUMN actor TYPE TEXT;

-- Data Fields table: Convert field names to TEXT
ALTER TABLE knowledge.knowledge_data_fields
    ALTER COLUMN field_name TYPE TEXT,
    ALTER COLUMN field_type TYPE TEXT;

-- Workflows table: Convert workflow_name and actor to TEXT
ALTER TABLE knowledge.knowledge_workflows
    ALTER COLUMN workflow_name TYPE TEXT,
    ALTER COLUMN actor TYPE TEXT;

-- Modules table: Convert potentially long fields to TEXT
ALTER TABLE knowledge.knowledge_modules
    ALTER COLUMN module_name TYPE TEXT,
    ALTER COLUMN module_type TYPE TEXT,
    ALTER COLUMN technology TYPE TEXT,
    ALTER COLUMN owner TYPE TEXT,
    ALTER COLUMN lifecycle TYPE TEXT;

-- Components table: Convert potentially long fields to TEXT
ALTER TABLE knowledge.knowledge_components
    ALTER COLUMN component_name TYPE TEXT,
    ALTER COLUMN component_type TYPE TEXT,
    ALTER COLUMN category TYPE TEXT,
    ALTER COLUMN technology TYPE TEXT,
    ALTER COLUMN capability TYPE TEXT;

-- Domains and Subdomains: Convert names to TEXT
ALTER TABLE knowledge.knowledge_domains
    ALTER COLUMN domain_name TYPE TEXT;

ALTER TABLE knowledge.knowledge_subdomains
    ALTER COLUMN subdomain_name TYPE TEXT;

-- APIs table: Convert potentially long fields to TEXT
ALTER TABLE knowledge.knowledge_apis
    ALTER COLUMN api_name TYPE TEXT,
    ALTER COLUMN api_type TYPE TEXT,
    ALTER COLUMN http_method TYPE TEXT;

-- Data Models table: Convert model fields to TEXT
ALTER TABLE knowledge.knowledge_data_models
    ALTER COLUMN model_name TYPE TEXT,
    ALTER COLUMN model_type TYPE TEXT;

-- Resources table: Convert resource fields to TEXT
ALTER TABLE knowledge.knowledge_resources
    ALTER COLUMN resource_name TYPE TEXT,
    ALTER COLUMN resource_type TYPE TEXT,
    ALTER COLUMN provider TYPE TEXT,
    ALTER COLUMN environment TYPE TEXT;

-- Integrations table: Convert integration fields to TEXT
ALTER TABLE knowledge.knowledge_integrations
    ALTER COLUMN source_system TYPE TEXT,
    ALTER COLUMN target_system TYPE TEXT,
    ALTER COLUMN protocol TYPE TEXT;

-- Relationships table: Convert entity type and name fields to TEXT
ALTER TABLE knowledge.knowledge_relationships
    ALTER COLUMN source_entity_type TYPE TEXT,
    ALTER COLUMN source_name TYPE TEXT,
    ALTER COLUMN target_entity_type TYPE TEXT,
    ALTER COLUMN target_name TYPE TEXT,
    ALTER COLUMN relationship_type TYPE TEXT;

-- Knowledge Chunks table: Convert chunk_type and entity_type to TEXT
ALTER TABLE knowledge.knowledge_chunks
    ALTER COLUMN chunk_type TYPE TEXT,
    ALTER COLUMN entity_type TYPE TEXT;

-- Knowledge Facts table: Convert fact fields to TEXT
ALTER TABLE knowledge.knowledge_facts
    ALTER COLUMN fact_type TYPE TEXT,
    ALTER COLUMN fact_key TYPE TEXT,
    ALTER COLUMN title TYPE TEXT,
    ALTER COLUMN priority TYPE TEXT,
    ALTER COLUMN source_entity_type TYPE TEXT;

-- Projects table: Convert potentially long fields to TEXT
ALTER TABLE knowledge.projects
    ALTER COLUMN project_name TYPE TEXT,
    ALTER COLUMN title TYPE TEXT,
    ALTER COLUMN source_bucket TYPE TEXT,
    ALTER COLUMN status TYPE TEXT;

-- Documents table: Convert document fields to TEXT
ALTER TABLE knowledge.documents
    ALTER COLUMN document_name TYPE TEXT,
    ALTER COLUMN document_type TYPE TEXT,
    ALTER COLUMN source_bucket TYPE TEXT,
    ALTER COLUMN source_checksum TYPE TEXT,
    ALTER COLUMN content_hash TYPE TEXT,
    ALTER COLUMN mime_type TYPE TEXT,
    ALTER COLUMN file_type TYPE TEXT,
    ALTER COLUMN title TYPE TEXT;

-- Ingestion Documents table
ALTER TABLE knowledge.ingestion_documents
    ALTER COLUMN document_name TYPE TEXT,
    ALTER COLUMN document_type TYPE TEXT,
    ALTER COLUMN extraction_status TYPE TEXT;

-- Source Chunks table
ALTER TABLE knowledge.knowledge_source_chunks
    ALTER COLUMN embedding_status TYPE TEXT;

-- Process tracking table
ALTER TABLE knowledge.knowledge_engine_processes
    ALTER COLUMN process_type TYPE TEXT,
    ALTER COLUMN status TYPE TEXT;

-- Ensure all JSONB columns can handle large JSON data
-- (JSONB doesn't have size limits, so this is just for documentation)
COMMENT ON COLUMN knowledge.knowledge_workflow_steps.input_parameters IS 'JSONB field for workflow step input parameters - no size limit';
COMMENT ON COLUMN knowledge.knowledge_workflow_steps.output_parameters IS 'JSONB field for workflow step output parameters - no size limit';
COMMENT ON COLUMN knowledge.knowledge_workflow_steps.business_rules IS 'JSONB field for business rules - no size limit';
COMMENT ON COLUMN knowledge.knowledge_data_fields.business_rules IS 'JSONB field for data field business rules - no size limit';

-- rollback ALTER TABLE statements to revert back to VARCHAR types would go here if needed
