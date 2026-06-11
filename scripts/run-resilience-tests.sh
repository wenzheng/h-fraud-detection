#!/usr/bin/env bash

set -euo pipefail

NAMESPACE="${NAMESPACE:-fraud-platform}"
OUTPUT_ROOT="${OUTPUT_ROOT:-resilience-reports}"
BASE_URL="${BASE_URL:-47.99.194.202}"
CSV_FILE="${CSV_FILE:-testtransactions.csv}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-5m}"
POST_TRAFFIC_DELAY_SECONDS="${POST_TRAFFIC_DELAY_SECONDS:-10}"
SCENARIOS="${SCENARIOS:-transaction-api-rollout fraud-processor-rollout alert-handler-rollout fraud-processor-scale transaction-api-pod-delete}"
CONFIRM="${CONFIRM:-no}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/run-resilience-tests.sh

Environment variables:
  NAMESPACE                   Kubernetes namespace. Default: fraud-platform
  OUTPUT_ROOT                 Report output directory root. Default: resilience-reports
  BASE_URL                    Public HTTP base URL for transaction-api. Optional.
  CSV_FILE                    CSV file used for traffic generation. Default: testtransactions.csv
  WAIT_TIMEOUT                Rollout wait timeout. Default: 5m
  POST_TRAFFIC_DELAY_SECONDS  Seconds to wait after sending traffic. Default: 10
  SCENARIOS                   Space-separated scenario list
  CONFIRM                     Must be 'yes' to execute destructive scenarios

Supported scenarios:
  transaction-api-rollout
  fraud-processor-rollout
  alert-handler-rollout
  fraud-processor-scale
  transaction-api-pod-delete

Example:
  CONFIRM=yes BASE_URL=http://47.99.194.202 ./scripts/run-resilience-tests.sh
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ "${CONFIRM}" != "yes" ]]; then
  echo "Refusing to run resilience scenarios unless CONFIRM=yes is set." >&2
  echo "This script performs deployment restarts, scaling, and pod deletion." >&2
  exit 1
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl is required but not found in PATH." >&2
  exit 1
fi

TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
REPORT_DIR="${OUTPUT_ROOT}/${TIMESTAMP}"
mkdir -p "${REPORT_DIR}"

SUMMARY_FILE="${REPORT_DIR}/summary.md"
RUN_LOG="${REPORT_DIR}/run.log"

log() {
  local message="$1"
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "${message}" | tee -a "${RUN_LOG}"
}

run_and_capture() {
  local label="$1"
  shift
  local outfile="${REPORT_DIR}/${label}.txt"
  {
    echo "\$ $*"
    "$@"
  } >"${outfile}" 2>&1
}

capture_cluster_state() {
  local prefix="$1"
  run_and_capture "${prefix}-pods" kubectl -n "${NAMESPACE}" get pods -o wide
  run_and_capture "${prefix}-services" kubectl -n "${NAMESPACE}" get svc
  run_and_capture "${prefix}-events" kubectl -n "${NAMESPACE}" get events --sort-by=.metadata.creationTimestamp
}

capture_app_logs() {
  local prefix="$1"
  run_and_capture "${prefix}-transaction-api-logs" kubectl -n "${NAMESPACE}" logs -l app=transaction-api --tail=100
  run_and_capture "${prefix}-fraud-processor-logs" kubectl -n "${NAMESPACE}" logs -l app=fraud-processor --tail=100
  run_and_capture "${prefix}-alert-handler-logs" kubectl -n "${NAMESPACE}" logs -l app=alert-handler --tail=100
}

send_traffic_if_configured() {
  local prefix="$1"
  if [[ -z "${BASE_URL}" ]]; then
    log "Skipping traffic generation because BASE_URL is not set."
    return
  fi
  if [[ ! -f "${CSV_FILE}" ]]; then
    log "Skipping traffic generation because CSV_FILE does not exist: ${CSV_FILE}"
    return
  fi

  local outfile="${REPORT_DIR}/${prefix}-traffic.txt"
  {
    echo "\$ BASE_URL=${BASE_URL} ./scripts/send-transactions.sh ${CSV_FILE}"
    BASE_URL="${BASE_URL}" ./scripts/send-transactions.sh "${CSV_FILE}"
  } >"${outfile}" 2>&1
  sleep "${POST_TRAFFIC_DELAY_SECONDS}"
}

append_summary() {
  local title="$1"
  local expected="$2"
  local actual="$3"
  local evidence_prefix="$4"

  {
    echo "## ${title}"
    echo
    echo "- Expected: ${expected}"
    echo "- Actual: ${actual}"
    echo "- Evidence:"
    echo "  - ${evidence_prefix}-pods.txt"
    echo "  - ${evidence_prefix}-services.txt"
    echo "  - ${evidence_prefix}-events.txt"
    echo "  - ${evidence_prefix}-transaction-api-logs.txt"
    echo "  - ${evidence_prefix}-fraud-processor-logs.txt"
    echo "  - ${evidence_prefix}-alert-handler-logs.txt"
    if [[ -f "${REPORT_DIR}/${evidence_prefix}-traffic.txt" ]]; then
      echo "  - ${evidence_prefix}-traffic.txt"
    fi
    echo
  } >> "${SUMMARY_FILE}"
}

