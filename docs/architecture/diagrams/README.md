# Toba architecture diagrams

These Mermaid diagrams describe **target architecture**, not proof of implemented features. See source, tests and [ROADMAP](../../../ROADMAP.md) for validation status.

| Diagram | Question answered |
|---|---|
| [system-context.md](system-context.md) | Where does Toba sit relative to Farabi, Numu, engines and storage? |
| [control-data-plane.md](control-data-plane.md) | Which components own policy and which execute data operations? |
| [hexagonal-boundaries.md](hexagonal-boundaries.md) | What dependency direction must code follow? |
| [governed-query.md](governed-query.md) | How is a query authorized, submitted and reconciled? |
| [catalog-authorization.md](catalog-authorization.md) | How are canonical actor and grants established? |
| [agentic-operations.md](agentic-operations.md) | How are agent actions governed and approved? |
| [job-lifecycle.md](job-lifecycle.md) | What are safe query job transitions? |
| [deployment.md](deployment.md) | What is a possible production topology? |

**Diagram conventions:** Solid arrows indicate required logical interactions. Dashed arrows indicate optional integrations. Each diagram's caption distinguishes target behavior from current implementation. Update diagrams with relevant contract changes; do not imply deployment verification.
