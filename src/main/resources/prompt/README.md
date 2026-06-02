# Knowledge Extraction Prompts - Architecture & Strategy

## Overview

This directory contains AI prompts used for extracting structured knowledge from enterprise documentation. The prompts follow **Schema v2** standards with comprehensive anti-hallucination rules, confidence scoring rubrics, and platform-specific element mappings.

## Prompt Architecture

### 1. Document-Level Analysis
**File:** `document-level-analysis-prompt.txt`

- **Purpose:** Analyzes entire documents BEFORE chunking to extract high-level context
- **Outputs:** Domain, subdomain, technologies, identified entities for entity linking
- **Used by:** `DocumentLevelAnalysisService.java`

### 2. Chunk-Level Extraction
Platform-specific prompts for extracting detailed knowledge from individual document chunks.

## Platform Routing Strategy

The system routes to platform-specific or generic prompts based on detected platform type:

### Platform-Specific Prompts (Comprehensive)

These prompts have detailed XML element mappings, platform-specific anti-hallucination rules, and optimized extraction patterns:

| Platform | Prompt File | Lines | Coverage |
|----------|-------------|-------|----------|
| TIBCO MDM | `tibco-mdm-extraction-prompt.txt` | 799 | 14/14 entity types |
| Pega BPM | `pega-bpm-extraction-prompt.txt` | 415 | 14/14 entity types |
| Camunda BPMN | `camunda-bpmn-extraction-prompt.txt` | 413 | 14/14 entity types |
| TIBCO BPM | `tibco-bpm-extraction-prompt.txt` | 27 | Delegates to enhanced |
| TIBCO BusinessWorks | `tibco-bw-extraction-prompt.txt` | 26 | Delegates to enhanced |

### Generic Category Prompts (Shared)

These prompts are shared across multiple platforms with similar patterns:

| Category | Platforms | Prompt File |
|----------|-----------|-------------|
| **Generic MDM** | Informatica, SAP, Oracle, IBM InfoSphere, Reltio, Semarchy | `mdm-xml-extraction-prompt.txt` |
| **Generic Workflow** | Activiti, JBPM, Flowable, BPMN 2.0, Oracle BPEL, IBM BPM, Appian, SAP Workflow | `workflow-xml-extraction-prompt.txt` |
| **Generic XML** | Any XML without specific platform detection | `generic-xml-extraction-prompt.txt` |
| **CSV** | CSV/TSV tabular data | `csv-extraction-prompt.txt` |
| **Generic Documents** | Markdown, Text, PDF, Word, PowerPoint, Images | `enhanced-chunk-extraction-prompt.txt` |

## Schema v2 Compliance

All prompts follow **Schema v2** standards with these mandatory requirements:

### 1. All 14 Entity Types

Every prompt MUST support extraction of all 14 entity types (empty arrays acceptable):

1. `businessRoles` - Process participants, actors, organizational units
2. `businessTerms` - Domain vocabulary, glossary terms
3. `businessPolicies` - SLA, compliance, governance policies
4. `businessDecisions` - Decision points, gateways, approvals
5. `businessMetrics` - KPIs, SLAs, performance targets
6. `businessCapabilities` - **WHAT** the business does (technology-independent)
7. `solutionComponents` - **HOW** it's implemented (technical solutions)
8. `apis` - Service interfaces, REST/SOAP endpoints
9. `dataModels` - Entities, tables, data structures
10. `integrations` - System connections, message flows
11. `deploymentResources` - Databases, servers, queues
12. `businessRules` - Validations, calculations, workflow rules
13. `businessFlows` - Processes, workflows, orchestrations
14. `relationships` - Dependencies, calls, data flows
15. `knowledgeNotes` - Architecture decisions, constraints

### 2. Deprecated Fields (DO NOT USE)

The following fields were removed in Schema v2:

❌ **Flow-level fields (removed):**
- `businessPurpose` - EMBED in `trigger` field instead
- `businessValue` - EMBED in `outcome` field instead
- `businessCriticality` - EMBED in `trigger` field instead
- `businessFrequency` - EMBED in `trigger` field instead
- `technicalDetails` - Belongs in `steps[]`, NOT at flow level

