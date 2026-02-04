#!/usr/bin/env bash
set -euo pipefail

APP_NAME="gateway-server"
APP_HOME="/opt/dhgx/dhgx-portal/${APP_NAME}"
JAR_FILE="${APP_HOME}/${APP_NAME}.jar"
CONFIG_FILE="${APP_HOME}/bootstrap.yml"
LOG_DIR="/opt/dhgx/dhgx-portal/logs"
LOG_FILE="${LOG_DIR}/${APP_NAME}.log"
LOGROTATE_CONF="${LOG_DIR}/${APP_NAME}.logrotate.conf"
LOGROTATE_STATE="${LOG_DIR}/${APP_NAME}.logrotate.state"
PID_FILE="/opt/dhgx/dhgx-portal/run/${APP_NAME}.pid"
JAVA_OPTS="${JAVA_OPTS:-}"

mkdir -p "${LOG_DIR}" "$(dirname "${PID_FILE}")"

rotate_logs() {
  if ! command -v logrotate >/dev/null 2>&1; then
    return
  fi

  cat > "${LOGROTATE_CONF}" <<EOF
${LOG_FILE} {
  daily
  rotate 5
  size 5M
  missingok
  notifempty
  copytruncate
  dateext
  dateformat -%Y%m%d
}
EOF

  logrotate -s "${LOGROTATE_STATE}" "${LOGROTATE_CONF}" >/dev/null 2>&1 || true
}

start() {
  if [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
    echo "${APP_NAME} is already running (PID $(cat "${PID_FILE}"))."
    exit 0
  fi

  if [[ ! -f "${JAR_FILE}" ]]; then
    echo "Jar not found: ${JAR_FILE}"
    exit 1
  fi

  if [[ ! -f "${CONFIG_FILE}" ]]; then
    echo "Config not found: ${CONFIG_FILE}"
    exit 1
  fi

  rotate_logs

  nohup java ${JAVA_OPTS} -Dspring.config.location="${CONFIG_FILE}" -jar "${JAR_FILE}" \
    >> "${LOG_FILE}" 2>&1 &

  echo $! > "${PID_FILE}"
  echo "Started ${APP_NAME} (PID $(cat "${PID_FILE}"))."
}

stop() {
  if [[ ! -f "${PID_FILE}" ]]; then
    echo "${APP_NAME} is not running (pid file missing)."
    exit 0
  fi

  PID=$(cat "${PID_FILE}")
  if ! kill -0 "${PID}" 2>/dev/null; then
    echo "${APP_NAME} is not running (stale pid ${PID})."
    rm -f "${PID_FILE}"
    exit 0
  fi

  kill "${PID}"
  for _ in {1..30}; do
    if ! kill -0 "${PID}" 2>/dev/null; then
      rm -f "${PID_FILE}"
      echo "Stopped ${APP_NAME}."
      return
    fi
    sleep 1
  done

  echo "${APP_NAME} did not stop gracefully, sending SIGKILL."
  kill -9 "${PID}" || true
  rm -f "${PID_FILE}"
}

restart() {
  stop
  start
}

case "${1:-}" in
  start) start ;;
  stop) stop ;;
  restart) restart ;;
  *)
    echo "Usage: $0 {start|stop|restart}"
    exit 1
    ;;
esac
