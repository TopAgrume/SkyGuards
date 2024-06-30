#!/bin/sh
set -x

sleep 6
echo 'Waiting for kafka to be reachable'
kafka-topics --bootstrap-server ${KAFKA_BROKER} --list

echo 'Creating kafka topics...'
kafka-topics --bootstrap-server ${KAFKA_BROKER} --create --if-not-exists --topic reports --replication-factor 1 --partitions 1
kafka-topics --bootstrap-server ${KAFKA_BROKER} --create --if-not-exists --topic alerts --replication-factor 1 --partitions 1

echo 'Setup done. Following topics created:'
kafka-topics --bootstrap-server ${KAFKA_BROKER} --list