-- ============================================================================
-- Knowledge Engine - Consolidated Database Schema
-- ============================================================================
-- Purpose: Single source of truth for all knowledge extraction tables
-- Version: 3.0 (Consolidated from project-centered-schema + fixes)
-- Last Updated: 2026-06-02
--
-- Key Changes in v3.0:
-- - All VARCHAR columns converted to TEXT for unlimited string storage
-- - No post-creation ALTER TABLE migrations required
-- - Optimal for fresh installations
--
-- Schema Overview:
-- 1. Core Tables: projects, documents, ingestion_documents
-- 2. Knowledge Tables: domains, subdomains, modules, components
-- 3. Business Logic: workflows, workflow_steps, business_rules
-- 4. Architecture: apis, data_models, data_fields, integrations
-- 5. Relationships: knowledge_relationships (links all entities)
-- 6. Business Concepts: capabilities, roles, terms, policies, decisions, metrics
-- 7. Cross-Document Analysis: shared_entities, document_conflicts, document_gaps
-- 8. Vector Indexes: HNSW indexes for semantic search on embeddings
-- ============================================================================

-- liquibase formatted sql

-- changeset kengine:project-centered-schema-v1
-- ============================================================================
-- Step 1: Create schema and enable required PostgreSQL extensions
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS knowledge;
CREATE EXTENSION IF NOT EXISTS vector;  -- pgvector for embeddings
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- UUID generation

-- ============================================================================
-- Step 2: Core Project and Document Management Tables
-- ============================================================================
-- These tables manage project metadata, source documents, and processing state

-- Projects table: Top-level container for all knowledge extraction projects
-- Each project represents a knowledge base (e.g., "legacy-system-analysis")
CREATE TABLE IF NOT EXISTS knowledge.projects (
    project_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_name TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    title TEXT,
    description TEXT,
    definition TEXT,
    definition_embedding vector(768),
    summary TEXT,
    summary_embedding vector(768),
    summary_generated_at TIMESTAMPTZ,
    source_bucket TEXT,
    gcs_prefix TEXT,
    status TEXT DEFAULT 'DRAFT',
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_projects_name_version UNIQUE (project_name, version)
);

-- Documents table: Tracks source documents uploaded to GCS for processing
-- Stores metadata and checksums for deduplication
CREATE TABLE IF NOT EXISTS knowledge.documents (
    source_document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    document_name TEXT,
    document_type TEXT,
    gcs_url TEXT,
    source_bucket TEXT,
    source_object TEXT,
    source_checksum TEXT,
    source_generation BIGINT,
    content_hash TEXT,
    file_size BIGINT,
    mime_type TEXT,
    file_type TEXT,
    title TEXT,
    is_current BOOLEAN DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_documents_project_source_hash UNIQUE (project_id, source_bucket, source_object, content_hash)
);

