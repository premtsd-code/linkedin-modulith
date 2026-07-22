#!/bin/bash
# Runs ONCE, on the leader, right after the cluster is first initialised. Patroni passes
# a libpq connection string to the fresh cluster as $1. Create the application user + DB
# so the app can connect as 'linkedin' (owner of its database, so ddl-auto can create tables).
set -euo pipefail

CONN="$1"
APP_USER="${APP_USER:-linkedin}"
APP_PASSWORD="${APP_PASSWORD:-linkedin}"

psql "$CONN" -v ON_ERROR_STOP=1 <<SQL
  CREATE ROLE ${APP_USER} WITH LOGIN PASSWORD '${APP_PASSWORD}';
  CREATE DATABASE ${APP_DB} OWNER ${APP_USER};
SQL