❌ **Rule-level fields (removed):**
- `ruleCategory` - Use `ruleType` instead
- `businessRationale` - EMBED in `condition` field instead
- `businessImpact` - EMBED in `outcome` field instead

❌ **Capability-level fields (removed):**
- `businessOwner` - Not in BusinessCapability.java

❌ **Removed entity types:**
- `costEstimates` - Rarely in docs, prone to hallucination
- `usageProfiles` - Rarely in docs, prone to hallucination
- `migrationNotes` - Consolidated into `knowledgeNotes`
- `businessComponents` - Replaced by `businessCapabilities` + `solutionComponents`

### 3. Embedding Business Context

Instead of deprecated fields, embed business context into existing fields:

**trigger field format:**
```
"trigger description. Purpose: WHY this exists. Criticality: High/Medium/Low"
```

**outcome field format:**
```
"outcome description. Value: business value delivered"
```

**condition field format (in rules):**
```
"[Validation] [Rationale: prevent fraud] actualConditionExpression"
```

**outcome field format (in rules):**
```
"result description. Impact: business impact. ErrorCode: code"
```

### 4. Constraints Field Type

❌ **WRONG (Array):**
```json
"constraints": ["Must be valid email", "Required field"]
```

✅ **CORRECT (Map/Object):**
```json
"constraints": {
  "minLength": 5,
  "maxLength": 100,
  "pattern": "^[A-Z]{2}[0-9]{4}$",
  "allowedValues": ["Active", "Inactive"],
  "defaultValue": "Active",
  "unique": true,
  "notNull": true
}
```

## Anti-Hallucination Rules

All comprehensive prompts include 10 anti-hallucination rules:

1. Extract ONLY information EXPLICITLY present in source XML/text
2. DO NOT infer entities beyond what exists in structure
3. DO NOT fabricate attribute values or content
4. PRESERVE exact element names and attributes
5. DO NOT assume standard schemas - extract actual content only
6. OMIT fields not present in source (empty arrays acceptable)
7. Use lower confidence (0.5-0.69) for implied information
8. DO NOT extract elements with only tag name, no content
9. When uncertain, omit rather than guess
10. DO NOT invent namespaces or qualifications

## Confidence Scoring Rubric

All comprehensive prompts use a 4-level confidence rubric:

### 0.9-1.0 (Very High Confidence)
- Element explicitly defined with multiple attributes AND descriptive content
- All key information present and unambiguous
- **Example:** `<Entity name="Customer" type="master" persistent="true"><Description>Master customer record</Description></Entity>`

### 0.7-0.89 (High Confidence)
- Element explicitly defined with some attributes OR descriptive content
- Most key information present (minimum: name + one other attribute)
- **Example:** `<Field name="emailAddress" type="String" required="true"/>`

### 0.5-0.69 (Medium Confidence)
- Element minimally defined with basic identification only
- Only name/id present, limited context
- **Example:** `<Validation name="CheckFormat"/>` (no condition, no description)

### Below 0.5 (Do Not Extract)
- Insufficient evidence to extract meaningful information
- Only tag name with no attributes or content
- **Example:** `<CustomEntity/>` (no name, no attributes, no content)

## Capability vs Component Distinction

**Critical Schema v2 Concept:** Separate WHAT (capability) from HOW (component)

### Business Capability (WHAT)
- Technology-independent business functions
- Stable over time even as technology changes
- Answer: "What does the business do?"
- **Examples:**
  - Order Management
  - Customer Onboarding
  - Risk Assessment
  - Payment Processing

### Solution Component (HOW)
- Technical implementations and solutions
- Technology-specific, may change with platform upgrades
- Answer: "How is this implemented?"
- **Examples:**
  - OrderService
  - CustomerAPI
  - RiskEngine
  - PaymentGateway

### Mapping in Prompts

