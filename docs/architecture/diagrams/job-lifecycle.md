# Toba query job lifecycle — proposed invariant model

```mermaid
stateDiagram-v2
  [*] --> CREATED
  CREATED --> AUTHORIZED: policy checks pass
  CREATED --> REJECTED: denied
  AUTHORIZED --> QUEUED: durable admission
  QUEUED --> SUBMITTING: fenced worker lease
  SUBMITTING --> RUNNING: confirmed provider job
  SUBMITTING --> UNCERTAIN: ambiguous timeout
  UNCERTAIN --> RUNNING: reconciled active job
  UNCERTAIN --> FAILED: proven failed / expired
  RUNNING --> SUCCEEDED: results committed
  RUNNING --> FAILED: terminal provider failure
  QUEUED --> CANCELLED: authorized cancellation
  RUNNING --> CANCELLING: authorized cancellation
  CANCELLING --> CANCELLED: confirmed cancellation
  CANCELLING --> SUCCEEDED: completed before cancellation
  REJECTED --> [*]
  FAILED --> [*]
  SUCCEEDED --> [*]
  CANCELLED --> [*]
```

**Invariant:** `UNCERTAIN` must not trigger blind duplicate submission. Terminal transitions require idempotency/fencing and audit. **Status:** reference state model; reconcile with actual persistence schema before implementation.
