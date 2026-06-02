# Knowledge Engine Database Changelog

This directory contains Liquibase database migration scripts for the Knowledge Engine.

## Files Overview

### Incremental Migration (Existing Databases)

**Use `changelog-master.xml`** for existing databases that need incremental migrations:

```xml
<!-- changelog-master.xml includes: -->
1. project-centered-schema.sql          - Base schema (30 tables)
2. add-processing-summary-fields        - Token/byte tracking
3. increase-varchar-lengths-*           - Expand column sizes
4. add-project-id-to-workflow-steps-*   - Add missing foreign keys
5. add-project-summary-profile          - Project summary fields
6. fix-text-columns-and-constraints-v1  - Convert VARCHAR to TEXT
```

**When to use:**
- Updating an existing database
- Production environments with existing data
- Need rollback capability
- Want to preserve migration history

### Consolidated Schema (New Databases)

**Use `sql/consolidated-schema-v3.sql`** for greenfield installations:

This single file represents the **final state** of all migrations combined:
- All 30 tables with final column definitions
- All VARCHAR columns already converted to TEXT
- All additional columns (tokens_processed, summary_embedding, etc.)
- All indexes including HNSW vector indexes
- Complete schema ready for use

**When to use:**
- New database deployments
- Development/testing environments
- Clean slate installations
- Simplified schema review

## Schema Version History

### Version 3 (Current - Consolidated)
- **File:** `consolidated-schema-v3.sql`
- **Features:** Single file with all migrations applied
- **Tables:** 30 tables (14 entity types + cross-document analysis)
- **Columns:** All TEXT (no VARCHAR size limits)
- **Optimizations:** Anti-hallucination, fail-safe entity saves

### Version 2 (Schema Evolution)
- **Migration:** Incremental changesets via changelog-master.xml
- **Changes:** Added business concept tables, cross-document analysis
- **Removed:** Deprecated entity types (costEstimates, usageProfiles, migrationNotes)
- **Improvements:** Capability/component distinction, confidence scoring

### Version 1 (Original)
- **File:** `project-centered-schema.sql` (base schema)
- **Features:** Core project/document tables, initial entity types

## Migration Strategy

### For New Projects (Recommended)

```yaml
# application.yml
spring:
  liquibase:
    change-log: classpath:db/changelog/sql/consolidated-schema-v3.sql
```

### For Existing Projects (Production)

```yaml
# application.yml
spring:
  liquibase:
    change-log: classpath:db/changelog/changelog-master.xml
```

## Database Schema Summary

### 30 Tables Organized by Purpose

**Core Tables (4):**
- projects
- documents
- knowledge_engine_processes
- ingestion_documents

**Chunks & Embeddings (3):**
- knowledge_source_chunks
- knowledge_chunks
- knowledge_facts

**Domain Organization (3):**
- knowledge_domains
- knowledge_subdomains
- knowledge_modules

**Business Architecture (6):**
- knowledge_capabilities (WHAT the business does)
- knowledge_roles
- knowledge_terms
- knowledge_policies
- knowledge_decisions
- knowledge_metrics

**Technical Architecture (5):**
- knowledge_components (HOW it's implemented)
- knowledge_apis
- knowledge_integrations
- knowledge_resources
- knowledge_workflows

**Data Models (2):**
- knowledge_data_models
- knowledge_data_fields

**Workflow Details (1):**
- knowledge_workflow_steps

**Rules & Notes (2):**
- knowledge_business_rules
- knowledge_notes

**Cross-Document Analysis (3):**
- knowledge_shared_entities
- knowledge_document_conflicts
- knowledge_document_gaps

**Relationships (1):**
- knowledge_relationships

## Key Features

### 1. Vector Embeddings
- 768-dimensional pgvector embeddings for semantic search
- HNSW indexes for efficient similarity queries
- Used for: entity matching, retrieval, deduplication

### 2. JSONB Metadata
- Flexible attribute storage without schema changes
- Efficient querying and indexing
- Used for: custom fields, AI-extracted attributes

### 3. TEXT Columns
- No size limits on string fields
- Prevents truncation of long content
- Applied to: names, descriptions, definitions, types

### 4. Project-Centered Multi-Tenancy
- All entities scoped by project_id
- CASCADE deletion for data isolation
- Efficient project-level queries

### 5. Cross-Document Analysis
- Detects shared entities across documents
- Identifies conflicts and contradictions
- Tracks documentation gaps
- Supports data quality and completeness verification

### 6. Anti-Hallucination Design
- Removed entity types prone to AI fabrication
- Confidence scoring for all extractions
- Source traceability via source_chunk_id
- Validation filters before persistence

## Rollback Strategy

### Incremental Migrations
Each changeset in `changelog-master.xml` includes rollback commands:
```xml
<rollback>
  <dropColumn schemaName="knowledge" tableName="..." columnName="..."/>
</rollback>
```

### Consolidated Schema
Rollback = Drop entire knowledge schema and restore from backup:
```sql
DROP SCHEMA IF EXISTS knowledge CASCADE;
-- Restore from backup
```

## Testing

### Verify Schema State
```sql
-- Check all tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'knowledge'
ORDER BY table_name;
-- Expected: 30 tables

-- Check TEXT column conversions
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'knowledge'
  AND data_type = 'text'
ORDER BY table_name, column_name;

-- Check vector indexes
SELECT indexname, tablename
FROM pg_indexes
WHERE schemaname = 'knowledge'
  AND indexname LIKE '%hnsw%';
-- Expected: 4 HNSW indexes
```

## Support

For questions or issues:
- Check entity alignment: `SCHEMA_ALIGNMENT_COMPLETE.md`
- Review schema analysis: `SCHEMA_ALIGNMENT_ANALYSIS.md`
- Changelog documentation: This file
