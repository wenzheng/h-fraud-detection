#!/usr/bin/env bash

set -euo pipefail

TCP_HOST="${TCP_HOST:-127.0.0.1}"
TCP_PORT="${TCP_PORT:-8000}"
INPUT_FILE="${1:-}"
request_count=0
latency_sum_ms=0
latency_min_ms=""
latency_max_ms=0

usage() {
  cat <<'EOF'
Usage:
  ./scripts/send-transactions-tcp.sh <csv-file>

CSV format:
  accountId,merchantId,deviceId,ipAddress,currency,amount,occurredAt

Notes:
  - `occurredAt` is optional.
  - Blank lines and lines starting with `#` are ignored.
  - A header row starting with `accountId` is ignored.

Examples:
  ./scripts/send-transactions-tcp.sh transactions.csv
  TCP_HOST=127.0.0.1 TCP_PORT=8000 ./scripts/send-transactions-tcp.sh transactions.csv
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
{"accountId":"${account_id}","merchantId":"${merchant_id}","deviceId":"${device_id}","ipAddress":"${ip_address}","currency":"${currency}","amount":${amount},"occurredAt":"${occurred_at}"}
EOF
  else
    cat <<EOF
{"accountId":"${account_id}","merchantId":"${merchant_id}","deviceId":"${device_id}","ipAddress":"${ip_address}","currency":"${currency}","amount":${amount}}
EOF
  fi
}

submit_transaction() {
  local line_number="$1"
  local payload="$2"
  local response_file
  response_file="$(mktemp)"

  local start_ns
  start_ns="$(date +%s%N)"
  printf '%s\n' "${payload}" | nc "${TCP_HOST}" "${TCP_PORT}" > "${response_file}"
  local end_ns
  end_ns="$(date +%s%N)"

  local latency_ms
  latency_ms="$(awk -v start="${start_ns}" -v end="${end_ns}" 'BEGIN { printf "%.3f", (end - start) / 1000000 }')"
  local response
  response="$(cat "${response_file}")"
  rm -f "${response_file}"

  request_count=$((request_count + 1))
  latency_sum_ms="$(awk -v total="${latency_sum_ms}" -v value="${latency_ms}" 'BEGIN { printf "%.3f", total + value }')"
  if [[ -z "${latency_min_ms}" ]] || awk -v value="${latency_ms}" -v min="${latency_min_ms:-0}" 'BEGIN { exit !(value < min) }'; then
    latency_min_ms="${latency_ms}"
  fi
  if awk -v value="${latency_ms}" -v max="${latency_max_ms}" 'BEGIN { exit !(value > max) }'; then
    latency_max_ms="${latency_ms}"
  fi

  echo "line=${line_number} latencyMs=${latency_ms}"
  echo "${response}"
  echo
}

print_latency_summary() {
  if [[ "${request_count}" -eq 0 ]]; then
    echo "No transactions were sent."
    return
  fi

  local latency_avg_ms
  latency_avg_ms="$(awk -v total="${latency_sum_ms}" -v count="${request_count}" 'BEGIN { printf "%.3f", total / count }')"

  echo "Latency summary"
  echo "requests=${request_count} avgMs=${latency_avg_ms} minMs=${latency_min_ms} maxMs=${latency_max_ms}"
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

print_latency_summary
