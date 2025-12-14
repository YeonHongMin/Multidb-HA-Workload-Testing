#!/bin/bash
# Oracle Load Test Script (HikariCP)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/multi-db-load-tester-2.1.0.jar"

# Default values - modify as needed
ORACLE_HOST="${ORACLE_HOST:-localhost}"
ORACLE_PORT="${ORACLE_PORT:-1521}"
ORACLE_SID="${ORACLE_SID:-XEPDB1}"
ORACLE_USER="${ORACLE_USER:-test_user}"
ORACLE_PASSWORD="${ORACLE_PASSWORD:-test_pass}"

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
echo "Oracle Load Test (HikariCP)"
echo "=========================================="
echo "Host: $ORACLE_HOST:$ORACLE_PORT"
echo "SID: $ORACLE_SID"
echo "User: $ORACLE_USER"
echo "Threads: $THREAD_COUNT"
echo "Duration: ${TEST_DURATION}s"
echo "Mode: $MODE"
echo "Pool Size: $MIN_POOL_SIZE - $MAX_POOL_SIZE"
echo "=========================================="

java -Xms512m -Xmx2g \
    -jar "$JAR_FILE" \
    --db-type oracle \
    --host "$ORACLE_HOST" \
    --port "$ORACLE_PORT" \
    --sid "$ORACLE_SID" \
    --user "$ORACLE_USER" \
    --password "$ORACLE_PASSWORD" \
    --thread-count "$THREAD_COUNT" \
    --test-duration "$TEST_DURATION" \
    --mode "$MODE" \
    --min-pool-size "$MIN_POOL_SIZE" \
    --max-pool-size "$MAX_POOL_SIZE" \
    "$@"
