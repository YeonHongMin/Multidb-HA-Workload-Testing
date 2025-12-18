#!/bin/bash
# MySQL Load Test Script (HikariCP)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/multi-db-load-tester-0.2.2.jar"

# Default values - modify as needed
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-testdb}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-test_pass}"

THREAD_COUNT="${THREAD_COUNT:-50}"
TEST_DURATION="${TEST_DURATION:-300}"
MODE="${MODE:-full}"
# MySQL has pool size limit of 32
MIN_POOL_SIZE="${MIN_POOL_SIZE:-20}"
MAX_POOL_SIZE="${MAX_POOL_SIZE:-32}"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found. Please run build.sh first."
    exit 1
fi

echo "=========================================="
echo "MySQL Load Test (HikariCP)"
echo "=========================================="
echo "Host: $MYSQL_HOST:$MYSQL_PORT"
echo "Database: $MYSQL_DATABASE"
echo "User: $MYSQL_USER"
echo "Threads: $THREAD_COUNT"
echo "Duration: ${TEST_DURATION}s"
echo "Mode: $MODE"
echo "Pool Size: $MIN_POOL_SIZE - $MAX_POOL_SIZE"
echo "=========================================="

java -Xms512m -Xmx2g \
    -jar "$JAR_FILE" \
    --db-type mysql \
    --host "$MYSQL_HOST" \
    --port "$MYSQL_PORT" \
    --database "$MYSQL_DATABASE" \
    --user "$MYSQL_USER" \
    --password "$MYSQL_PASSWORD" \
    --thread-count "$THREAD_COUNT" \
    --test-duration "$TEST_DURATION" \
    --mode "$MODE" \
    --min-pool-size "$MIN_POOL_SIZE" \
    --max-pool-size "$MAX_POOL_SIZE" \
    "$@"
