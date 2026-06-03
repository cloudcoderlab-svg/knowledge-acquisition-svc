-- =============================================================================
-- Knowledge Engine - Consolidated Database Schema v3.1
-- =============================================================================
--
-- Schema Version: 3.1 (DTO-Prompt-Entity Alignment)
-- Last Updated: 2026-06-03
-- Description: Complete database schema for Knowledge Engine project-centered
--              knowledge extraction and graph storage system
--
-- This schema supports:
-- - Multi-version project management
-- - Document ingestion and chunking
-- - Knowledge extraction (domains, components, workflows, business rules)
-- - Business concepts (capabilities, roles, terms, policies, decisions, metrics, notes)
-- - Architecture (APIs, data models, integrations, resources)
-- - Cross-entity relationships and knowledge facts
-- - Cross-document analysis (shared entities, conflicts, gaps)
-- - Vector similarity search with pgvector
-- =============================================================================

-- Enable pgvector extension for vector embeddings
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create knowledge schema
CREATE SCHEMA IF NOT EXISTS knowledge;

-- =============================================================================
-- CORE TABLES: Projects, Documents, Chunks, Processing
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: projects
-- Description: Root entity for all knowledge - supports multi-version projects
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.projects (
    project_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_name TEXT NOT NULL,
    version INTEGER NOT NULL,
    title TEXT,
    description TEXT,
    definition TEXT,
    definition_embedding vector(768),
    summary TEXT,
    summary_embedding vector(768),
    summary_generated_at TIMESTAMPTZ,
    source_bucket TEXT,
    gcs_prefix TEXT,
    status TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_projects_name_version UNIQUE (project_name, version)
);

