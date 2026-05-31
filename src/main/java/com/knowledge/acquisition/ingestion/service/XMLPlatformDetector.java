package com.knowledge.acquisition.ingestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Detects the platform/type of XML documents by analyzing their structure, namespaces, and
 * elements. This allows routing to platform-specific extraction prompts for better precision.
 */
@Service
@Slf4j
public class XMLPlatformDetector {

  /** Detected XML platform types */
  public enum XMLPlatform {
    TIBCO_MDM,
    TIBCO_BPM,
    TIBCO_BUSINESSWORKS,
    INFORMATICA_MDM,
    SAP_MDM,
    ORACLE_MDM,
    IBM_INFOSPHERE_MDM,
    RELTIO_MDM,
    SEMARCHY_MDM,
    PEGA_BPM,
    CAMUNDA_BPMN,
    ACTIVITI_BPMN,
    JBPM,
    FLOWABLE_BPMN,
    BPMN_20_GENERIC,
    ORACLE_BPEL,
    IBM_BPM,
    APPIAN,
    SAP_WORKFLOW,
    GENERIC_XML,
    CSV_DOCUMENT,
    MARKDOWN_DOCUMENT,
    TEXT_DOCUMENT,
    PDF_DOCUMENT,
    WORD_DOCUMENT,
    POWERPOINT_DOCUMENT,
    IMAGE_DOCUMENT,
    GENERIC_DOCUMENT,
    UNKNOWN_DOCUMENT
  }

  /**
   * Detects the platform type of an XML document by analyzing its content.
   *
   * @param xmlContent The XML content to analyze
   * @return The detected platform type
   */
  public XMLPlatform detectPlatform(String xmlContent) {
    return detectPlatform(xmlContent, null);
  }

