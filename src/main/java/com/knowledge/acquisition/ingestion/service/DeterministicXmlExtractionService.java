package com.knowledge.acquisition.ingestion.service;

import java.io.StringReader;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import com.knowledge.acquisition.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * Extracts high-confidence knowledge directly from XML structure before AI enrichment.
 *
 * <p>The extractor recognizes generic XML patterns as well as platform-specific hints supplied by
 * {@link XMLPlatformDetector}. Its output is intentionally conservative: it captures obvious
 * components, rules, workflows, data models, and relationships without depending on a model call,
 * then lets the AI extraction layer enrich the same document later.
 */
@Service
@Slf4j
public class DeterministicXmlExtractionService {
  private static final int MAX_ITEMS_PER_TYPE = 200;

  private final XMLPlatformDetector platformDetector;

  public DeterministicXmlExtractionService(XMLPlatformDetector platformDetector) {
    this.platformDetector = platformDetector;
  }

  /**
   * Parses XML and returns deterministic graph candidates for the source object.
   *
   * @param xmlContent decoded raw XML content
   * @param objectName source object name or path used for fallbacks and logging
   * @return extraction result populated from XML structure, or a partial result when parsing fails
   */
  public KnowledgeExtractionResult extract(String xmlContent, String objectName) {
    KnowledgeExtractionResult result = new KnowledgeExtractionResult();
    if (xmlContent == null || xmlContent.isBlank()) {
      return result;
    }

    try {
      Document document = parse(xmlContent);
      Element root = document.getDocumentElement();
      XMLPlatformDetector.XMLPlatform platform =
          platformDetector.detectPlatform(xmlContent, objectName);

      result.setArchitecturalSummary(summary(platform, root, objectName));
      result.setTechnicalComponents(extractTechnicalComponents(root, objectName, platform));
      result.setDataModels(extractDataModels(root, platform));
      result.setBusinessRules(extractRules(root, objectName, platform));
      result.setBusinessFlows(extractWorkflows(root, objectName, platform));
      result.setRelationships(extractRelationships(root, platform));
      result.setMigrationNotes(
          List.of(
              "Deterministic XML extraction used platform "
                  + platform.name()
                  + " before AI enrichment."));
      return result;
    } catch (Exception e) {
      log.warn("Deterministic XML extraction failed for {}", objectName, e);
      result.setArchitecturalSummary("XML parsed by AI enrichment only: " + shortMessage(e));
      return result;
    }
  }