CREATE INDEX IF NOT EXISTS idx_projects_name ON knowledge.projects (project_name);
CREATE INDEX IF NOT EXISTS idx_projects_status ON knowledge.projects (status);
CREATE INDEX IF NOT EXISTS idx_projects_definition_embedding ON knowledge.projects USING hnsw (definition_embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_projects_summary_embedding ON knowledge.projects USING hnsw (summary_embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.projects IS 'Root entity for knowledge extraction - supports multi-version project management';
COMMENT ON COLUMN knowledge.projects.definition IS 'High-level project definition provided at creation';
COMMENT ON COLUMN knowledge.projects.summary IS 'AI-generated project summary from extracted knowledge';

-- -----------------------------------------------------------------------------
-- Table: documents
-- Description: Source documents with version control and checksums
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.documents (
    source_document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    document_name TEXT,
    document_type TEXT,
    gcs_url TEXT,
    source_bucket TEXT,
    source_object TEXT,
    source_checksum VARCHAR(64),
    source_generation BIGINT,
    content_hash VARCHAR(64),
    file_size BIGINT,
    mime_type TEXT,
    file_type TEXT,
    title TEXT,
    is_current BOOLEAN,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT uq_documents_project_source_hash UNIQUE (project_id, source_bucket, source_object, content_hash)
);

CREATE INDEX IF NOT EXISTS idx_documents_project_id ON knowledge.documents (project_id);
CREATE INDEX IF NOT EXISTS idx_documents_type ON knowledge.documents (document_type);
CREATE INDEX IF NOT EXISTS idx_documents_current ON knowledge.documents (is_current) WHERE is_current = true;

COMMENT ON TABLE knowledge.documents IS 'Source documents with content-based versioning and GCS integration';
COMMENT ON COLUMN knowledge.documents.content_hash IS 'SHA-256 hash of document content for change detection';
COMMENT ON COLUMN knowledge.documents.is_current IS 'Indicates the most recent version of this document';

-- -----------------------------------------------------------------------------
-- Table: ingestion_documents
-- Description: Processing status and metadata for ingested documents
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.ingestion_documents (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    source_document_id UUID,
    document_name TEXT,
    document_type TEXT,
    summary TEXT,
    extracted_metadata JSONB,
    extracted_at TIMESTAMPTZ,
    chunk_count INTEGER,
    extraction_status TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ingestion_documents_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_ingestion_documents_source FOREIGN KEY (source_document_id) REFERENCES knowledge.documents(source_document_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ingestion_documents_project_id ON knowledge.ingestion_documents (project_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_source_id ON knowledge.ingestion_documents (source_document_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_status ON knowledge.ingestion_documents (extraction_status);

COMMENT ON TABLE knowledge.ingestion_documents IS 'Tracks document ingestion and chunking status';

-- -----------------------------------------------------------------------------
-- Table: knowledge_source_chunks
-- Description: Text chunks from documents with embeddings
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_source_chunks (
    source_chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    document_id UUID,
    source_document_id UUID,
    content TEXT NOT NULL,
    chunk_index INTEGER,
    char_start BIGINT,
    char_end BIGINT,
    context_summary TEXT,
    embedding vector(768),
    embedding_status TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_source_chunks_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_source_chunks_document FOREIGN KEY (document_id) REFERENCES knowledge.ingestion_documents(document_id) ON DELETE CASCADE,
    CONSTRAINT fk_source_chunks_source FOREIGN KEY (source_document_id) REFERENCES knowledge.documents(source_document_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_source_chunks_project_id ON knowledge.knowledge_source_chunks (project_id);
CREATE INDEX IF NOT EXISTS idx_source_chunks_document_id ON knowledge.knowledge_source_chunks (document_id);
CREATE INDEX IF NOT EXISTS idx_source_chunks_source_id ON knowledge.knowledge_source_chunks (source_document_id);
CREATE INDEX IF NOT EXISTS idx_source_chunks_embedding ON knowledge.knowledge_source_chunks USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_source_chunks IS 'Chunked document content with vector embeddings for semantic search';
COMMENT ON COLUMN knowledge.knowledge_source_chunks.embedding IS '768-dimensional vector from Vertex AI embeddings';

-- -----------------------------------------------------------------------------
-- Table: knowledge_engine_processes
-- Description: Tracks ingestion and extraction process execution
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_engine_processes (
    process_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    process_type TEXT,
    status TEXT NOT NULL,
    total_files INTEGER,
    processed_files INTEGER,
    failed_files INTEGER,
    file_list JSONB,
    current_file TEXT,
    failure_cause TEXT,
    total_tokens_processed BIGINT,
    total_bytes_processed BIGINT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_processes_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_processes_project_id ON knowledge.knowledge_engine_processes (project_id);
CREATE INDEX IF NOT EXISTS idx_processes_status ON knowledge.knowledge_engine_processes (status);
CREATE INDEX IF NOT EXISTS idx_processes_type ON knowledge.knowledge_engine_processes (process_type);

COMMENT ON TABLE knowledge.knowledge_engine_processes IS 'Execution tracking for ingestion and knowledge extraction processes';

-- =============================================================================
-- KNOWLEDGE DOMAIN TABLES: Domains, Subdomains, Components, Workflows
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: knowledge_domains
-- Description: Top-level business or technical domains
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_domains (
    domain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_name TEXT NOT NULL,
    knowledge TEXT,
    description TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_domains_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_domains_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_domains_project_id ON knowledge.knowledge_domains (project_id);
CREATE INDEX IF NOT EXISTS idx_domains_name ON knowledge.knowledge_domains (domain_name);
CREATE INDEX IF NOT EXISTS idx_domains_embedding ON knowledge.knowledge_domains USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_domains IS 'Top-level business or technical domains extracted from documentation';

-- -----------------------------------------------------------------------------
-- Table: knowledge_subdomains
-- Description: Subdomains within parent domains
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_subdomains (
    subdomain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_id UUID NOT NULL,
    subdomain_name TEXT NOT NULL,
    knowledge TEXT,
    description TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subdomains_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_subdomains_domain FOREIGN KEY (domain_id) REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE CASCADE,
    CONSTRAINT fk_subdomains_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_subdomains_project_id ON knowledge.knowledge_subdomains (project_id);
CREATE INDEX IF NOT EXISTS idx_subdomains_domain_id ON knowledge.knowledge_subdomains (domain_id);
CREATE INDEX IF NOT EXISTS idx_subdomains_name ON knowledge.knowledge_subdomains (subdomain_name);
CREATE INDEX IF NOT EXISTS idx_subdomains_embedding ON knowledge.knowledge_subdomains USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_subdomains IS 'Subdomains representing specialized areas within parent domains';

-- -----------------------------------------------------------------------------
-- Table: knowledge_components
-- Description: System components, services, and modules
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_components (
    component_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_id UUID,
    subdomain_id UUID,
    component_name TEXT NOT NULL,
    component_type TEXT,
    component_layer TEXT,
    category TEXT,
    knowledge TEXT,
    description TEXT,
    responsibility TEXT,
    technology TEXT,
    capability TEXT,
    owner TEXT,
    lifecycle TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_components_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_components_domain FOREIGN KEY (domain_id) REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    CONSTRAINT fk_components_subdomain FOREIGN KEY (subdomain_id) REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    CONSTRAINT fk_components_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_components_project_id ON knowledge.knowledge_components (project_id);
CREATE INDEX IF NOT EXISTS idx_components_domain_id ON knowledge.knowledge_components (domain_id);
CREATE INDEX IF NOT EXISTS idx_components_subdomain_id ON knowledge.knowledge_components (subdomain_id);
CREATE INDEX IF NOT EXISTS idx_components_name ON knowledge.knowledge_components (component_name);
CREATE INDEX IF NOT EXISTS idx_components_type ON knowledge.knowledge_components (component_type);
CREATE INDEX IF NOT EXISTS idx_components_layer ON knowledge.knowledge_components (component_layer);
CREATE INDEX IF NOT EXISTS idx_components_embedding ON knowledge.knowledge_components USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_components IS 'System components, services, modules with technical and business context';
COMMENT ON COLUMN knowledge.knowledge_components.component_layer IS 'Architecture layer: presentation, business, data, integration, etc.';

-- -----------------------------------------------------------------------------
-- Table: knowledge_workflows
-- Description: Business workflows and processes
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_workflows (
    workflow_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    workflow_name TEXT NOT NULL,
    trigger_text TEXT,
    outcome_text TEXT,
    actor TEXT,
    confidence DOUBLE PRECISION,
    embedding vector(768),
    source_chunk_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workflows_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_workflows_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_workflows_project_id ON knowledge.knowledge_workflows (project_id);
CREATE INDEX IF NOT EXISTS idx_workflows_name ON knowledge.knowledge_workflows (workflow_name);
CREATE INDEX IF NOT EXISTS idx_workflows_embedding ON knowledge.knowledge_workflows USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_workflows IS 'Business workflows and processes with trigger-action-outcome patterns';

-- -----------------------------------------------------------------------------
-- Table: knowledge_workflow_steps
-- Description: Individual steps within workflows
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_workflow_steps (
    step_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    workflow_id UUID NOT NULL,
    sequence_number INTEGER,
    actor TEXT,
    action_text TEXT,
    input_parameters JSONB,
    output_parameters JSONB,
    business_rules JSONB,
    embedding vector(768),
    CONSTRAINT fk_workflow_steps_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_workflow_steps_workflow FOREIGN KEY (workflow_id) REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_workflow_steps_project_id ON knowledge.knowledge_workflow_steps (project_id);
CREATE INDEX IF NOT EXISTS idx_workflow_steps_workflow_id ON knowledge.knowledge_workflow_steps (workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_steps_sequence ON knowledge.knowledge_workflow_steps (workflow_id, sequence_number);
CREATE INDEX IF NOT EXISTS idx_workflow_steps_embedding ON knowledge.knowledge_workflow_steps USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_workflow_steps IS 'Atomic steps within workflows with sequencing and business rules';

-- -----------------------------------------------------------------------------
-- Table: knowledge_business_rules
-- Description: Business rules and validation logic
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_business_rules (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    component_id UUID,
    rule_name TEXT NOT NULL,
    rule_type TEXT,
    condition_text TEXT,
    outcome_text TEXT,
    exception_text TEXT,
    validation_criteria TEXT,
    priority TEXT,
    confidence DOUBLE PRECISION,
    embedding vector(768),
    source_chunk_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_business_rules_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_business_rules_component FOREIGN KEY (component_id) REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    CONSTRAINT fk_business_rules_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_business_rules_project_id ON knowledge.knowledge_business_rules (project_id);
CREATE INDEX IF NOT EXISTS idx_business_rules_component_id ON knowledge.knowledge_business_rules (component_id);
CREATE INDEX IF NOT EXISTS idx_business_rules_type ON knowledge.knowledge_business_rules (rule_type);
CREATE INDEX IF NOT EXISTS idx_business_rules_embedding ON knowledge.knowledge_business_rules USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_business_rules IS 'Business rules with condition-outcome-exception patterns';

-- =============================================================================
-- BUSINESS CONCEPTS: Capabilities, Roles, Terms, Policies, Decisions, Metrics, Notes
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: knowledge_capabilities
-- Description: Business capabilities describing what the organization does
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_capabilities (
    capability_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_id UUID,
    subdomain_id UUID,
    capability_name TEXT NOT NULL,
    capability_type TEXT,
    description TEXT,
    business_value TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_capabilities_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_capabilities_domain FOREIGN KEY (domain_id) REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    CONSTRAINT fk_capabilities_subdomain FOREIGN KEY (subdomain_id) REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    CONSTRAINT fk_capabilities_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_capabilities_project_id ON knowledge.knowledge_capabilities (project_id);
CREATE INDEX IF NOT EXISTS idx_capabilities_domain_id ON knowledge.knowledge_capabilities (domain_id);
CREATE INDEX IF NOT EXISTS idx_capabilities_subdomain_id ON knowledge.knowledge_capabilities (subdomain_id);
CREATE INDEX IF NOT EXISTS idx_capabilities_name ON knowledge.knowledge_capabilities (capability_name);
CREATE INDEX IF NOT EXISTS idx_capabilities_embedding ON knowledge.knowledge_capabilities USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_capabilities IS 'Business capabilities representing what the organization does or can do';
COMMENT ON COLUMN knowledge.knowledge_capabilities.capability_type IS 'Classification: core, supporting, or enabling';

-- -----------------------------------------------------------------------------
-- Table: knowledge_roles
-- Description: Business roles and actors in processes
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_id UUID,
    subdomain_id UUID,
    role_name TEXT NOT NULL,
    role_type TEXT,
    description TEXT,
    responsibilities TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_roles_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_roles_domain FOREIGN KEY (domain_id) REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    CONSTRAINT fk_roles_subdomain FOREIGN KEY (subdomain_id) REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    CONSTRAINT fk_roles_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_roles_project_id ON knowledge.knowledge_roles (project_id);
CREATE INDEX IF NOT EXISTS idx_roles_domain_id ON knowledge.knowledge_roles (domain_id);
CREATE INDEX IF NOT EXISTS idx_roles_subdomain_id ON knowledge.knowledge_roles (subdomain_id);
CREATE INDEX IF NOT EXISTS idx_roles_name ON knowledge.knowledge_roles (role_name);
CREATE INDEX IF NOT EXISTS idx_roles_embedding ON knowledge.knowledge_roles USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_roles IS 'Business roles and actors that interact with processes and capabilities';
COMMENT ON COLUMN knowledge.knowledge_roles.role_type IS 'Classification: internal, external, system, or partner';

-- -----------------------------------------------------------------------------
-- Table: knowledge_terms
-- Description: Domain vocabulary and business terminology
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_terms (
    term_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_id UUID,
    subdomain_id UUID,
    term_name TEXT NOT NULL,
    category TEXT,
    business_definition TEXT,
    technical_definition TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_terms_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_terms_domain FOREIGN KEY (domain_id) REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    CONSTRAINT fk_terms_subdomain FOREIGN KEY (subdomain_id) REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    CONSTRAINT fk_terms_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_terms_project_id ON knowledge.knowledge_terms (project_id);
CREATE INDEX IF NOT EXISTS idx_terms_domain_id ON knowledge.knowledge_terms (domain_id);
CREATE INDEX IF NOT EXISTS idx_terms_subdomain_id ON knowledge.knowledge_terms (subdomain_id);
CREATE INDEX IF NOT EXISTS idx_terms_name ON knowledge.knowledge_terms (term_name);
CREATE INDEX IF NOT EXISTS idx_terms_embedding ON knowledge.knowledge_terms USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_terms IS 'Domain vocabulary forming the ubiquitous language of the business';
COMMENT ON COLUMN knowledge.knowledge_terms.category IS 'Classification: domain, process, metric, role, or entity';

-- -----------------------------------------------------------------------------
-- Table: knowledge_policies
-- Description: Business policies and compliance requirements
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_policies (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_id UUID,
    subdomain_id UUID,
    policy_name TEXT NOT NULL,
    policy_type TEXT,
    description TEXT,
    business_rationale TEXT,
    regulatory_requirement TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policies_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_policies_domain FOREIGN KEY (domain_id) REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    CONSTRAINT fk_policies_subdomain FOREIGN KEY (subdomain_id) REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    CONSTRAINT fk_policies_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_policies_project_id ON knowledge.knowledge_policies (project_id);
CREATE INDEX IF NOT EXISTS idx_policies_domain_id ON knowledge.knowledge_policies (domain_id);
CREATE INDEX IF NOT EXISTS idx_policies_subdomain_id ON knowledge.knowledge_policies (subdomain_id);
CREATE INDEX IF NOT EXISTS idx_policies_name ON knowledge.knowledge_policies (policy_name);
CREATE INDEX IF NOT EXISTS idx_policies_embedding ON knowledge.knowledge_policies USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_policies IS 'Business policies, compliance requirements, and governance rules';
COMMENT ON COLUMN knowledge.knowledge_policies.policy_type IS 'Classification: compliance, operational, security, quality, governance';

-- -----------------------------------------------------------------------------
-- Table: knowledge_decisions
-- Description: Business decision points in workflows
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_decisions (
    decision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_id UUID,
    subdomain_id UUID,
    workflow_id UUID,
    decision_name TEXT NOT NULL,
    decision_question TEXT,
    decision_criteria TEXT,
    decision_context TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_decisions_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_decisions_domain FOREIGN KEY (domain_id) REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    CONSTRAINT fk_decisions_subdomain FOREIGN KEY (subdomain_id) REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    CONSTRAINT fk_decisions_workflow FOREIGN KEY (workflow_id) REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE SET NULL,
    CONSTRAINT fk_decisions_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_decisions_project_id ON knowledge.knowledge_decisions (project_id);
CREATE INDEX IF NOT EXISTS idx_decisions_domain_id ON knowledge.knowledge_decisions (domain_id);
CREATE INDEX IF NOT EXISTS idx_decisions_subdomain_id ON knowledge.knowledge_decisions (subdomain_id);
CREATE INDEX IF NOT EXISTS idx_decisions_workflow_id ON knowledge.knowledge_decisions (workflow_id);
CREATE INDEX IF NOT EXISTS idx_decisions_name ON knowledge.knowledge_decisions (decision_name);
CREATE INDEX IF NOT EXISTS idx_decisions_embedding ON knowledge.knowledge_decisions USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_decisions IS 'Business decision points where choices are made based on criteria';

-- -----------------------------------------------------------------------------
-- Table: knowledge_metrics
-- Description: Business metrics and KPIs
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_metrics (
    metric_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_id UUID,
    subdomain_id UUID,
    capability_id UUID,
    workflow_id UUID,
    component_id UUID,
    metric_name TEXT NOT NULL,
    metric_type TEXT,
    description TEXT,
    calculation_method TEXT,
    target_value TEXT,
    unit TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_metrics_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_metrics_domain FOREIGN KEY (domain_id) REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    CONSTRAINT fk_metrics_subdomain FOREIGN KEY (subdomain_id) REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    CONSTRAINT fk_metrics_capability FOREIGN KEY (capability_id) REFERENCES knowledge.knowledge_capabilities(capability_id) ON DELETE SET NULL,
    CONSTRAINT fk_metrics_workflow FOREIGN KEY (workflow_id) REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE SET NULL,
    CONSTRAINT fk_metrics_component FOREIGN KEY (component_id) REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    CONSTRAINT fk_metrics_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_metrics_project_id ON knowledge.knowledge_metrics (project_id);
CREATE INDEX IF NOT EXISTS idx_metrics_domain_id ON knowledge.knowledge_metrics (domain_id);
CREATE INDEX IF NOT EXISTS idx_metrics_subdomain_id ON knowledge.knowledge_metrics (subdomain_id);
CREATE INDEX IF NOT EXISTS idx_metrics_capability_id ON knowledge.knowledge_metrics (capability_id);
CREATE INDEX IF NOT EXISTS idx_metrics_workflow_id ON knowledge.knowledge_metrics (workflow_id);
CREATE INDEX IF NOT EXISTS idx_metrics_component_id ON knowledge.knowledge_metrics (component_id);
CREATE INDEX IF NOT EXISTS idx_metrics_name ON knowledge.knowledge_metrics (metric_name);
CREATE INDEX IF NOT EXISTS idx_metrics_embedding ON knowledge.knowledge_metrics USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_metrics IS 'Business metrics and KPIs measuring capabilities, workflows, and components';
COMMENT ON COLUMN knowledge.knowledge_metrics.metric_type IS 'Classification: performance, quality, financial, operational, customer';

-- -----------------------------------------------------------------------------
-- Table: knowledge_notes
-- Description: Miscellaneous knowledge notes and observations
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_notes (
    note_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_id UUID,
    subdomain_id UUID,
    note_type TEXT,
    topic TEXT,
    note_text TEXT NOT NULL,
    related_entity TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notes_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_notes_domain FOREIGN KEY (domain_id) REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    CONSTRAINT fk_notes_subdomain FOREIGN KEY (subdomain_id) REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    CONSTRAINT fk_notes_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_notes_project_id ON knowledge.knowledge_notes (project_id);
CREATE INDEX IF NOT EXISTS idx_notes_domain_id ON knowledge.knowledge_notes (domain_id);
CREATE INDEX IF NOT EXISTS idx_notes_subdomain_id ON knowledge.knowledge_notes (subdomain_id);
CREATE INDEX IF NOT EXISTS idx_notes_type ON knowledge.knowledge_notes (note_type);
CREATE INDEX IF NOT EXISTS idx_notes_embedding ON knowledge.knowledge_notes USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_notes IS 'Important observations, insights, constraints, and contextual information';
COMMENT ON COLUMN knowledge.knowledge_notes.note_type IS 'Values: architecture, design_decision, constraint, assumption, risk, recommendation, migration, technical_debt, code_smell';

-- =============================================================================
-- ARCHITECTURE TABLES: APIs, Data Models, Integrations, Resources
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: knowledge_apis
-- Description: API endpoints and specifications
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_apis (
    api_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    component_id UUID,
    source_document_id UUID,
    api_name TEXT NOT NULL,
    api_type TEXT,
    endpoint_path TEXT,
    http_method TEXT,
    description TEXT,
    request_schema JSONB,
    response_schema JSONB,
    authentication TEXT,
    source_component_name TEXT,
    business_capability TEXT,
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_apis_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_apis_component FOREIGN KEY (component_id) REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    CONSTRAINT fk_apis_source_document FOREIGN KEY (source_document_id) REFERENCES knowledge.documents(source_document_id) ON DELETE SET NULL,
    CONSTRAINT fk_apis_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_apis_project_id ON knowledge.knowledge_apis (project_id);
CREATE INDEX IF NOT EXISTS idx_apis_component_id ON knowledge.knowledge_apis (component_id);
CREATE INDEX IF NOT EXISTS idx_apis_name ON knowledge.knowledge_apis (api_name);
CREATE INDEX IF NOT EXISTS idx_apis_type ON knowledge.knowledge_apis (api_type);
CREATE INDEX IF NOT EXISTS idx_apis_embedding ON knowledge.knowledge_apis USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_apis IS 'API endpoints with request/response schemas and authentication';

-- -----------------------------------------------------------------------------
-- Table: knowledge_data_models
-- Description: Data models and entity definitions
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_data_models (
    data_model_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    model_name TEXT NOT NULL,
    model_type TEXT,
    schema_definition JSONB,
    business_definition TEXT,
    embedding vector(768),
    CONSTRAINT fk_data_models_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_data_models_project_id ON knowledge.knowledge_data_models (project_id);
CREATE INDEX IF NOT EXISTS idx_data_models_name ON knowledge.knowledge_data_models (model_name);
CREATE INDEX IF NOT EXISTS idx_data_models_type ON knowledge.knowledge_data_models (model_type);
CREATE INDEX IF NOT EXISTS idx_data_models_embedding ON knowledge.knowledge_data_models USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_data_models IS 'Data models representing entities, tables, or data structures';

-- -----------------------------------------------------------------------------
-- Table: knowledge_data_fields
-- Description: Fields within data models
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_data_fields (
    field_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    data_model_id UUID NOT NULL,
    field_name TEXT NOT NULL,
    field_type TEXT,
    is_required BOOLEAN,
    description TEXT,
    business_definition TEXT,
    business_meaning TEXT,
    business_rules JSONB,
    CONSTRAINT fk_data_fields_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_data_fields_model FOREIGN KEY (data_model_id) REFERENCES knowledge.knowledge_data_models(data_model_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_data_fields_project_id ON knowledge.knowledge_data_fields (project_id);
CREATE INDEX IF NOT EXISTS idx_data_fields_model_id ON knowledge.knowledge_data_fields (data_model_id);
CREATE INDEX IF NOT EXISTS idx_data_fields_name ON knowledge.knowledge_data_fields (field_name);

COMMENT ON TABLE knowledge.knowledge_data_fields IS 'Individual fields within data models with type and validation rules';

-- -----------------------------------------------------------------------------
-- Table: knowledge_integrations
-- Description: System integrations and data flows
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_integrations (
    integration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    component_id UUID,
    source_document_id UUID,
    integration_name TEXT,
    integration_type TEXT,
    source_system TEXT,
    target_system TEXT,
    protocol TEXT,
    data_exchanged TEXT,
    description TEXT,
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_integrations_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_integrations_component FOREIGN KEY (component_id) REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    CONSTRAINT fk_integrations_source_document FOREIGN KEY (source_document_id) REFERENCES knowledge.documents(source_document_id) ON DELETE SET NULL,
    CONSTRAINT fk_integrations_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_integrations_project_id ON knowledge.knowledge_integrations (project_id);
CREATE INDEX IF NOT EXISTS idx_integrations_component_id ON knowledge.knowledge_integrations (component_id);
CREATE INDEX IF NOT EXISTS idx_integrations_type ON knowledge.knowledge_integrations (integration_type);
CREATE INDEX IF NOT EXISTS idx_integrations_embedding ON knowledge.knowledge_integrations USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_integrations IS 'System integrations describing data flows between components';

-- -----------------------------------------------------------------------------
-- Table: knowledge_resources
-- Description: Infrastructure and deployment resources
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_resources (
    resource_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    resource_name TEXT NOT NULL,
    resource_type TEXT,
    provider TEXT,
    environment TEXT,
    configs JSONB,
    embedding vector(768),
    CONSTRAINT fk_resources_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_resources_project_id ON knowledge.knowledge_resources (project_id);
CREATE INDEX IF NOT EXISTS idx_resources_name ON knowledge.knowledge_resources (resource_name);
CREATE INDEX IF NOT EXISTS idx_resources_type ON knowledge.knowledge_resources (resource_type);
CREATE INDEX IF NOT EXISTS idx_resources_embedding ON knowledge.knowledge_resources USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_resources IS 'Infrastructure resources like databases, queues, storage, compute';

-- =============================================================================
-- RELATIONSHIPS AND KNOWLEDGE CHUNKS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: knowledge_relationships
-- Description: Cross-entity relationships in the knowledge graph
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_relationships (
    relationship_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    source_entity_type TEXT NOT NULL,
    source_entity_id UUID,
    source_name TEXT,
    target_entity_type TEXT NOT NULL,
    target_entity_id UUID,
    target_name TEXT,
    relationship_type TEXT NOT NULL,
    relationship_definition TEXT,
    business_description TEXT,
    confidence DOUBLE PRECISION,
    source_chunk_id UUID,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_relationships_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_relationships_source_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_relationships_project_id ON knowledge.knowledge_relationships (project_id);
CREATE INDEX IF NOT EXISTS idx_relationships_source_entity ON knowledge.knowledge_relationships (source_entity_type, source_entity_id);
CREATE INDEX IF NOT EXISTS idx_relationships_target_entity ON knowledge.knowledge_relationships (target_entity_type, target_entity_id);
CREATE INDEX IF NOT EXISTS idx_relationships_type ON knowledge.knowledge_relationships (relationship_type);
CREATE INDEX IF NOT EXISTS idx_relationships_embedding ON knowledge.knowledge_relationships USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_relationships IS 'Cross-entity relationships forming the knowledge graph';
COMMENT ON COLUMN knowledge.knowledge_relationships.relationship_type IS 'Examples: depends_on, implements, uses, contains, triggers, produces';

-- -----------------------------------------------------------------------------
-- Table: knowledge_chunks
-- Description: Reusable knowledge chunks
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_chunks (
    knowledge_chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    chunk_type TEXT,
    entity_type TEXT,
    entity_id UUID,
    content TEXT NOT NULL,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_knowledge_chunks_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_project_id ON knowledge.knowledge_chunks (project_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_entity ON knowledge.knowledge_chunks (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_embedding ON knowledge.knowledge_chunks USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_chunks IS 'Reusable knowledge chunks linked to entities for retrieval';

-- -----------------------------------------------------------------------------
-- Table: knowledge_facts
-- Description: Atomic knowledge facts with hierarchical structure
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_facts (
    fact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    parent_fact_id UUID,
    root_fact_id UUID,
    fact_type TEXT NOT NULL,
    fact_key TEXT,
    title TEXT NOT NULL,
    summary TEXT,
    content TEXT,
    priority TEXT,
    source_entity_type TEXT,
    source_entity_id UUID,
    source_rule_id UUID,
    attributes JSONB,
    embedding vector(768),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_facts_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_facts_parent FOREIGN KEY (parent_fact_id) REFERENCES knowledge.knowledge_facts(fact_id) ON DELETE CASCADE,
    CONSTRAINT fk_facts_root FOREIGN KEY (root_fact_id) REFERENCES knowledge.knowledge_facts(fact_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_facts_project_id ON knowledge.knowledge_facts (project_id);
CREATE INDEX IF NOT EXISTS idx_facts_parent_id ON knowledge.knowledge_facts (parent_fact_id);
CREATE INDEX IF NOT EXISTS idx_facts_root_id ON knowledge.knowledge_facts (root_fact_id);
CREATE INDEX IF NOT EXISTS idx_facts_type ON knowledge.knowledge_facts (fact_type);
CREATE INDEX IF NOT EXISTS idx_facts_source_entity ON knowledge.knowledge_facts (source_entity_type, source_entity_id);
CREATE INDEX IF NOT EXISTS idx_facts_embedding ON knowledge.knowledge_facts USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE knowledge.knowledge_facts IS 'Atomic knowledge facts with parent-child hierarchy for composition';

-- =============================================================================
-- ANALYSIS TABLES: Shared Entities, Conflicts, Gaps
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: knowledge_shared_entities
-- Description: Entities appearing in multiple documents
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_shared_entities (
    shared_entity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    entity_name TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    appears_in_documents JSONB,
    consistency_score DOUBLE PRECISION,
    notes TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shared_entities_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_shared_entities_project_id ON knowledge.knowledge_shared_entities (project_id);
CREATE INDEX IF NOT EXISTS idx_shared_entities_name ON knowledge.knowledge_shared_entities (entity_name);
CREATE INDEX IF NOT EXISTS idx_shared_entities_type ON knowledge.knowledge_shared_entities (entity_type);
CREATE INDEX IF NOT EXISTS idx_shared_entities_score ON knowledge.knowledge_shared_entities (consistency_score);

COMMENT ON TABLE knowledge.knowledge_shared_entities IS 'Entities referenced across multiple documents with consistency tracking';
COMMENT ON COLUMN knowledge.knowledge_shared_entities.consistency_score IS 'Measures how uniformly entity is described (0.0 to 1.0)';

-- -----------------------------------------------------------------------------
-- Table: knowledge_document_conflicts
-- Description: Conflicts detected across documents
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_document_conflicts (
    conflict_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    entity_name TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    conflict_type TEXT NOT NULL,
    description TEXT NOT NULL,
    documents_involved JSONB NOT NULL,
    severity TEXT,
    resolution_status TEXT DEFAULT 'UNRESOLVED',
    resolution_notes TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conflicts_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_conflicts_project_id ON knowledge.knowledge_document_conflicts (project_id);
CREATE INDEX IF NOT EXISTS idx_conflicts_entity_name ON knowledge.knowledge_document_conflicts (entity_name);
CREATE INDEX IF NOT EXISTS idx_conflicts_type ON knowledge.knowledge_document_conflicts (conflict_type);
CREATE INDEX IF NOT EXISTS idx_conflicts_severity ON knowledge.knowledge_document_conflicts (severity);
CREATE INDEX IF NOT EXISTS idx_conflicts_status ON knowledge.knowledge_document_conflicts (resolution_status);

COMMENT ON TABLE knowledge.knowledge_document_conflicts IS 'Conflicts and inconsistencies detected across multiple documents';
COMMENT ON COLUMN knowledge.knowledge_document_conflicts.conflict_type IS 'Values: contradictory_definitions, version_mismatch, duplicate_responsibility, incompatible_dependencies';

-- -----------------------------------------------------------------------------
-- Table: knowledge_document_gaps
-- Description: Missing or incomplete information detected
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge.knowledge_document_gaps (
    gap_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    entity_name TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    gap_type TEXT NOT NULL,
    description TEXT NOT NULL,
    referenced_in JSONB,
    expected_in TEXT,
    resolution_status TEXT DEFAULT 'OPEN',
    resolution_notes TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gaps_project FOREIGN KEY (project_id) REFERENCES knowledge.projects(project_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_gaps_project_id ON knowledge.knowledge_document_gaps (project_id);
CREATE INDEX IF NOT EXISTS idx_gaps_entity_name ON knowledge.knowledge_document_gaps (entity_name);
CREATE INDEX IF NOT EXISTS idx_gaps_type ON knowledge.knowledge_document_gaps (gap_type);
CREATE INDEX IF NOT EXISTS idx_gaps_status ON knowledge.knowledge_document_gaps (resolution_status);

COMMENT ON TABLE knowledge.knowledge_document_gaps IS 'Gaps representing missing or incomplete information';
COMMENT ON COLUMN knowledge.knowledge_document_gaps.gap_type IS 'Values: referenced_not_defined, missing_implementation, undefined_dependency, incomplete_specification';

-- =============================================================================
-- END OF SCHEMA
-- =============================================================================