  /**
   * Detects the platform type of an XML document by analyzing its content and source object name.
   *
   * @param xmlContent The XML content to analyze
   * @param objectName The source object name, if available
   * @return The detected platform type
   */
  public XMLPlatform detectPlatform(String xmlContent, String objectName) {
    if (xmlContent == null || xmlContent.isBlank()) {
      return XMLPlatform.GENERIC_XML;
    }

    String content = xmlContent.trim();

    // ========================================================================
    // MDM Platforms Detection
    // ========================================================================

    // TIBCO MDM - Look for TIBCO MDM specific elements
    if (containsAny(
            content,
            "<Repository",
            "<Entity",
            "xmlns:tibco",
            "tibco.mdm",
            "<Validation",
            "<WorkflowTemplate",
            "<Rulebase",
            "<RuleBase",
            "<Catalog",
            "<Relationship")
        && containsAny(content, "mdm", "Repository", "Entity", "Catalog", "Affiliation")) {
      log.info("Detected TIBCO MDM XML");
      return XMLPlatform.TIBCO_MDM;
    }

    if (hasTibcoMdmObjectName(objectName)
        && containsAny(
            content,
            "Repository",
            "Catalog",
            "Affiliation",
            "Relationship",
            "Workflow",
            "WorkItem",
            "Rule",
            "Validation",
            "Record",
            "Attribute",
            "MassLoad")) {
      log.info("Detected TIBCO MDM XML");
      return XMLPlatform.TIBCO_MDM;
    }

    // Informatica MDM
    if (containsAny(content, "<BDM", "<businessEntity", "<Cleanse", "<Match", "informatica.mdm")) {
      log.info("Detected Informatica MDM XML");
      return XMLPlatform.INFORMATICA_MDM;
    }

    // SAP MDM
    if (containsAny(content, "<MainTable", "<LookupTable", "<QualifierTable", "sap.mdm", "SAP_MDM")
        && containsAny(content, "Repository", "Table")) {
      log.info("Detected SAP MDM XML");
      return XMLPlatform.SAP_MDM;
    }

    // Oracle MDM
    if (containsAny(content, "<EntityDef", "<AttributeDef", "<RelationshipDef", "oracle.mdm")) {
      log.info("Detected Oracle MDM XML");
      return XMLPlatform.ORACLE_MDM;
    }

    // IBM InfoSphere MDM
    if (containsAny(
        content, "<BusinessObject", "<ComponentObject", "<BusinessObjectAttribute", "ibm.mdm")) {
      log.info("Detected IBM InfoSphere MDM XML");
      return XMLPlatform.IBM_INFOSPHERE_MDM;
    }

    // Reltio MDM (JSON-like, but can be XML too)
    if (containsAny(content, "reltio", "crosswalk", "survivorship")
        && contains(content, "entity")) {
      log.info("Detected Reltio MDM XML");
      return XMLPlatform.RELTIO_MDM;
    }

    // Semarchy xDM
    if (containsAny(content, "<model", "<entity", "semarchy", "<stewardship", "<enricher>")) {
      log.info("Detected Semarchy xDM XML");
      return XMLPlatform.SEMARCHY_MDM;
    }

    // ========================================================================
    // BPM/Workflow Platforms Detection
    // ========================================================================

    // TIBCO BPM (ActiveMatrix BPM) - Look for pd:ProcessDefinition
    if (containsAny(content, "<pd:ProcessDefinition", "<pd:activity", "<pd:transition")
        && !containsAny(content, "Repository", "Entity", "mdm")) {
      log.info("Detected TIBCO BPM (ActiveMatrix) XML");
      return XMLPlatform.TIBCO_BPM;
    }

    // TIBCO BusinessWorks (Integration/ESB)
    if (containsAny(
            content,
            "<pd:ProcessDefinition",
            "tibco.plugin",
            "tibco.bw",
            "<pd:starter",
            "<pd:coercions")
        && containsAny(content, "integration", "adapter", "service")) {
      log.info("Detected TIBCO BusinessWorks XML");
      return XMLPlatform.TIBCO_BUSINESSWORKS;
    }

    // Pega BPM
    if (containsAny(
        content, "<pega:Case", "<pega:Flow", "<pega:Process", "Rule-Obj-Flow", "<pega:Stage")) {
      log.info("Detected Pega BPM XML");
      return XMLPlatform.PEGA_BPM;
    }

    // Camunda BPMN
    if (containsAny(
        content, "camunda.org/schema", "<camunda:", "zeebe:", "<camunda:executionListener")) {
      log.info("Detected Camunda BPMN XML");
      return XMLPlatform.CAMUNDA_BPMN;
    }

    // Activiti BPMN
    if (containsAny(content, "activiti.org/bpmn", "<activiti:", "<activiti:formProperty")) {
      log.info("Detected Activiti BPMN XML");
      return XMLPlatform.ACTIVITI_BPMN;
    }

    // jBPM
    if (containsAny(content, "drools.org", "<drools:", "jbpm.org")) {
      log.info("Detected jBPM XML");
      return XMLPlatform.JBPM;
    }

    // Flowable BPMN
    if (containsAny(content, "flowable.org", "<flowable:")) {
      log.info("Detected Flowable BPMN XML");
      return XMLPlatform.FLOWABLE_BPMN;
    }

    // Generic BPMN 2.0
    if (containsAny(
        content,
        "bpmn.org/schema/BPMN",
        "<bpmn:process",
        "<bpmn2:process",
        "<bpmn:definitions",
        "<bpmn2:definitions")) {
      log.info("Detected BPMN 2.0 Generic XML");
      return XMLPlatform.BPMN_20_GENERIC;
    }

    // Oracle BPEL
    if (containsAny(
        content, "<process name=", "<bpelx:", "oracle.com/bpel", "<humanTask", "<invoke")) {
      log.info("Detected Oracle BPEL XML");
      return XMLPlatform.ORACLE_BPEL;
    }

    // IBM BPM (Lombardi)
    if (containsAny(content, "<process-app", "<toolkit", "ibm.com/bpm", "<participant")) {
      log.info("Detected IBM BPM XML");
      return XMLPlatform.IBM_BPM;
    }

    // Appian
    if (containsAny(content, "<processModel", "<smart-service", "appian.com", "<node type=")) {
      log.info("Detected Appian XML");
      return XMLPlatform.APPIAN;
    }

    // SAP Workflow
    if (containsAny(content, "<WorkflowTemplate", "<STEP>", "<AGENT>", "sap.workflow")
        && !containsAny(content, "MainTable", "LookupTable")) {
      log.info("Detected SAP Workflow XML");
      return XMLPlatform.SAP_WORKFLOW;
    }

    // ========================================================================
    // Default to Generic XML
    // ========================================================================
    log.info("No specific platform detected, using generic XML extraction");
    return XMLPlatform.GENERIC_XML;
  }

