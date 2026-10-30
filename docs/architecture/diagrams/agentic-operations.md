# Governed agent operations — target

```mermaid
flowchart TD
  Intent["User / Operator Intent"] --> Planner["Agent Planner — Untrusted Proposal"]
  Planner --> Typed["Typed Allowlisted Tool Contract"]
  Typed --> Auth["Verified Delegation + Tenant Scope"]
  Auth --> Policy{"Policy and Budget Permit?"}
  Policy -->|No| Deny["Reject and Audit"]
  Policy -->|Yes| Risk{"High-Risk Mutation?"}
  Risk -->|Yes| Approval{"Human Approval?"}
  Approval -->|No| Deny
  Approval -->|Yes| Preflight["Preflight / Dry Run"]
  Risk -->|No| Preflight
  Preflight --> Execute["Idempotent Governed Execution"]
  Execute --> Observe["Audit / Telemetry / Result"]
  Observe --> Review["Review Outcome / Compensation if Needed"]
```

**Rule:** Prompts and retrieved data cannot grant privileges. Destructive operations require explicit policy and approval gates. **Status:** target workflow; not an assertion of deployed autonomous operations.