| Platform | Capability Sources | Component Sources |
|----------|-------------------|-------------------|
| **Pega** | `<pega:CaseType>` | `<pega:Connector>`, `<pega:Utility>`, `<pega:Section>` |
| **Camunda** | High-level process names, pool participants | `<bpmn:serviceTask>`, `<bpmn:scriptTask>`, listeners |
| **TIBCO MDM** | `<Capability>`, `<BusinessFunction>` | `<Component>`, `<Service>`, `<Module>` |

## Token Optimization Strategy

### Design Goals
1. **Comprehensive:** All 14 entity types, anti-hallucination rules, confidence rubric
2. **Token-efficient:** 40-50% smaller than verbose baseline while maintaining quality
3. **Platform-specific:** Preserve XML element mappings and domain expertise

### Optimization Techniques

#### 1. Table Format for Mappings
**Before (verbose list - 20+ lines):**
```
**Case Structure:**
- <pega:CaseType> → businessCapabilities (case types represent capabilities)
- <pega:Stage> → businessFlow.steps (case lifecycle stages)
- <pega:Process> / <pega:Flow> → businessFlows
```

**After (compact table - 8 lines):**
```
| Pega Element | Maps To | Context |
|--------------|---------|---------|
| <pega:CaseType> | businessCapabilities | Case = business capability |
| <pega:Stage> | businessFlow.steps | Lifecycle stages |
```

**Token savings:** 30% reduction

#### 2. Consolidated Instructions
**Before (verbose field-by-field - 100+ lines):**
```
**modelName field:**
- Source: <Entity @name="X"> attribute
- Preserve EXACT casing and naming
- This is the PRIMARY identifier

**modelType field:**
- ALWAYS set to "entity"
- Alternative values: "table", "view"
[... 80 more lines]
```

**After (consolidated - 40 lines):**
```
**Entity Extraction:**
- Extract ALL entities as dataModels entries
- modelName: From @name (preserve exact casing)
- modelType: "entity|master_data|reference_data"
- description: Combine sources (see Field Population section)
```

**Token savings:** 60% reduction

#### 3. Reference Patterns
Create reusable patterns referenced from multiple sections rather than repeating:

```
=== FIELD POPULATION PATTERN ===
For multi-source description fields:
1. Primary definition (from <Description>)
2. Business name if different
3. Owner/Steward info
4. Sensitivity classification
```

Then reference: "Apply Field Population Pattern (section X)"

#### 4. Example Reduction
- Keep 1 example per concept (instead of 3-5)
- Remove redundant examples where format is self-explanatory
- **Token savings:** ~800 tokens per prompt

### Results

| Prompt | Original | Optimized | Reduction | Quality |
|--------|----------|-----------|-----------|---------|
| TIBCO MDM | 799 lines | 799 lines | Baseline | Comprehensive |
| Pega BPM | 276 lines | 415 lines | +50% (added missing) | Comprehensive |
| Camunda BPMN | 242 lines | 413 lines | +71% (added missing) | Comprehensive |

**Note:** Pega and Camunda increased in size because they were missing 3-6 entity types and lacked anti-hallucination rules. The optimized versions are 48% more efficient than TIBCO baseline while being comprehensive.

## Prompt Structure Template

All comprehensive prompts follow this structure:

```
1. INTRODUCTION (10 lines)
   - Role definition
   - Platform specificity
   - Critical reminders

2. DOCUMENT CONTEXT (10 lines)
   - Template variables
   - Entity linking context

3. PLATFORM ELEMENT MAPPINGS (60-80 lines)
   - Table format
   - Element → Entity type mappings

4. ANTI-HALLUCINATION RULES (25 lines)
   - 10 rules with platform-specific adaptations

5. CONFIDENCE SCORING RUBRIC (30 lines)
   - 4-level rubric with platform examples

6. EXTRACTION INSTRUCTIONS (80-100 lines)
   - Priority order
   - Consolidated patterns
   - Reference-based approach

7. OUTPUT FORMAT (180-220 lines)
   - All 14 entity types
   - Schema-aligned field names
   - Embedding guidance

8. CRITICAL RULES (40 lines)
   - Schema v2 alignment (6 rules)
   - Anti-hallucination (5 rules)
   - Platform-specific (5 rules)
   - Common pitfalls (7-8 items)

9. EXTRACTION VALIDATION CHECKLIST (15 lines)
   - Pre-submission verification

10. CHUNK TO ANALYZE (5 lines)
    - Template variable

TOTAL: 500-600 lines
```