  /**
   * Builds a DOM document with external entities disabled.
   *
   * <p>Using {@link StringReader} preserves the caller's decoded text and avoids the parser
   * reinterpreting the source bytes with the wrong character set.
   */
  private Document parse(String xmlContent) throws Exception {
    String sanitizedXml = sanitizeXml(xmlContent);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(sanitizedXml)));
  }

  /** Removes leading noise such as BOMs or transport prefixes before DOM parsing. */
  private String sanitizeXml(String xmlContent) {
    String value = xmlContent == null ? "" : xmlContent.stripLeading();
    if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
      value = value.substring(1).stripLeading();
    }
    int firstXmlChar = value.indexOf('<');
    if (firstXmlChar > 0) {
      value = value.substring(firstXmlChar);
    }
    return value;
  }

  private List<TechnicalComponent> extractTechnicalComponents(
      Element root, String objectName, XMLPlatformDetector.XMLPlatform platform) {
    List<TechnicalComponent> components = new ArrayList<>();
    TechnicalComponent source = new TechnicalComponent();
    source.setComponentName(fileName(objectName));
    source.setComponentType(rootName(root));
    source.setTechnology(platform.name());
    source.setResponsibility("XML artifact parsed from " + rootName(root));
    source.setConfidence(0.95);
    components.add(source);

    for (Element element : elements(root)) {
      String name = localName(element);
      if (!isComponentElement(name) || components.size() >= MAX_ITEMS_PER_TYPE) {
        continue;
      }
      String componentName = firstAttr(element, "name", "id", "Name", "ID", "class", "type");
      if (isBlank(componentName)) {
        continue;
      }
      TechnicalComponent component = new TechnicalComponent();
      component.setComponentName(componentName);
      component.setComponentType(name);
      component.setTechnology(platform.name());
      component.setResponsibility(textFromChildren(element, "Description", "description"));
      component.setConfidence(0.9);
      components.add(component);
    }
    return dedupeComponents(components);
  }

  private List<KnowledgeDataModel> extractDataModels(
      Element root, XMLPlatformDetector.XMLPlatform platform) {
    List<KnowledgeDataModel> models = new ArrayList<>();
    for (Element element : elements(root)) {
      String name = localName(element);
      if (!isDataModelElement(name) || models.size() >= MAX_ITEMS_PER_TYPE) {
        continue;
      }
      String modelName = firstAttr(element, "name", "id", "Name", "ID", "displayName");
      if (isBlank(modelName)) {
        continue;
      }
      models.add(
          KnowledgeDataModel.builder()
              .modelName(modelName)
              .modelType(name)
              .description(textFromChildren(element, "Description", "description", "BusinessName"))
              .schemaDefinition(schema(element, platform))
              .fields(extractFields(element))
              .build());
    }
    return dedupeModels(models);
  }

  private List<KnowledgeDataField> extractFields(Element parent) {
    List<KnowledgeDataField> fields = new ArrayList<>();
    for (Element child : childElements(parent)) {
      String name = localName(child);
      if (!isFieldElement(name) || fields.size() >= MAX_ITEMS_PER_TYPE) {
        continue;
      }
      String fieldName = firstAttr(child, "name", "id", "Name", "ID", "column", "attributeName");
      if (isBlank(fieldName)) {
        continue;
      }
      fields.add(
          KnowledgeDataField.builder()
              .fieldName(fieldName)
              .fieldType(firstAttr(child, "type", "dataType", "DataType", "datatype"))
              .description(textFromChildren(child, "Description", "description", "BusinessName"))
              .isRequired(booleanAttr(child, "required", "mandatory", "nullable"))
              .constraints(attributes(child))
              .build());
    }
    return fields;
  }

  private List<BusinessRule> extractRules(
      Element root, String objectName, XMLPlatformDetector.XMLPlatform platform) {
    List<BusinessRule> rules = new ArrayList<>();
    for (Element element : elements(root)) {
      String name = localName(element);
      if (!isRuleElement(name) || rules.size() >= MAX_ITEMS_PER_TYPE) {
        continue;
      }
      String ruleName = firstAttr(element, "name", "id", "Name", "ID", "ruleName");
      if (isBlank(ruleName)) {
        ruleName = name + " in " + fileName(objectName);
      }
      BusinessRule rule = new BusinessRule();
      rule.setRuleName(ruleName);
      rule.setRuleType(ruleType(name));
      rule.setCondition(
          firstNonBlank(textFromChildren(element, "Condition", "Expression"), text(element)));
      rule.setOutcome(textFromChildren(element, "Outcome", "ErrorMessage", "Description"));
      rule.setPriority(firstAttr(element, "priority", "Priority", "severity"));
      rule.setTechnicalImplementation(
          "Extracted from XML element <" + name + "> with attributes " + attributes(element));
      rule.setValidationCriteria(rule.getOutcome());
      rule.setConfidence(0.9);
      rules.add(rule);
    }

    if (rules.isEmpty() && platform == XMLPlatformDetector.XMLPlatform.TIBCO_MDM) {
      BusinessRule rule = new BusinessRule();
      rule.setRuleName(fileName(objectName));
      rule.setRuleType("workflow");
      rule.setCondition("TIBCO MDM rulebase XML artifact");
      rule.setOutcome("Rulebase behavior is defined by XML configuration");
      rule.setTechnicalImplementation("Extracted from TIBCO MDM XML file " + objectName);
      rule.setConfidence(0.75);
      rules.add(rule);
    }
    return dedupeRules(rules);
  }

  private List<BusinessFlow> extractWorkflows(
      Element root, String objectName, XMLPlatformDetector.XMLPlatform platform) {
    List<BusinessFlow> flows = new ArrayList<>();
    for (Element element : elements(root)) {
      String name = localName(element);
      if (!isWorkflowElement(name) || flows.size() >= MAX_ITEMS_PER_TYPE) {
        continue;
      }
      String flowName = firstAttr(element, "name", "id", "Name", "ID", "processName");
      if (isBlank(flowName)) {
        flowName = fileName(objectName);
      }
      BusinessFlow flow = new BusinessFlow();
      flow.setFlowName(flowName);
      flow.setTrigger(firstAttr(element, "trigger", "event", "start"));
      flow.setOutcome(textFromChildren(element, "Outcome", "Description", "description"));
      flow.setOwner(firstAttr(element, "owner", "role", "actor"));
      flow.setSteps(extractWorkflowSteps(element));
      flow.setConfidence(0.9);
      flows.add(flow);
    }

    if (flows.isEmpty()
        && platform == XMLPlatformDetector.XMLPlatform.TIBCO_MDM
        && fileName(objectName).toLowerCase().startsWith("wf")) {
      BusinessFlow flow = new BusinessFlow();
      flow.setFlowName(fileName(objectName));
      flow.setOutcome("TIBCO MDM workflow configuration");
      flow.setSteps(extractWorkflowSteps(root));
      flow.setConfidence(0.75);
      flows.add(flow);
    }
    return dedupeFlows(flows);
  }

  private List<BusinessFlowStep> extractWorkflowSteps(Element workflow) {
    List<BusinessFlowStep> steps = new ArrayList<>();
    int sequence = 1;
    for (Element element : elements(workflow)) {
      String name = localName(element);
      if (!isStepElement(name) || steps.size() >= MAX_ITEMS_PER_TYPE) {
        continue;
      }
      BusinessFlowStep step = new BusinessFlowStep();
      step.setSequence(sequence++);
      step.setStepName(firstNonBlank(firstAttr(element, "name", "id", "Name", "ID"), name));
      step.setActor(firstAttr(element, "actor", "role", "participant", "performer"));
      step.setAction(
          firstNonBlank(firstAttr(element, "action", "type"), textFromChildren(element, "Action")));
      step.setInputParameters(parameterText(element, "Input", "ActualParameters", "Parameter"));
      step.setOutputParameters(parameterText(element, "Output", "OutputParameters"));
      step.setNextStep(firstAttr(element, "next", "to", "target", "targetRef"));
      step.setTechnicalDetails("Extracted from XML element <" + name + ">");
      steps.add(step);
    }
    return steps;
  }

  private List<KnowledgeRelationship> extractRelationships(
      Element root, XMLPlatformDetector.XMLPlatform platform) {
    List<KnowledgeRelationship> relationships = new ArrayList<>();
    for (Element element : elements(root)) {
      String name = localName(element);
      if (!isRelationshipElement(name) || relationships.size() >= MAX_ITEMS_PER_TYPE) {
        continue;
      }
      String source = firstAttr(element, "source", "from", "sourceRef", "parent", "left");
      String target = firstAttr(element, "target", "to", "targetRef", "child", "right");
      if (isBlank(source) || isBlank(target)) {
        continue;
      }
      KnowledgeRelationship relationship = new KnowledgeRelationship();
      relationship.setSourceName(source);
      relationship.setSourceType("xml_element");
      relationship.setTargetName(target);
      relationship.setTargetType("xml_element");
      relationship.setRelationshipType(firstNonBlank(firstAttr(element, "type", "relation"), name));
      relationship.setContext("Extracted from " + platform.name() + " XML element <" + name + ">");
      relationship.setConfidence(0.9);
      relationships.add(relationship);
    }
    return relationships;
  }

  private String summary(
      XMLPlatformDetector.XMLPlatform platform, Element root, String objectName) {
    return "Deterministic "
        + platform.name()
        + " XML parse of "
        + fileName(objectName)
        + " with root element <"
        + rootName(root)
        + ">.";
  }

  private Map<String, Object> schema(Element element, XMLPlatformDetector.XMLPlatform platform) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("xmlElement", localName(element));
    schema.put("platform", platform.name());
    schema.put("attributes", attributes(element));
    return schema;
  }

  private List<Element> elements(Element root) {
    List<Element> elements = new ArrayList<>();
    NodeList nodes = root.getElementsByTagName("*");
    for (int i = 0; i < nodes.getLength(); i++) {
      if (nodes.item(i) instanceof Element element) {
        elements.add(element);
      }
    }
    return elements;
  }

  private List<Element> childElements(Element parent) {
    List<Element> children = new ArrayList<>();
    NodeList nodes = parent.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      if (nodes.item(i) instanceof Element element) {
        children.add(element);
      }
    }
    return children;
  }

  private String localName(Element element) {
    String localName = element.getLocalName();
    return localName == null ? element.getTagName() : localName;
  }

  private String rootName(Element root) {
    return localName(root);
  }

  private String fileName(String objectName) {
    if (objectName == null || objectName.isBlank()) {
      return "xml-document";
    }
    return objectName.substring(objectName.lastIndexOf('/') + 1);
  }

  private String firstAttr(Element element, String... names) {
    for (String name : names) {
      if (element.hasAttribute(name) && !element.getAttribute(name).isBlank()) {
        return element.getAttribute(name).strip();
      }
    }
    return null;
  }

  private Boolean booleanAttr(Element element, String... names) {
    String value = firstAttr(element, names);
    if (isBlank(value)) {
      return null;
    }
    if ("nullable".equalsIgnoreCase(names[names.length - 1])) {
      return !Boolean.parseBoolean(value);
    }
    return Boolean.parseBoolean(value) || "Y".equalsIgnoreCase(value) || "1".equals(value);
  }

  private Map<String, Object> attributes(Element element) {
    Map<String, Object> values = new LinkedHashMap<>();
    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node attribute = attributes.item(i);
      values.put(attribute.getNodeName(), attribute.getNodeValue());
    }
    return values;
  }

  private String textFromChildren(Element element, String... childNames) {
    for (Element child : childElements(element)) {
      if (containsAny(localName(child), childNames)) {
        String text = text(child);
        if (!isBlank(text)) {
          return text;
        }
      }
    }
    return null;
  }

  private String parameterText(Element element, String... childNames) {
    String text = textFromChildren(element, childNames);
    return isBlank(text) ? null : text;
  }

  private String text(Element element) {
    String text = element.getTextContent();
    if (text == null) {
      return null;
    }
    text = text.replaceAll("\\s+", " ").strip();
    return text.length() <= 1000 ? text : text.substring(0, 1000);
  }

  private String ruleType(String elementName) {
    String lower = elementName.toLowerCase();
    if (lower.contains("validation") || lower.contains("check")) {
      return "validation";
    }
    if (lower.contains("calculate") || lower.contains("calculation")) {
      return "calculation";
    }
    if (lower.contains("workflow")) {
      return "workflow";
    }
    return "policy";
  }

  private boolean isDataModelElement(String name) {
    return containsAny(
        name, "Entity", "Repository", "Table", "Record", "Schema", "Object", "DataModel");
  }

  private boolean isFieldElement(String name) {
    return containsAny(name, "Field", "Attribute", "Column", "Property");
  }

  private boolean isRuleElement(String name) {
    return containsAny(
        name, "Rule", "Rulebase", "Validation", "Condition", "Check", "Constraint", "Policy");
  }

  private boolean isWorkflowElement(String name) {
    return containsAny(
        name, "Workflow", "Process", "ProcessDefinition", "Flow", "WorkflowTemplate");
  }

  private boolean isStepElement(String name) {
    return containsAny(name, "Step", "Activity", "Task", "Transition", "WorkItem", "Action");
  }

  private boolean isRelationshipElement(String name) {
    return containsAny(name, "Relationship", "Relation", "Link", "Association", "Transition");
  }

  private boolean isComponentElement(String name) {
    return containsAny(name, "Service", "Component", "Module", "Activity", "Task", "Process");
  }

  private boolean containsAny(String value, String... tokens) {
    if (value == null) {
      return false;
    }
    String lower = value.toLowerCase();
    for (String token : tokens) {
      if (lower.contains(token.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) {
        return value;
      }
    }
    return null;
  }

  private List<TechnicalComponent> dedupeComponents(List<TechnicalComponent> components) {
    Map<String, TechnicalComponent> deduped = new LinkedHashMap<>();
    for (TechnicalComponent component : components) {
      deduped.putIfAbsent(component.getComponentName(), component);
    }
    return new ArrayList<>(deduped.values());
  }

  private List<KnowledgeDataModel> dedupeModels(List<KnowledgeDataModel> models) {
    Map<String, KnowledgeDataModel> deduped = new LinkedHashMap<>();
    for (KnowledgeDataModel model : models) {
      deduped.putIfAbsent(model.getModelName(), model);
    }
    return new ArrayList<>(deduped.values());
  }

  private List<BusinessRule> dedupeRules(List<BusinessRule> rules) {
    Map<String, BusinessRule> deduped = new LinkedHashMap<>();
    for (BusinessRule rule : rules) {
      deduped.putIfAbsent(rule.getRuleName(), rule);
    }
    return new ArrayList<>(deduped.values());
  }

  private List<BusinessFlow> dedupeFlows(List<BusinessFlow> flows) {
    Map<String, BusinessFlow> deduped = new LinkedHashMap<>();
    for (BusinessFlow flow : flows) {
      deduped.putIfAbsent(flow.getFlowName(), flow);
    }
    return new ArrayList<>(deduped.values());
  }

  private String shortMessage(Exception exception) {
    String message = exception.getMessage();
    return message == null ? exception.getClass().getSimpleName() : message;
  }
}
