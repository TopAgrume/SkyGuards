#!/bin/sh
set -x

sleep 6
echo 'Waiting for kafka to be reachable'
kafka-topics --bootstrap-server ${KAFKA_BROKER} --list

echo 'Creating kafka topics...'
kafka-topics --bootstrap-server ${KAFKA_BROKER} --create --if-not-exists --topic reports --replication-factor 3 --partitions 3 --config min.insync.replicas=2 --config retention.ms=120000
kafka-topics --bootstrap-server ${KAFKA_BROKER} --create --if-not-exists --topic alerts --replication-factor 3 --partitions 3 --config min.insync.replicas=2 --config retention.ms=120000

echo 'Setup done. Following topics created:'
kafka-topics --bootstrap-server ${KAFKA_BROKER} --list

tail -f /dev/null