scenario_transaction_api_rollout() {
  local prefix="transaction-api-rollout"
  log "Running scenario: ${prefix}"
  capture_cluster_state "${prefix}-before"
  send_traffic_if_configured "${prefix}-before"
  run_and_capture "${prefix}-restart" kubectl -n "${NAMESPACE}" rollout restart deployment/transaction-api
  run_and_capture "${prefix}-status" kubectl -n "${NAMESPACE}" rollout status deployment/transaction-api --timeout="${WAIT_TIMEOUT}"
  capture_cluster_state "${prefix}-after"
  capture_app_logs "${prefix}-after"
  append_summary \
    "Scenario: Restart transaction-api during ingress" \
    "transaction-api should roll out successfully and continue accepting requests with minimal interruption." \
    "See rollout output and traffic evidence files." \
    "${prefix}-after"
}

scenario_fraud_processor_rollout() {
  local prefix="fraud-processor-rollout"
  log "Running scenario: ${prefix}"
  capture_cluster_state "${prefix}-before"
  send_traffic_if_configured "${prefix}-before"
  run_and_capture "${prefix}-restart" kubectl -n "${NAMESPACE}" rollout restart deployment/fraud-processor
  run_and_capture "${prefix}-status" kubectl -n "${NAMESPACE}" rollout status deployment/fraud-processor --timeout="${WAIT_TIMEOUT}"
  capture_cluster_state "${prefix}-after"
  capture_app_logs "${prefix}-after"
  append_summary \
    "Scenario: Restart fraud-processor during queue consumption" \
    "fraud-processor should recover and continue consuming queued transactions after restart." \
    "See rollout output, logs, and post-restart traffic evidence." \
    "${prefix}-after"
}

scenario_alert_handler_rollout() {
  local prefix="alert-handler-rollout"
  log "Running scenario: ${prefix}"
  capture_cluster_state "${prefix}-before"
  send_traffic_if_configured "${prefix}-before"
  run_and_capture "${prefix}-restart" kubectl -n "${NAMESPACE}" rollout restart deployment/alert-handler
  run_and_capture "${prefix}-status" kubectl -n "${NAMESPACE}" rollout status deployment/alert-handler --timeout="${WAIT_TIMEOUT}"
  capture_cluster_state "${prefix}-after"
  capture_app_logs "${prefix}-after"
  append_summary \
    "Scenario: Restart alert-handler during alert processing" \
    "alert-handler should recover and continue consuming alert messages after restart." \
    "See rollout output and alert-handler log evidence." \
    "${prefix}-after"
}

scenario_fraud_processor_scale() {
  local prefix="fraud-processor-scale"
  log "Running scenario: ${prefix}"
  capture_cluster_state "${prefix}-before"
  run_and_capture "${prefix}-scale-down" kubectl -n "${NAMESPACE}" scale deployment/fraud-processor --replicas=1
  run_and_capture "${prefix}-scale-down-status" kubectl -n "${NAMESPACE}" rollout status deployment/fraud-processor --timeout="${WAIT_TIMEOUT}"
  send_traffic_if_configured "${prefix}-scaled-down"
  run_and_capture "${prefix}-scale-up" kubectl -n "${NAMESPACE}" scale deployment/fraud-processor --replicas=2
  run_and_capture "${prefix}-scale-up-status" kubectl -n "${NAMESPACE}" rollout status deployment/fraud-processor --timeout="${WAIT_TIMEOUT}"
  capture_cluster_state "${prefix}-after"
  capture_app_logs "${prefix}-after"
  append_summary \
    "Scenario: Scale fraud-processor down and back up" \
    "message processing should continue when replicas are reduced, and throughput headroom should recover after scaling back up." \
    "See scale command outputs and traffic evidence." \
    "${prefix}-after"
}

scenario_transaction_api_pod_delete() {
  local prefix="transaction-api-pod-delete"
  log "Running scenario: ${prefix}"
  capture_cluster_state "${prefix}-before"
  local pod_name
  pod_name="$(kubectl -n "${NAMESPACE}" get pods -l app=transaction-api -o jsonpath='{.items[0].metadata.name}')"
  if [[ -z "${pod_name}" ]]; then
    echo "No transaction-api pod found" > "${REPORT_DIR}/${prefix}-delete.txt"
    return 1
  fi
  run_and_capture "${prefix}-delete" kubectl -n "${NAMESPACE}" delete pod "${pod_name}"
  run_and_capture "${prefix}-wait" kubectl -n "${NAMESPACE}" rollout status deployment/transaction-api --timeout="${WAIT_TIMEOUT}"
  capture_cluster_state "${prefix}-after"
  capture_app_logs "${prefix}-after"
  append_summary \
    "Scenario: Delete a single transaction-api pod" \
    "Kubernetes should recreate the deleted pod and restore readiness automatically." \
    "See delete output, rollout status, and post-recovery logs." \
    "${prefix}-after"
}

{
  echo "# Resilience Test Report"
  echo
  echo "- Namespace: ${NAMESPACE}"
  echo "- Run timestamp: ${TIMESTAMP}"
  echo "- Scenarios: ${SCENARIOS}"
  echo "- Traffic source: ${BASE_URL:-not configured}"
  echo
} > "${SUMMARY_FILE}"

for scenario in ${SCENARIOS}; do
  case "${scenario}" in
    transaction-api-rollout)
      scenario_transaction_api_rollout
      ;;
    fraud-processor-rollout)
      scenario_fraud_processor_rollout
      ;;
    alert-handler-rollout)
      scenario_alert_handler_rollout
      ;;
    fraud-processor-scale)
      scenario_fraud_processor_scale
      ;;
    transaction-api-pod-delete)
      scenario_transaction_api_pod_delete
      ;;
    *)
      echo "Unknown scenario: ${scenario}" >&2
      exit 1
      ;;
  esac
done

log "Resilience test artifacts written to ${REPORT_DIR}"
echo "Summary: ${SUMMARY_FILE}"