  /**
   * Checks if content contains the given substring (case-insensitive).
   *
   * @param content The content to search
   * @param substring The substring to find
   * @return true if found
   */
  private boolean contains(String content, String substring) {
    return content.toLowerCase().contains(substring.toLowerCase());
  }

  /**
   * Checks if content contains any of the given substrings (case-insensitive).
   *
   * @param content The content to search
   * @param substrings The substrings to find
   * @return true if any substring is found
   */
  private boolean containsAny(String content, String... substrings) {
    for (String substring : substrings) {
      if (contains(content, substring)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasTibcoMdmObjectName(String objectName) {
    if (objectName == null || objectName.isBlank()) {
      return false;
    }
    String fileName = objectName.substring(objectName.lastIndexOf('/') + 1).toLowerCase();
    return fileName.startsWith("rb")
        || fileName.startsWith("wf")
        || fileName.startsWith("cv")
        || fileName.startsWith("fm")
        || fileName.contains("catalogvalidation")
        || fileName.contains("affiliation");
  }

  /**
   * Gets the recommended prompt file name for a given platform.
   *
   * @param platform The detected platform
   * @return The prompt file name (without path)
   */
  public String getPromptFileName(XMLPlatform platform) {
    return switch (platform) {
      case TIBCO_MDM -> "tibco-mdm-extraction-prompt.txt";
      case TIBCO_BPM -> "tibco-bpm-extraction-prompt.txt";
      case TIBCO_BUSINESSWORKS -> "tibco-bw-extraction-prompt.txt";
      case INFORMATICA_MDM -> "informatica-mdm-extraction-prompt.txt";
      case SAP_MDM -> "sap-mdm-extraction-prompt.txt";
      case ORACLE_MDM -> "oracle-mdm-extraction-prompt.txt";
      case IBM_INFOSPHERE_MDM -> "ibm-mdm-extraction-prompt.txt";
      case RELTIO_MDM -> "reltio-mdm-extraction-prompt.txt";
      case SEMARCHY_MDM -> "semarchy-mdm-extraction-prompt.txt";
      case PEGA_BPM -> "pega-bpm-extraction-prompt.txt";
      case CAMUNDA_BPMN -> "camunda-bpmn-extraction-prompt.txt";
      case ACTIVITI_BPMN -> "activiti-bpmn-extraction-prompt.txt";
      case JBPM -> "jbpm-extraction-prompt.txt";
      case FLOWABLE_BPMN -> "flowable-bpmn-extraction-prompt.txt";
      case BPMN_20_GENERIC -> "bpmn-20-extraction-prompt.txt";
      case ORACLE_BPEL -> "oracle-bpel-extraction-prompt.txt";
      case IBM_BPM -> "ibm-bpm-extraction-prompt.txt";
      case APPIAN -> "appian-extraction-prompt.txt";
      case SAP_WORKFLOW -> "sap-workflow-extraction-prompt.txt";
      case GENERIC_XML -> "generic-xml-extraction-prompt.txt";
      case CSV_DOCUMENT -> "csv-extraction-prompt.txt";
      case MARKDOWN_DOCUMENT,
              TEXT_DOCUMENT,
              PDF_DOCUMENT,
              WORD_DOCUMENT,
              POWERPOINT_DOCUMENT,
              IMAGE_DOCUMENT,
              GENERIC_DOCUMENT,
              UNKNOWN_DOCUMENT ->
          "enhanced-chunk-extraction-prompt.txt";
    };
  }

  /**
   * Determines if the platform should use multimodal parsing or text-only.
   *
   * @param platform The detected platform
   * @return true if text-only parsing is sufficient
   */
  public boolean isTextOnlyPlatform(XMLPlatform platform) {
    // All XML platforms can be parsed as text
    // Non-XML documents (PDFs, images) need multimodal parsing
    return true;
  }
}
