#!/bin/bash
# Generate this node's Patroni config from env vars, then hand off to Patroni.
# Per-node differences (NODE_NAME, SCOPE, APP_DB) come from the compose service;
# everything else is identical across nodes and clusters.
set -euo pipefail

# Each node must advertise an address the others (and HAProxy) can reach. When scaled with
# `docker compose up --scale patroni-a=3`, instances share one service name, so we advertise
# the container's own IP and derive a unique node name from the container hostname. Both fall
# back to explicit env vars (NODE_NAME) for the simple fixed-node case.
NODE_IP="$(hostname -i | awk '{print $1}')"
NODE_NAME="${NODE_NAME:-$(hostname)}"

cat > /tmp/patroni.yml <<EOF
scope: ${SCOPE}
name: ${NODE_NAME}
namespace: /linkedin/

restapi:
  listen: 0.0.0.0:8008
  connect_address: ${NODE_IP}:8008

etcd3:
  hosts: ${ETCD_HOSTS}

bootstrap:
  # Written to etcd once, on first cluster init, then shared by every node.
  dcs:
    ttl: 30            # leader lease seconds; if not renewed within this, failover starts
    loop_wait: 10
    retry_timeout: 10
    maximum_lag_on_failover: 1048576   # a replica lagging > 1MB is not eligible to promote
    postgresql:
      use_pg_rewind: true              # rejoin a failed old primary without a full re-clone
      parameters:
        wal_level: replica
        hot_standby: "on"
        max_wal_senders: 10
        max_replication_slots: 10
  initdb:
    - encoding: UTF8
    - data-checksums
  pg_hba:
    - local all all trust
    - host all all 0.0.0.0/0 md5
    - host replication replicator 0.0.0.0/0 md5
  post_bootstrap: /usr/local/bin/post_bootstrap.sh

postgresql:
  listen: 0.0.0.0:5432
  connect_address: ${NODE_IP}:5432
  data_dir: /var/lib/postgresql/data/pgdata
  bin_dir: /usr/lib/postgresql/16/bin
  pgpass: /tmp/pgpass
  authentication:
    superuser:
      username: postgres
      password: postgres
    replication:
      username: replicator
      password: replicator
  parameters:
    password_encryption: md5

tags:
  nofailover: false
  noloadbalance: false
  clonefrom: false
  nosync: false
EOF

exec patroni /tmp/patroni.yml
