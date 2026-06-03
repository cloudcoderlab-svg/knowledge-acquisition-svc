# Knowledge Acquisition Service

> AI-powered knowledge extraction and reverse engineering service for enterprise software modernization, migration, and documentation generation.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Supported File Types](#supported-file-types)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Analysis Capabilities](#analysis-capabilities)
- [Configuration](#configuration)
- [Development](#development)
- [Package Structure](#package-structure)
- [Examples](#examples)

---

## Overview

The **Knowledge Acquisition Service** is an intelligent document and source code analysis platform that extracts structured business and technical knowledge from enterprise artifacts. It uses AI-powered extraction to reverse-engineer:

- **Business Requirements** → Epics, User Stories, Acceptance Criteria
- **High-Level Design (HLD)** → Architecture diagrams, component models, data models
- **Test Cases** → Test scenarios in Given-When-Then format
- **Business Rules** → Validation logic, decision rules, calculations
- **Workflows** → Business processes, orchestrations, state machines
- **Technical Architecture** → Components, APIs, integrations, data flows

### Use Cases

1. **Legacy System Modernization** - Extract knowledge from old documentation and code
2. **Migration Projects** - Understand existing systems before cloud/platform migration
3. **Documentation Generation** - Auto-generate technical and business documentation
4. **Regulatory Compliance** - Document business rules and processes for audits
5. **Knowledge Transfer** - Capture tribal knowledge from legacy systems
6. **Reverse Engineering** - Extract business logic from source code

---

## Key Features

### 🤖 AI-Powered Extraction
- **Multi-modal AI parsing** using Google Gemini for PDF, images, Office documents
- **Apache Tika extraction** for text-based files and source code
- **Platform-specific prompts** optimized for TIBCO, Pega, Camunda, SAP, Oracle, and more
- **Source code analysis** for Java, Python, PHP, JSP, SQL, Oracle Forms

### 📊 Comprehensive Knowledge Extraction
Extracts **14 entity types** following Schema v2:
1. **Business Roles** - Process participants, actors, organizational units
2. **Business Terms** - Domain vocabulary, glossary terms
3. **Business Policies** - SLA, compliance, governance policies
4. **Business Decisions** - Decision points, gateways, approvals
5. **Business Metrics** - KPIs, SLAs, performance targets
6. **Business Capabilities** - WHAT the business does (technology-independent)
7. **Solution Components** - HOW it's implemented (technical solutions)
8. **APIs** - Service interfaces, REST/SOAP endpoints
9. **Data Models** - Entities, tables, data structures
10. **Integrations** - System connections, message flows
11. **Deployment Resources** - Databases, servers, queues
12. **Business Rules** - Validations, calculations, workflow rules
13. **Business Flows** - Processes, workflows, orchestrations
14. **Relationships** - Dependencies, calls, data flows

### 📈 Readiness Analysis
- **Agile Readiness** - Can extracted knowledge generate epics and user stories?
- **HLD Readiness** - Sufficient architecture knowledge for design documents?
- **Test Readiness** - Can test cases be auto-generated from business rules?

### 🔄 Complete Processing Pipeline
1. **Ingestion** - Parse documents from Google Cloud Storage
2. **Document-Level Analysis** - Identify domain, platform, technologies
3. **Chunk Extraction** - Break into manageable pieces with context preservation
4. **Knowledge Graph** - Store entities and relationships in PostgreSQL
5. **Consolidation** - Cross-document relationship inference
6. **Planning Generation** - Generate delivery artifacts
7. **Embeddings** - Vector embeddings for semantic search

---

## Supported File Types

### 📄 Documents
| Type | Extensions | Parser |
|------|------------|--------|
| **PDF** | `.pdf` | Gemini Multimodal |
| **Microsoft Office** | `.doc`, `.docx`, `.ppt`, `.pptx`, `.xls`, `.xlsx` | Gemini Multimodal |
| **Images** | `.png`, `.jpg`, `.jpeg`, `.gif`, `.bmp`, `.webp` | Gemini Multimodal |
| **Markdown** | `.md`, `.markdown` | Tika |
| **Text** | `.txt`, `.log` | Tika |
| **CSV/TSV** | `.csv`, `.tsv` | Tika + CSV-specific prompt |

### 🖥️ Source Code (NEW)
| Language | Extensions | Extracts |
|----------|------------|----------|
| **Java** | `.java` | Classes, @Annotations, JavaDoc, Spring components, REST APIs, JPA entities |
| **Python** | `.py` | Classes, decorators, docstrings, Django/Flask routes, SQLAlchemy models |
| **PHP** | `.php` | Classes, PHPDoc, Laravel routes, Eloquent models, middleware |
| **JSP** | `.jsp` | JSP tags, embedded Java, form definitions |
| **SQL** | `.sql` | CREATE TABLE, stored procedures, triggers, views, constraints, foreign keys |
| **Oracle Forms** | `.fmb` | Form blocks, items, triggers, LOVs |

### 🏢 Enterprise Platforms (XML/Process Definitions)
| Platform | Detection | Prompt |
|----------|-----------|--------|
| **TIBCO MDM** | `<Entity>`, `<Rulebase>`, `<WorkflowTemplate>` | Platform-specific |
| **TIBCO BPM** | TIBCO BPM namespaces | Platform-specific |
| **TIBCO BusinessWorks** | TIBCO BW namespaces | Platform-specific |
| **Pega BPM** | `<pega:CaseType>`, `<pega:Flow>` | Platform-specific |
| **Camunda BPMN** | Camunda extensions, BPMN 2.0 | Platform-specific |
| **Informatica MDM** | `<BDM>`, `<businessEntity>` | Generic MDM |
| **SAP MDM** | `<MainTable>`, `<LookupTable>` | Generic MDM |
| **Oracle MDM** | `<EntityDef>`, `<AttributeDef>` | Generic MDM |
| **Activiti BPMN** | Activiti namespaces | Generic Workflow |
| **jBPM** | jBPM namespaces | Generic Workflow |
| **Flowable** | Flowable namespaces | Generic Workflow |
| **Oracle BPEL** | `<process name=`, `<humanTask>` | Generic Workflow |
| **IBM BPM** | `<process-app>`, `<toolkit>` | Generic Workflow |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    REST API Layer                                │
│  ProjectController │ ProcessingController │ AnalysisController  │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼─────────────────────────────────────┐
│                    Service Layer                                  │
│  ProjectPipelineService │ KnowledgeIngestionService              │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌──────────────────┐  ┌────────────────────┐
│  GCS Storage    │  │   AI Services    │  │  Database Layer    │
│  (Documents)    │  │  - Gemini        │  │  - PostgreSQL      │
│                 │  │  - Vertex AI     │  │  - JPA/Hibernate   │
│                 │  │  - Embeddings    │  │  - Liquibase       │
└─────────────────┘  └──────────────────┘  └────────────────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │  Knowledge Graph  │
                    │  - Entities       │
                    │  - Relationships  │
                    │  - Chunks         │
                    └───────────────────┘
```

### Processing Pipeline

```
1. Upload Documents → GCS
         │
2. Ingestion Pipeline
         ├─ Parse (Tika/Gemini)
         ├─ Document-Level Analysis
         ├─ Platform Detection
         ├─ Chunking
         └─ Enhanced Extraction (AI)
         │
3. Knowledge Graph
         ├─ Store Entities
         ├─ Store Relationships
         └─ Generate Embeddings
         │
4. Consolidation
         ├─ Cross-Document Relationships
         ├─ Golden Chunk Generation
         └─ Planning Artifacts
         │
5. Analysis & Readiness Scoring
         ├─ Agile Readiness
         ├─ HLD Readiness
         └─ Test Readiness
```

---

## Getting Started

### Prerequisites

- **Java 21+**
- **PostgreSQL 14+**
- **Google Cloud Platform Account** (for GCS and Vertex AI)
- **Gradle 8.x** (included via wrapper)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd knowledge-acquisition-svc
   ```

2. **Configure database**
   ```bash
   # Create PostgreSQL database
   createdb kengine-db

   # Update connection in application-dev.yml
   ```

3. **Set up GCP credentials**
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"
   ```

4. **Run the service**
   ```powershell
   # Windows
   .\gradlew bootRun

   # Linux/Mac
   ./gradlew bootRun
   ```

5. **Access Swagger UI**
   ```
   http://localhost:8087/swagger-ui.html
   ```

### Quick Start Example

```bash
# 1. Create a project
curl -X POST http://localhost:8087/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{"projectName": "LegacyApp", "description": "Legacy Java application"}'

# 2. Upload files to GCS (use the upload URL from response)

# 3. Start processing pipeline
curl -X POST http://localhost:8087/api/v1/projects/{projectId}/processes/pipeline

# 4. Check agile readiness
curl http://localhost:8087/api/v1/analysis/projects/{projectId}/agile-readiness

# 5. Check HLD readiness
curl http://localhost:8087/api/v1/analysis/projects/{projectId}/hld-readiness

# 6. Check test readiness
curl http://localhost:8087/api/v1/analysis/projects/{projectId}/test-readiness
```

---

## API Documentation

### Project Management

#### Create Project
```http
POST /api/v1/projects
Content-Type: application/json

{
  "projectName": "MyProject",
  "description": "Project description"
}
```

#### Get Project
```http
GET /api/v1/projects/{projectId}
```

#### List Projects
```http
GET /api/v1/projects
```

#### Delete Project
```http
DELETE /api/v1/projects/{projectId}
```

### Processing Pipeline

#### Start Single Project Pipeline
```http
POST /api/v1/projects/{projectId}/processes/pipeline
```

Executes all phases:
1. Ingestion
2. Consolidation
3. Planning Generation
4. Project Summary

#### Start Batch Pipeline
```http
POST /api/v1/projects/processes/pipeline
Content-Type: application/json

{
  "projectIds": ["uuid1", "uuid2", "uuid3"]
}
```

#### Monitor Process
```http
GET /api/v1/monitoring/processes/{processId}
```

Returns:
- Process status (PENDING, IN_PROGRESS, COMPLETED, FAILED)
- Progress percentage
- Current stage
- Error messages (if any)

### Analysis Endpoints

#### Agile Readiness Analysis
```http
GET /api/v1/analysis/projects/{projectId}/agile-readiness
```

**Response:**
```json
{
  "projectId": "uuid",
  "totalWorkflows": 45,
  "workflowsWithTriggers": 40,
  "workflowsWithOutcomes": 38,
  "workflowsWithActors": 42,
  "workflowCompletenessPercent": 88.9,
  "totalBusinessRules": 120,
  "rulesWithConditions": 110,
  "rulesWithValidation": 105,
  "rulesWithOutcomes": 115,
  "ruleCompletenessPercent": 91.7,
  "totalDomains": 8,
  "domainsWithDescriptions": 8,
  "domainCompletenessPercent": 100.0,
  "overallAgileReadinessScore": 93.5,
  "recommendations": [
    "EXCELLENT: Knowledge is ready for automated epic and story generation."
  ]
}
```

**Scoring:**
- **0-30**: CRITICAL - Insufficient for artifact generation
- **30-60**: WARNING - Significant gaps, manual enrichment needed
- **60-80**: GOOD - Suitable with minor manual enrichment
- **80-100**: EXCELLENT - Ready for automated generation

#### HLD Readiness Analysis
```http
GET /api/v1/analysis/projects/{projectId}/hld-readiness
```

**Response:**
```json
{
  "projectId": "uuid",
  "totalDataModels": 35,
  "modelsWithBusinessDefinition": 32,
  "modelsWithSchema": 30,
  "dataModelCompletenessPercent": 88.1,
  "totalComponents": 28,
  "componentsWithResponsibility": 25,
  "componentCompletenessPercent": 89.3,
  "totalRelationships": 85,
  "relationshipDensityPercent": 135.0,
  "overallHLDReadinessScore": 90.8,
  "recommendations": [
    "EXCELLENT: Ready for automated HLD and architecture diagram generation."
  ]
}
```

#### Test Readiness Analysis
```http
GET /api/v1/analysis/projects/{projectId}/test-readiness
```

**Response:**
```json
{
  "projectId": "uuid",
  "totalWorkflows": 45,
  "testableWorkflows": 40,
  "workflowTestabilityPercent": 88.9,
  "totalBusinessRules": 120,
  "testableBusinessRules": 110,
  "ruleTestabilityPercent": 91.7,
  "estimatedTestCases": 340,
  "overallTestReadinessScore": 90.3,
  "recommendations": [
    "EXCELLENT: Ready for automated test case generation with given-when-then format."
  ]
}
```

**Test Case Estimation:**
- 3 test scenarios per workflow (happy path, edge case, error case)
- 2 test scenarios per business rule (valid input, invalid input)

### Data Quality

#### Get Data Quality Metrics
```http
GET /api/v1/data-quality/projects/{projectId}/metrics
```

Returns quality metrics for extracted knowledge.

---

## Analysis Capabilities

### What Can Be Generated?

| Artifact | Requirements | Generated From |
|----------|-------------|----------------|
| **Epics** | Domains + Business Capabilities | Domain groupings, high-level business functions |
| **User Stories** | Workflows with triggers/actors/outcomes | Business flows in "As a...When...Then" format |
| **Acceptance Criteria** | Business Rules with conditions/outcomes | Validation rules, decision logic |
| **HLD Documents** | Components + Data Models + Relationships | Architecture, component diagrams, ERDs |
| **Component Diagrams** | Solution Components + Relationships | Technical architecture, service dependencies |
| **ERD Diagrams** | Data Models + Foreign Keys | Database schema, entity relationships |
| **Test Cases** | Workflows + Business Rules | Given-When-Then test scenarios |
| **API Documentation** | APIs with endpoints/parameters | REST/SOAP API specifications |

### Readiness Score Interpretation

#### Agile Readiness
- **Workflows**: Need triggers (Given), actors (As a), outcomes (Then)
- **Business Rules**: Need conditions (When), validation (criteria), outcomes (Then)
- **Domains**: Need descriptions for epic categorization

#### HLD Readiness
- **Data Models**: Need schema definitions for ERDs
- **Components**: Need responsibility descriptions for component diagrams
- **Relationships**: Need high density for architecture visualization

#### Test Readiness
- **Workflows**: Need trigger + outcome for integration tests
- **Business Rules**: Need condition + validation for unit tests

---

## Configuration

### Application Properties

Key configuration in `application-dev.yml`:

```yaml
server:
  port: 8087

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/kengine-db
    username: ${KENGINE_DB_USER}
    password: ${KENGINE_DB_PASSWORD}

knowledge-engine:
  # Enable/disable multimodal parsing (Gemini)
  parser:
    use-multimodal: true
    multimodal-types: pdf,png,jpg,jpeg,docx,pptx

  # Enable/disable document-level analysis
  extraction:
    enable-document-level-analysis: true

  # GCS configuration
  gcs:
    bucket-name: ${GCS_BUCKET_NAME}
    project-id: ${GCP_PROJECT_ID}

  # Vertex AI configuration
  vertex-ai:
    project-id: ${GCP_PROJECT_ID}
    location: us-central1
    model: gemini-1.5-pro
```

### Environment Variables

```bash
# Database
export KENGINE_DB_USER=kengine_app
export KENGINE_DB_PASSWORD=yourpassword

# GCP
export GCP_PROJECT_ID=your-project-id
export GCS_BUCKET_NAME=your-bucket-name
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
```

---

## Development

### Build

```powershell
# Compile
.\gradlew build

# Run tests
.\gradlew test

# Skip tests
.\gradlew build -x test

# Clean build
.\gradlew clean build
```

### Testing

```powershell
# Run all tests
.\gradlew test

# Run specific test
.\gradlew test --tests "KnowledgeIngestionServiceTest"

# Generate coverage report
.\gradlew test jacocoTestReport
```

### Database Migrations

Using Liquibase:

```bash
# View pending changes
.\gradlew liquibaseStatus

# Update database
.\gradlew liquibaseUpdate

# Rollback last changeset
.\gradlew liquibaseRollbackCount -PliquibaseCommandValue=1
```

### Code Style

- **Java**: Follow Google Java Style Guide
- **Formatting**: Use `google-java-format`
- **Linting**: Checkstyle configured in `build.gradle`

---

## Package Structure

```
src/main/java/com/knowledge/acquisition/
│
├── controller/                  # REST API endpoints
│   ├── ProjectController.java
│   ├── ProcessingController.java
│   ├── KnowledgeAnalysisController.java
│   ├── DataQualityController.java
│   └── ProcessMonitoringController.java
│
├── service/                     # Business logic layer
│   ├── ProjectService.java
│   ├── ProjectPipelineService.java
│   └── GcsProjectFileService.java
│
├── ingestion/                   # Core ingestion engine
│   ├── parser/                  # Document parsing
│   │   ├── DocumentParserOrchestrator.java
│   │   ├── TikaContentExtractor.java
│   │   └── GeminiMultimodalExtractor.java
│   │
│   ├── service/                 # Extraction services
│   │   ├── KnowledgeIngestionService.java
│   │   ├── DocumentLevelAnalysisService.java
│   │   ├── EnhancedKnowledgeExtractionService.java
│   │   ├── KnowledgeGraphService.java
│   │   ├── CrossDocumentRelationshipService.java
│   │   └── XMLPlatformDetector.java
│   │
│   └── service/ai/              # AI integration
│       ├── VertexAIService.java
│       ├── EmbeddingService.java
│       └── SemanticClassificationService.java
│
├── entity/                      # JPA entities
│   ├── ProjectEntity.java
│   ├── WorkflowEntity.java
│   ├── BusinessRuleEntity.java
│   ├── DataModelEntity.java
│   └── ComponentEntity.java
│
├── repository/                  # Data access layer
│   ├── ProjectRepository.java
│   ├── WorkflowRepository.java
│   └── BusinessRuleRepository.java
│
└── dto/                         # Data transfer objects
    ├── KnowledgeExtractionResult.java
    ├── DocumentKnowledge.java
    └── ProcessResponse.java
```

### Resources Structure

```
src/main/resources/
│
├── prompt/                      # AI extraction prompts
│   ├── enhanced-chunk-extraction-prompt.txt
│   ├── source-code-extraction-prompt.txt      # NEW
│   ├── tibco-mdm-extraction-prompt.txt
│   ├── pega-bpm-extraction-prompt.txt
│   ├── camunda-bpmn-extraction-prompt.txt
│   └── README.md                               # Prompt documentation
│
├── db/changelog/                # Database migrations
│   ├── changelog-master.xml
│   └── sql/
│       └── project-centered-schema.sql
│
└── application-dev.yml          # Configuration
```

---

## Examples

### Example 1: Process Java Codebase

```bash
# 1. Create project
curl -X POST http://localhost:8087/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "OrderManagementService",
    "description": "Legacy Java order processing system"
  }'

# 2. Upload Java files to GCS
# Use GCS upload URL from project creation response

# 3. Start pipeline
curl -X POST http://localhost:8087/api/v1/projects/{projectId}/processes/pipeline

# 4. Wait for completion, then analyze
curl http://localhost:8087/api/v1/analysis/projects/{projectId}/agile-readiness
```

**Expected Extraction from Java Code:**
- **Classes** with `@Service` → Business Capabilities
- **REST Controllers** with `@RestController` → APIs
- **JPA Entities** with `@Entity` → Data Models
- **Validation Annotations** → Business Rules
- **Service Methods** with `@Transactional` → Business Flows
- **Foreign Keys** in entities → Relationships

### Example 2: Process TIBCO MDM XML

```bash
# Upload TIBCO MDM files (.xml with <Entity>, <Rulebase>)
# Platform auto-detected as TIBCO_MDM
# Uses specialized tibco-mdm-extraction-prompt.txt

# Result:
# - Master data entities → Data Models
# - Workflow templates → Business Flows
# - Validation rules → Business Rules
# - Catalog validations → Business Rules
```

### Example 3: Process SQL Schema

```bash
# Upload SQL DDL files (.sql)
# Detected as SOURCE_CODE_DOCUMENT
# Uses source-code-extraction-prompt.txt

# Extracted:
# - CREATE TABLE → Data Models with fields
# - PRIMARY KEY → Constraints
# - FOREIGN KEY → Relationships
# - CHECK constraints → Business Rules
# - Stored procedures → Business Flows
# - Triggers → Business Rules
```

### Example 4: Batch Processing

```bash
# Process multiple projects concurrently
curl -X POST http://localhost:8087/api/v1/projects/processes/pipeline \
  -H "Content-Type: application/json" \
  -d '{
    "projectIds": [
      "project-uuid-1",
      "project-uuid-2",
      "project-uuid-3"
    ]
  }'
```

---

## Anti-Hallucination Features

The service includes robust anti-hallucination mechanisms:

1. **10 Anti-Hallucination Rules** in all extraction prompts
2. **Confidence Scoring** (0.5-1.0) for all extracted entities
3. **Platform-Specific Element Mappings** prevent XML misinterpretation
4. **Source Preservation** - Exact class/method/table names preserved
5. **Empty Arrays Preferred** over fabricated entities
6. **Context Validation** against document-level analysis

### Confidence Score Rubric

- **0.9-1.0**: Very High - Explicitly documented with comments/JavaDoc
- **0.7-0.89**: High - Descriptive naming, annotations present
- **0.5-0.69**: Medium - Minimal naming, inferred from code structure
- **< 0.5**: Rejected - Insufficient evidence

---

## Troubleshooting

### Common Issues

**Issue: Low agile readiness score**
- **Cause**: Workflows missing triggers, actors, or outcomes
- **Solution**: Ensure documents/code include business context, not just technical details

**Issue: Source code not extracting business logic**
- **Cause**: Missing JavaDoc, comments, or descriptive naming
- **Solution**: Add documentation to code or accept lower confidence scores

**Issue: Platform not detected correctly**
- **Cause**: XML namespace or structure doesn't match detection patterns
- **Solution**: Check `XMLPlatformDetector.java` patterns or use generic extraction

**Issue: Processing pipeline stuck**
- **Cause**: Large files, AI quota exceeded, network issues
- **Solution**: Check process monitoring endpoint, review logs

### Logs

```powershell
# View logs
.\gradlew bootRun --info

# Debug mode
.\gradlew bootRun --debug
```

---

## Roadmap

- [ ] Support for C#/.NET source code
- [ ] Support for JavaScript/TypeScript
- [ ] GraphQL API support
- [ ] Real-time processing with WebSocket notifications
- [ ] Export to Jira/Azure DevOps for epic/story creation
- [ ] Generate PlantUML diagrams from architecture
- [ ] Support for JSON schema files
- [ ] Machine learning model fine-tuning per domain

---

## Contributing

1. Follow Google Java Style Guide
2. Write unit tests for new features
3. Update documentation (README, JavaDoc, Swagger)
4. Run `.\gradlew test` before committing
5. Use conventional commit messages

---

## License

Proprietary - All rights reserved

---

## Contact

For questions or support, contact the Knowledge Engine team.

---

**Last Updated**: 2026-06-03
**Version**: 1.0
**Service Port**: 8087
