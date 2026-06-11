# Resilience Test Report

- Namespace: fraud-platform
- Run timestamp: 20260611-123516
- Scenarios: transaction-api-rollout fraud-processor-rollout alert-handler-rollout fraud-processor-scale transaction-api-pod-delete
- Traffic source: 47.99.194.202

## Scenario: Restart transaction-api during ingress

- Expected: transaction-api should roll out successfully and continue accepting requests with minimal interruption.
- Actual: See rollout output and traffic evidence files.
- Evidence:
  - transaction-api-rollout-after-pods.txt
  - transaction-api-rollout-after-services.txt
  - transaction-api-rollout-after-events.txt
  - transaction-api-rollout-after-transaction-api-logs.txt
  - transaction-api-rollout-after-fraud-processor-logs.txt
  - transaction-api-rollout-after-alert-handler-logs.txt

## Scenario: Restart fraud-processor during queue consumption

- Expected: fraud-processor should recover and continue consuming queued transactions after restart.
- Actual: See rollout output, logs, and post-restart traffic evidence.
- Evidence:
  - fraud-processor-rollout-after-pods.txt
  - fraud-processor-rollout-after-services.txt
  - fraud-processor-rollout-after-events.txt
  - fraud-processor-rollout-after-transaction-api-logs.txt
  - fraud-processor-rollout-after-fraud-processor-logs.txt
  - fraud-processor-rollout-after-alert-handler-logs.txt

## Scenario: Restart alert-handler during alert processing

- Expected: alert-handler should recover and continue consuming alert messages after restart.
- Actual: See rollout output and alert-handler log evidence.
- Evidence:
  - alert-handler-rollout-after-pods.txt
  - alert-handler-rollout-after-services.txt
  - alert-handler-rollout-after-events.txt
  - alert-handler-rollout-after-transaction-api-logs.txt
  - alert-handler-rollout-after-fraud-processor-logs.txt
  - alert-handler-rollout-after-alert-handler-logs.txt

## Scenario: Scale fraud-processor down and back up

- Expected: message processing should continue when replicas are reduced, and throughput headroom should recover after scaling back up.
- Actual: See scale command outputs and traffic evidence.
- Evidence:
  - fraud-processor-scale-after-pods.txt
  - fraud-processor-scale-after-services.txt
  - fraud-processor-scale-after-events.txt
  - fraud-processor-scale-after-transaction-api-logs.txt
  - fraud-processor-scale-after-fraud-processor-logs.txt
  - fraud-processor-scale-after-alert-handler-logs.txt

## Scenario: Delete a single transaction-api pod

- Expected: Kubernetes should recreate the deleted pod and restore readiness automatically.
- Actual: See delete output, rollout status, and post-recovery logs.
- Evidence:
  - transaction-api-pod-delete-after-pods.txt
  - transaction-api-pod-delete-after-services.txt
  - transaction-api-pod-delete-after-events.txt
  - transaction-api-pod-delete-after-transaction-api-logs.txt
  - transaction-api-pod-delete-after-fraud-processor-logs.txt
  - transaction-api-pod-delete-after-alert-handler-logs.txt

