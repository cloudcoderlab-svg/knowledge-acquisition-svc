-- liquibase formatted sql

-- changeset kengine:project-centered-schema-v1
CREATE SCHEMA IF NOT EXISTS knowledge;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS knowledge.projects (
    project_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_name VARCHAR(500) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    title VARCHAR(500),
    description TEXT,
    definition TEXT,
    definition_embedding vector(768),
    summary TEXT,
    summary_embedding vector(768),
    summary_generated_at TIMESTAMPTZ,
    source_bucket VARCHAR(255),
    gcs_prefix TEXT,
    status VARCHAR(50) DEFAULT 'DRAFT',
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_projects_name_version UNIQUE (project_name, version)
);

CREATE TABLE IF NOT EXISTS knowledge.documents (
    source_document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    document_name VARCHAR(500),
    document_type VARCHAR(100),
    gcs_url TEXT,
    source_bucket VARCHAR(255),
    source_object TEXT,
    source_checksum VARCHAR(64),
    source_generation BIGINT,
    content_hash VARCHAR(64),
    file_size BIGINT,
    mime_type VARCHAR(255),
    file_type VARCHAR(100),
    title VARCHAR(1000),
    is_current BOOLEAN DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_documents_project_source_hash UNIQUE (project_id, source_bucket, source_object, content_hash)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_engine_processes (
    process_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    process_type VARCHAR(50) DEFAULT 'PROJECT_INGESTION',
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    total_files INTEGER DEFAULT 0,
    processed_files INTEGER DEFAULT 0,
    failed_files INTEGER DEFAULT 0,
    file_list JSONB,
    current_file TEXT,
    failure_cause TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.ingestion_documents (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    source_document_id UUID REFERENCES knowledge.documents(source_document_id) ON DELETE SET NULL,
    document_name VARCHAR(500),
    document_type VARCHAR(100),
    summary TEXT,
    extracted_metadata JSONB,
    extracted_at TIMESTAMPTZ,
    chunk_count INTEGER DEFAULT 0,
    extraction_status VARCHAR(50) DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_source_chunks (
    source_chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    document_id UUID REFERENCES knowledge.ingestion_documents(document_id) ON DELETE CASCADE,
    source_document_id UUID REFERENCES knowledge.documents(source_document_id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    chunk_index INTEGER,
    char_start BIGINT,
    char_end BIGINT,
    context_summary TEXT,
    embedding vector(768),
    embedding_status VARCHAR(50) DEFAULT 'PENDING',
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_domains (
    domain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_name VARCHAR(500) NOT NULL,
    knowledge TEXT,
    description TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_subdomains (
    subdomain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id UUID NOT NULL REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE CASCADE,
    subdomain_name VARCHAR(500) NOT NULL,
    knowledge TEXT,
    description TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_modules (
    module_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    module_name VARCHAR(500) NOT NULL,
    module_type VARCHAR(100),
    knowledge TEXT,
    responsibility TEXT,
    technology VARCHAR(255),
    owner VARCHAR(255),
    lifecycle VARCHAR(50),
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_components (
    component_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    component_name VARCHAR(500) NOT NULL,
    component_type VARCHAR(100),
    category VARCHAR(50),
    knowledge TEXT,
    responsibility TEXT,
    technology VARCHAR(255),
    capability VARCHAR(500),
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_business_rules (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    component_id UUID REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    rule_name VARCHAR(500) NOT NULL,
    rule_type VARCHAR(100),
    condition_text TEXT,
    outcome_text TEXT,
    exception_text TEXT,
    validation_criteria TEXT,
    priority VARCHAR(50),
    confidence DOUBLE PRECISION,
    embedding vector(768),
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_workflows (
    workflow_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    workflow_name VARCHAR(500) NOT NULL,
    trigger_text TEXT,
    outcome_text TEXT,
    actor VARCHAR(255),
    confidence DOUBLE PRECISION,
    embedding vector(768),
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_workflow_steps (
    step_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE CASCADE,
    sequence_number INTEGER,
    actor VARCHAR(255),
    action_text TEXT,
    input_parameters JSONB,
    output_parameters JSONB,
    business_rules JSONB,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_apis (
    api_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    component_id UUID REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    api_name VARCHAR(500) NOT NULL,
    api_type VARCHAR(100),
    endpoint_path TEXT,
    http_method VARCHAR(10),
    request_schema JSONB,
    response_schema JSONB,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_data_models (
    data_model_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    model_name VARCHAR(500) NOT NULL,
    model_type VARCHAR(100),
    schema_definition JSONB,
    business_definition TEXT,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_data_fields (
    field_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_model_id UUID NOT NULL REFERENCES knowledge.knowledge_data_models(data_model_id) ON DELETE CASCADE,
    field_name VARCHAR(255) NOT NULL,
    field_type VARCHAR(100),
    business_definition TEXT,
    business_rules JSONB
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_integrations (
    integration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    source_system VARCHAR(255),
    target_system VARCHAR(255),
    protocol VARCHAR(100),
    description TEXT,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_resources (
    resource_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    resource_name VARCHAR(500) NOT NULL,
    resource_type VARCHAR(100),
    provider VARCHAR(100),
    environment VARCHAR(50),
    configs JSONB,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_relationships (
    relationship_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    source_entity_type VARCHAR(100) NOT NULL,
    source_entity_id UUID,
    source_name VARCHAR(500),
    target_entity_type VARCHAR(100) NOT NULL,
    target_entity_id UUID,
    target_name VARCHAR(500),
    relationship_type VARCHAR(100) NOT NULL,
    relationship_definition TEXT,
    business_description TEXT,
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_chunks (
    knowledge_chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    chunk_type VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id UUID,
    content TEXT NOT NULL,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_facts (
    fact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    parent_fact_id UUID REFERENCES knowledge.knowledge_facts(fact_id) ON DELETE CASCADE,
    root_fact_id UUID REFERENCES knowledge.knowledge_facts(fact_id) ON DELETE CASCADE,
    fact_type VARCHAR(100) NOT NULL,
    fact_key VARCHAR(255),
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    content TEXT,
    priority VARCHAR(50),
    source_entity_type VARCHAR(100),
    source_entity_id UUID,
    source_rule_id UUID,
    attributes JSONB,
    embedding vector(768),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documents_project_id ON knowledge.documents(project_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_project_id ON knowledge.ingestion_documents(project_id);
CREATE INDEX IF NOT EXISTS idx_source_chunks_project_id ON knowledge.knowledge_source_chunks(project_id);
CREATE INDEX IF NOT EXISTS idx_domains_project_id ON knowledge.knowledge_domains(project_id);
CREATE INDEX IF NOT EXISTS idx_subdomains_project_id ON knowledge.knowledge_subdomains(project_id);
CREATE INDEX IF NOT EXISTS idx_modules_project_id ON knowledge.knowledge_modules(project_id);
CREATE INDEX IF NOT EXISTS idx_components_project_id ON knowledge.knowledge_components(project_id);
CREATE INDEX IF NOT EXISTS idx_relationships_project_id ON knowledge.knowledge_relationships(project_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_project_id ON knowledge.knowledge_chunks(project_id);
CREATE INDEX IF NOT EXISTS idx_facts_project_id ON knowledge.knowledge_facts(project_id);
CREATE INDEX IF NOT EXISTS idx_facts_parent_fact_id ON knowledge.knowledge_facts(parent_fact_id);
CREATE INDEX IF NOT EXISTS idx_facts_type_project_id ON knowledge.knowledge_facts(project_id, fact_type);

-- changeset kengine:embedding-hnsw-indexes-v1
CREATE INDEX IF NOT EXISTS idx_projects_definition_embedding_hnsw
    ON knowledge.projects
    USING hnsw (definition_embedding vector_cosine_ops)
    WHERE definition_embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_projects_summary_embedding_hnsw
    ON knowledge.projects
    USING hnsw (summary_embedding vector_cosine_ops)
    WHERE summary_embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_source_chunks_embedding_hnsw
    ON knowledge.knowledge_source_chunks
    USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_embedding_hnsw
    ON knowledge.knowledge_chunks
    USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

-- changeset kengine:project-schema-align-v2
ALTER TABLE knowledge.knowledge_domains
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_subdomains
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_modules
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_components
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_business_rules
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_workflows
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_apis
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_data_models
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_relationships
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;
