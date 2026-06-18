#!/bin/bash

# SingleStore Load Test Script
# Multi-Database Load Tester v0.2.6 (HikariCP)

# Default Configuration
DB_HOST="${SINGLESTORE_HOST:-localhost}"
DB_PORT="${SINGLESTORE_PORT:-3306}"
DB_NAME="${SINGLESTORE_DATABASE:-testdb}"
DB_USER="${SINGLESTORE_USER:-root}"
DB_PASSWORD="${SINGLESTORE_PASSWORD:-password}"

# Test Configuration
THREAD_COUNT="${THREAD_COUNT:-100}"
TEST_DURATION="${TEST_DURATION:-300}"
MODE="${MODE:-full}"
WARMUP="${WARMUP:-30}"
TARGET_TPS="${TARGET_TPS:-0}"

# Output Configuration
OUTPUT_FORMAT="${OUTPUT_FORMAT:-}"
OUTPUT_FILE="${OUTPUT_FILE:-}"

# Connection Pool Configuration
MIN_POOL_SIZE="${MIN_POOL_SIZE:-100}"
MAX_POOL_SIZE="${MAX_POOL_SIZE:-200}"

# JAR file
JAR_FILE="target/multi-db-load-tester-0.2.6.jar"

echo "=================================="
echo "SingleStore Load Test Configuration"
echo "=================================="
echo "Host: $DB_HOST"
echo "Port: $DB_PORT"
echo "Database: $DB_NAME"
echo "User: $DB_USER"
echo "Threads: $THREAD_COUNT"
echo "Duration: ${TEST_DURATION}s"
echo "Mode: $MODE"
echo "Warmup: ${WARMUP}s"
if [ "$TARGET_TPS" -gt 0 ]; then
    echo "Target TPS: $TARGET_TPS"
fi
echo "Min Pool Size: $MIN_POOL_SIZE"
echo "Max Pool Size: $MAX_POOL_SIZE"
echo "=================================="
echo ""

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found: $JAR_FILE"
    echo "Please run build.sh first to build the project."
    exit 1
fi

# Build command
CMD="java -jar $JAR_FILE"
CMD="$CMD --db-type singlestore"
CMD="$CMD --host $DB_HOST"
CMD="$CMD --port $DB_PORT"
CMD="$CMD --database $DB_NAME"
CMD="$CMD --user $DB_USER"
CMD="$CMD --password $DB_PASSWORD"
CMD="$CMD --thread-count $THREAD_COUNT"
CMD="$CMD --test-duration $TEST_DURATION"
CMD="$CMD --mode $MODE"
CMD="$CMD --warmup $WARMUP"
CMD="$CMD --min-pool-size $MIN_POOL_SIZE"
CMD="$CMD --max-pool-size $MAX_POOL_SIZE"

if [ "$TARGET_TPS" -gt 0 ]; then
    CMD="$CMD --target-tps $TARGET_TPS"
fi

if [ -n "$OUTPUT_FORMAT" ] && [ -n "$OUTPUT_FILE" ]; then
    CMD="$CMD --output-format $OUTPUT_FORMAT"
    CMD="$CMD --output-file $OUTPUT_FILE"
fi

# Run the test
echo "Starting SingleStore load test..."
echo ""
eval $CMD
