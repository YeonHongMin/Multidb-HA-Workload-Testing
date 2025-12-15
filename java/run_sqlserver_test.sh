#!/bin/bash
# SQL Server Load Test Script (HikariCP)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/multi-db-load-tester-0.2.jar"

# Default values - modify as needed
SQLSERVER_HOST="${SQLSERVER_HOST:-localhost}"
SQLSERVER_PORT="${SQLSERVER_PORT:-1433}"
SQLSERVER_DATABASE="${SQLSERVER_DATABASE:-testdb}"
SQLSERVER_USER="${SQLSERVER_USER:-sa}"
SQLSERVER_PASSWORD="${SQLSERVER_PASSWORD:-test_pass}"

THREAD_COUNT="${THREAD_COUNT:-100}"
TEST_DURATION="${TEST_DURATION:-300}"
MODE="${MODE:-full}"
MIN_POOL_SIZE="${MIN_POOL_SIZE:-100}"
MAX_POOL_SIZE="${MAX_POOL_SIZE:-200}"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found. Please run build.sh first."
    exit 1
fi

echo "=========================================="
echo "SQL Server Load Test (HikariCP)"
echo "=========================================="
echo "Host: $SQLSERVER_HOST:$SQLSERVER_PORT"
echo "Database: $SQLSERVER_DATABASE"
echo "User: $SQLSERVER_USER"
echo "Threads: $THREAD_COUNT"
echo "Duration: ${TEST_DURATION}s"
echo "Mode: $MODE"
echo "Pool Size: $MIN_POOL_SIZE - $MAX_POOL_SIZE"
echo "=========================================="

java -Xms512m -Xmx2g \
    -jar "$JAR_FILE" \
    --db-type sqlserver \
    --host "$SQLSERVER_HOST" \
    --port "$SQLSERVER_PORT" \
    --database "$SQLSERVER_DATABASE" \
    --user "$SQLSERVER_USER" \
    --password "$SQLSERVER_PASSWORD" \
    --thread-count "$THREAD_COUNT" \
    --test-duration "$TEST_DURATION" \
    --mode "$MODE" \
    --min-pool-size "$MIN_POOL_SIZE" \
    --max-pool-size "$MAX_POOL_SIZE" \
    "$@"
