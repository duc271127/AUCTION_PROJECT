#!/usr/bin/env sh
set -eu

if [ -n "${PORT:-}" ] && [ -z "${AUCTION_SERVER_PORT:-}" ]; then
  export AUCTION_SERVER_PORT="$PORT"
fi

if [ -z "${AUCTION_DB_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ] && [ -n "${RENDER_DB_HOST:-}" ] && [ -n "${RENDER_DB_NAME:-}" ]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${RENDER_DB_HOST}:${RENDER_DB_PORT:-5432}/${RENDER_DB_NAME}"
fi

if [ -z "${AUCTION_DB_USERNAME:-}" ] && [ -z "${SPRING_DATASOURCE_USERNAME:-}" ] && [ -n "${RENDER_DB_USER:-}" ]; then
  export SPRING_DATASOURCE_USERNAME="${RENDER_DB_USER}"
fi

if [ -z "${AUCTION_DB_PASSWORD:-}" ] && [ -z "${SPRING_DATASOURCE_PASSWORD:-}" ] && [ -n "${RENDER_DB_PASSWORD:-}" ]; then
  export SPRING_DATASOURCE_PASSWORD="${RENDER_DB_PASSWORD}"
fi

export SPRING_DATASOURCE_DRIVER_CLASS_NAME="${SPRING_DATASOURCE_DRIVER_CLASS_NAME:-org.postgresql.Driver}"
export SPRING_JPA_DATABASE_PLATFORM="${SPRING_JPA_DATABASE_PLATFORM:-org.hibernate.dialect.PostgreSQLDialect}"

exec java ${JAVA_OPTS:-} -jar /app/app.jar
