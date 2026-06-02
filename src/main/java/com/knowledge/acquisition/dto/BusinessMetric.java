package com.knowledge.acquisition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for business metrics and KPIs (Key Performance Indicators) extracted from enterprise
 * documentation.
 *
 * <p>Represents quantifiable measures used to evaluate the success, performance, or effectiveness
 * of business capabilities, workflows, processes, or components. Metrics provide objective data for
 * decision-making, performance monitoring, and continuous improvement.
 *
 * <p>Understanding metrics is essential for:
 *
 * <ul>
 *   <li>Performance monitoring and dashboards
 *   <li>SLA (Service Level Agreement) tracking
 *   <li>Business intelligence and analytics
 *   <li>Capacity planning and resource optimization
 *   <li>Identifying bottlenecks and improvement opportunities
 *   <li>Executive reporting and KPI dashboards
 *   <li>Goal setting and progress tracking
 * </ul>
 *
 * <p>Metrics can be leading indicators (predictive of future performance) or lagging indicators
 * (reporting past performance). They should be SMART: Specific, Measurable, Achievable, Relevant,
 * and Time-bound.
 *
 * <p>Examples: Order Processing Time, Customer Satisfaction Score (CSAT), Monthly Recurring Revenue
 * (MRR), System Uptime Percentage, Average Response Time, Defect Rate, Conversion Rate, Customer
 * Churn Rate, Net Promoter Score (NPS).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessMetric {
  /**
   * Name of the metric as extracted from the document.
   *
   * <p>Should be clear and specific. Examples: "Order Processing Time", "Customer Satisfaction
   * Score", "System Uptime", "Conversion Rate".
   */
  private String metricName;

  /**
   * Category classification for this metric.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>performance</b> - System or process performance metric (latency, throughput, uptime)
   *   <li><b>quality</b> - Quality or defect metric (error rate, defect density, test coverage)
   *   <li><b>financial</b> - Financial or cost metric (revenue, cost, ROI, burn rate)
   *   <li><b>operational</b> - Operational efficiency metric (cycle time, utilization,
   *       productivity)
   *   <li><b>customer</b> - Customer satisfaction or engagement metric (CSAT, NPS, churn,
   *       retention)
   * </ul>
   */
  private String metricType;

  /**
   * Detailed description of what this metric measures and why it matters.
   *
   * <p>Should explain what aspect of the business or system this metric quantifies and its business
   * significance. Example: "Measures the average time from order placement to order fulfillment
   * completion, indicating operational efficiency."
   */
  private String description;

  /**
   * Formula or method used to calculate this metric.
   *
   * <p>Should include the exact calculation if available. Examples: "Sum of order processing times
   * / Number of orders", "(New Customers - Churned Customers) / Total Customers * 100", "Uptime
   * Hours / Total Hours * 100".
   */
  private String calculationMethod;

  /**
   * Target or goal value for this metric.
   *
   * <p>The desired or acceptable value, often part of SLAs or business objectives. Examples: "95%
   * uptime", "< 2 seconds average response time", "> 80 NPS score", "$1M monthly revenue".
   */
  private String targetValue;

  /**
   * Workflow or process that measures or produces this metric.
   *
   * <p>Should reference workflow names from document-level or chunk-level analysis for linking.
   * Example: "Order Fulfillment Workflow" for "Order Processing Time" metric.
   */
  private String measuredByWorkflow;

  /**
   * Technical component or system that measures or reports this metric.
   *
   * <p>Should reference component names from technical components or solution architecture.
   * Example: "Monitoring Service", "Analytics Dashboard", "Order Management System".
   */
  private String measuredByComponent;

  /**
   * Business capability this metric evaluates or monitors.
   *
   * <p>Should reference capability names from document-level analysis for linking. Example: "Order
   * Management" capability measured by "Order Processing Time" and "Order Accuracy Rate" metrics.
   */
  private String businessCapability;

  /**
   * AI extraction confidence score.
   *
   * <p>Range: 0.0 to 1.0, where:
   *
   * <ul>
   *   <li>0.9-1.0 = High confidence (clearly defined with calculation method AND aligns with
   *       document context)
   *   <li>0.7-0.89 = Medium confidence (clearly mentioned but calculation not specified)
   *   <li>0.5-0.69 = Low confidence (implied or partially described)
   * </ul>
   */
  private Double confidence;
}
