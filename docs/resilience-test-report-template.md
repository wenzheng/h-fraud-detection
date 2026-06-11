# Resilience Test Report Template

Use this template after running:

```bash
CONFIRM=yes BASE_URL=http://<external-ip>/ ./scripts/run-resilience-tests.sh
```

## Environment

- Date:
- Cluster:
- Namespace:
- Region:
- Kubernetes version:
- Images under test:
  - transaction-api:
  - fraud-processor:
  - alert-handler:

## Test Scope

- Objective:
- Expected resilience behavior:
- Input traffic method:
  - HTTP:
  - TCP:
- Relevant queues:
  - transaction queue:
  - alert queue:

## Scenario 1: Restart transaction-api during ingress

- Command(s):
- Expected result:
- Actual result:
- Duration:
- Impact observed:
- Evidence files:

## Scenario 2: Restart fraud-processor during queue consumption

- Command(s):
- Expected result:
- Actual result:
- Duration:
- Impact observed:
- Evidence files:

## Scenario 3: Restart alert-handler during alert processing

- Command(s):
- Expected result:
- Actual result:
- Duration:
- Impact observed:
- Evidence files:

## Scenario 4: Scale fraud-processor down and back up

- Command(s):
- Expected result:
- Actual result:
- Duration:
- Impact observed:
- Evidence files:

## Scenario 5: Delete a single transaction-api pod

- Command(s):
- Expected result:
- Actual result:
- Duration:
- Impact observed:
- Evidence files:

## Metrics Summary

- `fraud_ingress_transactions_total` before:
- `fraud_ingress_transactions_total` after:
- `fraud_data_points_processed_total` before:
- `fraud_data_points_processed_total` after:
- `fraud_alerts_handled_total` before:
- `fraud_alerts_handled_total` after:

## Findings

- What recovered automatically:
- What required manual intervention:
- Message loss observed:
- Alert loss observed:
- Latency impact observed:

## Risks and Follow-Up Actions

- Remaining risks:
- Recommended improvements:
- Next resilience scenarios to add:
