#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ENDPOINT="${ENDPOINT:-$BASE_URL/api/v1/transactions}"
INPUT_FILE="${1:-}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/send-transactions.sh <csv-file>

CSV format:
  accountId,merchantId,deviceId,ipAddress,currency,amount,occurredAt

Notes:
  - `occurredAt` is optional. If empty, the API will use the current time.
  - Blank lines and lines starting with `#` are ignored.
  - A header row starting with `accountId` is ignored.

Examples:
  ./scripts/send-transactions.sh transactions.csv
  BASE_URL=http://localhost:8080 ./scripts/send-transactions.sh transactions.csv
EOF
}

if [[ -z "${INPUT_FILE}" || "${INPUT_FILE}" == "-h" || "${INPUT_FILE}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ ! -f "${INPUT_FILE}" ]]; then
  echo "Input file not found: ${INPUT_FILE}" >&2
  exit 1
fi

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "${value}"
}

build_payload() {
  local account_id="$1"
  local merchant_id="$2"
  local device_id="$3"
  local ip_address="$4"
  local currency="$5"
  local amount="$6"
  local occurred_at="$7"

  if [[ -n "${occurred_at}" ]]; then
    cat <<EOF
{
  "accountId": "${account_id}",
  "merchantId": "${merchant_id}",
  "deviceId": "${device_id}",
  "ipAddress": "${ip_address}",
  "currency": "${currency}",
  "amount": ${amount},
  "occurredAt": "${occurred_at}"
}
EOF
  else
    cat <<EOF
{
  "accountId": "${account_id}",
  "merchantId": "${merchant_id}",
  "deviceId": "${device_id}",
  "ipAddress": "${ip_address}",
  "currency": "${currency}",
  "amount": ${amount}
}
EOF
  fi
}

submit_transaction() {
  local line_number="$1"
  local payload="$2"

  local response
  response="$(curl -sS -w '\n%{http_code}' \
    -X POST "${ENDPOINT}" \
    -H 'Content-Type: application/json' \
    -d "${payload}")"

  local status
  status="$(printf '%s\n' "${response}" | tail -n 1)"
  local body
  body="$(printf '%s\n' "${response}" | sed '$d')"

  echo "line=${line_number} status=${status}"
  echo "${body}"
  echo
}

line_number=0

while IFS= read -r raw_line || [[ -n "${raw_line}" ]]; do
  line_number=$((line_number + 1))

  line="$(trim "${raw_line}")"
  if [[ -z "${line}" || "${line}" == \#* ]]; then
    continue
  fi

  IFS=',' read -r account_id merchant_id device_id ip_address currency amount occurred_at extra <<< "${line}"

  account_id="$(trim "${account_id:-}")"
  merchant_id="$(trim "${merchant_id:-}")"
  device_id="$(trim "${device_id:-}")"
  ip_address="$(trim "${ip_address:-}")"
  currency="$(trim "${currency:-}")"
  amount="$(trim "${amount:-}")"
  occurred_at="$(trim "${occurred_at:-}")"
  extra="$(trim "${extra:-}")"

  if [[ "${account_id}" == "accountId" ]]; then
    continue
  fi

  if [[ -n "${extra}" ]]; then
    echo "Invalid CSV on line ${line_number}: too many columns" >&2
    exit 1
  fi

  if [[ -z "${account_id}" || -z "${merchant_id}" || -z "${device_id}" || -z "${ip_address}" || -z "${currency}" || -z "${amount}" ]]; then
    echo "Invalid CSV on line ${line_number}: expected accountId,merchantId,deviceId,ipAddress,currency,amount[,occurredAt]" >&2
    exit 1
  fi

  payload="$(build_payload "${account_id}" "${merchant_id}" "${device_id}" "${ip_address}" "${currency}" "${amount}" "${occurred_at}")"
  submit_transaction "${line_number}" "${payload}"
done < "${INPUT_FILE}"