-- Process tracking table: Monitors long-running extraction pipelines
-- Tracks progress, failures, and completion status for UI/API monitoring
CREATE TABLE IF NOT EXISTS knowledge.knowledge_engine_processes (
    process_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    process_type TEXT DEFAULT 'PROJECT_INGESTION',
    status TEXT NOT NULL DEFAULT 'QUEUED',
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
    document_name TEXT,
    document_type TEXT,
    summary TEXT,
    extracted_metadata JSONB,
    extracted_at TIMESTAMPTZ,
    chunk_count INTEGER DEFAULT 0,
    extraction_status TEXT DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Step 3: Knowledge Source and Chunking Tables
-- ============================================================================
-- Manages document chunking and embedding generation for semantic search

-- Source chunks table: Stores text chunks from documents with embeddings
-- Each chunk represents a semantic unit (e.g., paragraph, section)
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
    embedding_status TEXT DEFAULT 'PENDING',
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Step 4: Extracted Knowledge - Domain Model Tables
-- ============================================================================
-- Stores structured knowledge extracted from documents via AI
-- Hierarchical structure: domains → subdomains → modules → components

-- Domains table: Top-level business domains (e.g., "Customer Management")
CREATE TABLE IF NOT EXISTS knowledge.knowledge_domains (
    domain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_name TEXT NOT NULL,
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
    subdomain_name TEXT NOT NULL,
    knowledge TEXT,
    description TEXT,
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
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    component_name TEXT NOT NULL,
    component_type TEXT,
    category TEXT,
    knowledge TEXT,
    responsibility TEXT,
    technology TEXT,
    capability TEXT,
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
    component_id UUID REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    rule_name TEXT NOT NULL,
    rule_type TEXT,
    condition_text TEXT,
    outcome_text TEXT,
    exception_text TEXT,
    validation_criteria TEXT,
    priority TEXT,
    confidence DOUBLE PRECISION,
    embedding vector(768),
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_workflows (
    workflow_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    workflow_name TEXT NOT NULL,
    trigger_text TEXT,
    outcome_text TEXT,
    actor TEXT,
    confidence DOUBLE PRECISION,
    embedding vector(768),
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_workflow_steps (
    step_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE CASCADE,
    sequence_number INTEGER,
    actor TEXT,
    action_text TEXT,
    input_parameters JSONB,
    output_parameters JSONB,
    business_rules JSONB,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_apis (
    api_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    component_id UUID REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    api_name TEXT NOT NULL,
    api_type TEXT,
    endpoint_path TEXT,
    http_method TEXT,
    request_schema JSONB,
    response_schema JSONB,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_data_models (
    data_model_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    model_name TEXT NOT NULL,
    model_type TEXT,
    schema_definition JSONB,
    business_definition TEXT,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_data_fields (
    field_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_model_id UUID NOT NULL REFERENCES knowledge.knowledge_data_models(data_model_id) ON DELETE CASCADE,
    field_name TEXT NOT NULL,
    field_type TEXT,
    business_definition TEXT,
    business_rules JSONB
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_integrations (
    integration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    source_system TEXT,
    target_system TEXT,
    protocol TEXT,
    description TEXT,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_resources (
    resource_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    resource_name TEXT NOT NULL,
    resource_type TEXT,
    provider TEXT,
    environment TEXT,
    configs JSONB,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_relationships (
    relationship_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
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
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge.knowledge_chunks (
    knowledge_chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    chunk_type TEXT,
    entity_type TEXT,
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
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documents_project_id ON knowledge.documents(project_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_project_id ON knowledge.ingestion_documents(project_id);
CREATE INDEX IF NOT EXISTS idx_source_chunks_project_id ON knowledge.knowledge_source_chunks(project_id);
CREATE INDEX IF NOT EXISTS idx_domains_project_id ON knowledge.knowledge_domains(project_id);
CREATE INDEX IF NOT EXISTS idx_subdomains_project_id ON knowledge.knowledge_subdomains(project_id);
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

ALTER TABLE knowledge.knowledge_components
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_business_rules
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_workflows
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_relationships
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

-- changeset kengine:business-concept-tables-v1
-- Business concept tables for document-level entity extraction and linking
-- These tables store high-level business concepts identified during document-level analysis
-- They serve as reference entities for chunk-level extraction to ensure consistent naming

-- Business Capabilities: What the organization does or can do to achieve its goals
-- Examples: Customer Management, Order Processing, Payment Handling, Inventory Management
CREATE TABLE IF NOT EXISTS knowledge.knowledge_capabilities (
    capability_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    capability_name TEXT NOT NULL,
    capability_type TEXT,
    description TEXT,
    business_value TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Business Roles: Actors (human or system) that interact with business processes and workflows
-- Examples: Customer, Admin, Approver, Sales Representative, System User, Account Manager
CREATE TABLE IF NOT EXISTS knowledge.knowledge_roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    role_name TEXT NOT NULL,
    role_type TEXT,
    description TEXT,
    responsibilities TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Business Terms: Domain vocabulary and ubiquitous language used within the business context
-- Examples: Order, Customer, SKU, Contract, Policy, Premium Customer, Discount Tier
CREATE TABLE IF NOT EXISTS knowledge.knowledge_terms (
    term_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    term_name TEXT NOT NULL,
    category TEXT,
    business_definition TEXT,
    technical_definition TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Business Policies: Rules, constraints, and compliance requirements that govern operations
-- Examples: Data Retention Policy, GDPR Compliance, Approval Policy, Security Policy
CREATE TABLE IF NOT EXISTS knowledge.knowledge_policies (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    policy_name TEXT NOT NULL,
    policy_type TEXT,
    description TEXT,
    business_rationale TEXT,
    regulatory_requirement TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Business Decisions: Critical decision points within workflows where choices are made
-- Examples: Credit Approval Decision, Discount Eligibility Check, Route Selection
CREATE TABLE IF NOT EXISTS knowledge.knowledge_decisions (
    decision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    workflow_id UUID REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE SET NULL,
    decision_name TEXT NOT NULL,
    decision_question TEXT,
    decision_criteria TEXT,
    decision_context TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Business Metrics: KPIs and performance indicators used to measure capabilities and workflows
-- Examples: Order Processing Time, Customer Satisfaction Score, System Uptime, Conversion Rate
CREATE TABLE IF NOT EXISTS knowledge.knowledge_metrics (
    metric_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    capability_id UUID REFERENCES knowledge.knowledge_capabilities(capability_id) ON DELETE SET NULL,
    workflow_id UUID REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE SET NULL,
    component_id UUID REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    metric_name TEXT NOT NULL,
    metric_type TEXT, -- performance, quality, financial, operational, customer
    description TEXT,
    calculation_method TEXT,
    target_value TEXT,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge Notes: Miscellaneous insights that don't fit structured categories
-- Examples: Architecture decisions, constraints, assumptions, risks, recommendations
CREATE TABLE IF NOT EXISTS knowledge.knowledge_notes (
    note_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id UUID REFERENCES knowledge.knowledge_domains(domain_id) ON DELETE SET NULL,
    subdomain_id UUID REFERENCES knowledge.knowledge_subdomains(subdomain_id) ON DELETE SET NULL,
    note_type TEXT, -- architecture, design_decision, constraint, assumption, risk, recommendation
    note_text TEXT NOT NULL,
    embedding vector(768),
    confidence DOUBLE PRECISION,
    source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient project-scoped queries
CREATE INDEX IF NOT EXISTS idx_capabilities_project_id ON knowledge.knowledge_capabilities(project_id);
CREATE INDEX IF NOT EXISTS idx_roles_project_id ON knowledge.knowledge_roles(project_id);
CREATE INDEX IF NOT EXISTS idx_terms_project_id ON knowledge.knowledge_terms(project_id);
CREATE INDEX IF NOT EXISTS idx_policies_project_id ON knowledge.knowledge_policies(project_id);
CREATE INDEX IF NOT EXISTS idx_decisions_project_id ON knowledge.knowledge_decisions(project_id);
CREATE INDEX IF NOT EXISTS idx_metrics_project_id ON knowledge.knowledge_metrics(project_id);
CREATE INDEX IF NOT EXISTS idx_notes_project_id ON knowledge.knowledge_notes(project_id);

-- changeset kengine:cross-document-analysis-tables-v1
-- Cross-document analysis tables for storing insights from multi-document relationship analysis
-- These tables capture shared entities, conflicts, and gaps identified when analyzing multiple documents together
-- They enable data quality verification, entity deduplication, and documentation completeness assessment

-- Shared Entities: Entities appearing in multiple documents across the knowledge base
-- Used for entity consolidation, consistency verification, and traceability
CREATE TABLE IF NOT EXISTS knowledge.knowledge_shared_entities (
    shared_entity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    entity_name TEXT NOT NULL,
    entity_type TEXT NOT NULL, -- solution_component, api, business_flow, business_role, etc.
    appears_in_documents JSONB, -- Array of document filenames where this entity appears
    consistency_score DOUBLE PRECISION, -- 1.0 = perfect consistency, lower = discrepancies
    notes TEXT, -- Description of alignment or discrepancies across documents
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Document Conflicts: Contradictions or inconsistencies detected across multiple documents
-- Used for data quality verification, migration planning, and architectural governance
CREATE TABLE IF NOT EXISTS knowledge.knowledge_document_conflicts (
    conflict_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    entity_name TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    conflict_type TEXT NOT NULL, -- contradictory_definitions, version_mismatch, duplicate_responsibility, incompatible_dependencies
    description TEXT NOT NULL, -- Detailed explanation of the conflict
    documents_involved JSONB NOT NULL, -- Array of document filenames involved in the conflict
    severity TEXT, -- critical, high, medium, low
    resolution_status TEXT DEFAULT 'UNRESOLVED', -- UNRESOLVED, INVESTIGATING, RESOLVED
    resolution_notes TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Document Gaps: Missing or incomplete information detected during cross-document analysis
-- Used for completeness verification, missing implementation detection, and documentation improvement
CREATE TABLE IF NOT EXISTS knowledge.knowledge_document_gaps (
    gap_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    entity_name TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    gap_type TEXT NOT NULL, -- referenced_not_defined, missing_implementation, undefined_dependency, incomplete_specification
    description TEXT NOT NULL, -- Detailed explanation of what is missing or incomplete
    referenced_in JSONB, -- Array of document filenames that reference this missing entity
    expected_in TEXT, -- Where this entity should be defined (guidance)
    resolution_status TEXT DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, CLOSED
    resolution_notes TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient project-scoped queries
CREATE INDEX IF NOT EXISTS idx_shared_entities_project_id ON knowledge.knowledge_shared_entities(project_id);
CREATE INDEX IF NOT EXISTS idx_shared_entities_entity_name ON knowledge.knowledge_shared_entities(entity_name);
CREATE INDEX IF NOT EXISTS idx_shared_entities_consistency ON knowledge.knowledge_shared_entities(project_id, consistency_score);

CREATE INDEX IF NOT EXISTS idx_document_conflicts_project_id ON knowledge.knowledge_document_conflicts(project_id);
CREATE INDEX IF NOT EXISTS idx_document_conflicts_severity ON knowledge.knowledge_document_conflicts(project_id, severity);
CREATE INDEX IF NOT EXISTS idx_document_conflicts_status ON knowledge.knowledge_document_conflicts(project_id, resolution_status);

CREATE INDEX IF NOT EXISTS idx_document_gaps_project_id ON knowledge.knowledge_document_gaps(project_id);
CREATE INDEX IF NOT EXISTS idx_document_gaps_type ON knowledge.knowledge_document_gaps(project_id, gap_type);
CREATE INDEX IF NOT EXISTS idx_document_gaps_status ON knowledge.knowledge_document_gaps(project_id, resolution_status);

-- ============================================================================
-- SCHEMA EVOLUTION NOTES
-- ============================================================================

-- changeset kengine:schema-v2-documentation
-- Documentation of schema v2 changes aligned with optimized knowledge extraction prompt
-- Date: 2025-01-XX
-- Summary: Removed deprecated entity types prone to hallucination and clarified capability/component distinction

-- REMOVED ENTITY TYPES (Schema v2):
-- These entity types were REMOVED from the extraction schema and never had corresponding database tables.
-- They were removed from KnowledgeExtractionResult DTO and knowledge-extraction-prompt.txt to:
--   1. Reduce hallucination risk (AI frequently fabricated these without source evidence)
--   2. Optimize token consumption (rarely present in actual documentation)
--   3. Improve extraction quality (focus on entities with high source fidelity)
--
-- Removed entities (never persisted to database):
--   - businessComponents: Replaced by clear separation of businessCapabilities (WHAT) and solutionComponents (HOW)
--   - costEstimates: Removed - rarely in documentation, high hallucination risk
--   - usageProfiles: Removed - rarely in documentation, high hallucination risk
--   - migrationNotes: Removed - consolidated into knowledge_notes table with noteType filtering
--
-- NO DATABASE MIGRATION REQUIRED - these types never had tables

-- CLARIFIED ENTITY DISTINCTION (Schema v2):
-- Business Capabilities (knowledge_capabilities table):
--   - WHAT the business does - technology-independent business functions
--   - Examples: "Order Management", "Customer Onboarding", "Risk Assessment"
--   - Extracted from businessCapabilities field in prompt
--   - Stable over time even as technology changes
--   - Core/supporting/enabling classification
--
-- Solution Components (knowledge_components table):
--   - HOW capabilities are implemented - technical implementation details
--   - Examples: "OrderService", "CustomerAPI", "PaymentGateway"
--   - Extracted from solutionComponents field (aliased to technicalComponents in DTO)
--   - Technology-specific, evolves with architecture changes
--   - Category field distinguishes "technical" vs legacy "business" components
--
-- This separation enables:
--   - Clear business architecture modeling independent of technology
--   - Capability mapping for strategic planning and modernization
--   - Technology impact analysis when platforms change
--   - Business continuity planning across technology evolution

-- SCHEMA V2 EXTRACTION IMPROVEMENTS:
-- The updated knowledge-extraction-prompt.txt includes:
--   - Anti-hallucination rules (9 critical rules to prevent fabrication)
--   - Confidence scoring rubric (4 levels: very high, high, medium, omit)
--   - Entity matching criteria (exact match vs create new)
--   - Output quality rules (minimum extraction criteria, field inclusion)
--   - Edge case handling (abbreviations, partial info, conflicts, ambiguity)
--   - Relationship type expansion (13 -> 19 types for better granularity)
--   - Token optimization (27% reduction via schema simplification)
--
-- Result: Higher quality extractions with fewer hallucinations and better entity linking