## Adding a New Platform-Specific Prompt

### Step 1: Create Prompt File
1. Copy the closest platform template (e.g., `pega-bpm-extraction-prompt.txt` for BPM platforms)
2. Rename to `{platform}-extraction-prompt.txt`

### Step 2: Customize Platform Mappings
Update the **PLATFORM ELEMENT MAPPINGS** table:
```
| Platform Element | Maps To | Context |
|------------------|---------|---------|
| <platform:SpecificTag> | entityType | Mapping context |
```

### Step 3: Update Anti-Hallucination Rules
Adapt rules 4, 5, 9 with platform-specific guidance:
- Rule 4: Specific attribute preservation rules
- Rule 5: Platform-specific schema assumptions to avoid
- Rule 9: Platform-specific uncertainty scenarios

### Step 4: Create Confidence Examples
Provide 4 platform-specific XML examples (one per confidence level).

### Step 5: Add Extraction Instructions
Customize extraction priority order and platform-specific patterns.

### Step 6: Update Routing
Add routing in `EnhancedKnowledgeExtractionService.getPlatformSpecificPromptFile()`:

```java
case YOUR_PLATFORM -> "prompt/your-platform-extraction-prompt.txt";
```

### Step 7: Add JavaDoc
Update the method JavaDoc to document the new platform routing.

### Step 8: Test
Test with platform-specific XML samples to verify:
- ✅ All 14 entity types extractable
- ✅ No deprecated fields in output
- ✅ Confidence scores follow rubric
- ✅ Valid JSON structure
- ✅ Platform-specific element mappings work

## Maintenance Guidelines

### When Updating Prompts

1. **Schema Changes:**
   - Update ALL prompts to maintain schema consistency
   - Update this README with schema version notes
   - Update JavaDocs in Java DTOs

2. **Anti-Hallucination Rules:**
   - Keep 10 rules consistent across prompts
   - Adapt rules 4, 5, 9 for platform specifics
   - Test with known hallucination cases

3. **Confidence Rubric:**
   - Maintain 4-level rubric across all prompts
   - Update examples when XML patterns change
   - Validate score distribution in production

4. **Token Optimization:**
   - Measure token count before/after changes
   - Target: 500-600 lines for comprehensive prompts
   - Avoid verbose repetition

### Quality Assurance Checklist

Before deploying prompt changes:

- [ ] All 14 entity types present
- [ ] Zero deprecated fields used
- [ ] Constraints as Map not Array
- [ ] Anti-hallucination rules (10) present
- [ ] Confidence rubric (4 levels) present
- [ ] Extraction validation checklist present
- [ ] Common pitfalls documented
- [ ] Platform mappings in table format
- [ ] JavaDoc updated in routing logic
- [ ] Tested with real samples

## References

- **Schema Authority:** `KnowledgeExtractionResult.java` - All 14 entity types
- **Routing Logic:** `EnhancedKnowledgeExtractionService.getPlatformSpecificPromptFile()`
- **Service Processing:** `KnowledgeGraphService.java` - Chunk extraction
- **Storage:** `PostgresStorageService.java` - Entity persistence
- **Gold Standard:** `tibco-mdm-extraction-prompt.txt` - Comprehensive example

## Version History

### Schema v2 (Current)
- **Added:** businessCapabilities (WHAT), solutionComponents alias for technicalComponents (HOW)
- **Removed:** costEstimates, usageProfiles, migrationNotes, businessComponents
- **Removed Deprecated Fields:** businessPurpose, businessValue at flow level, ruleCategory
- **Fixed:** constraints from Array to Map<String,Object>
- **Enhanced:** Embedding business context in trigger/outcome/condition fields

### Schema v1 (Legacy)
- Used technicalComponents without businessCapabilities distinction
- Had businessPurpose, businessValue as separate flow-level fields
- Used ruleCategory instead of ruleType
- Had constraints as Array
