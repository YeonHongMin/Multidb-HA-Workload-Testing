#!/bin/bash
# IBM DB2 Load Test Script (HikariCP)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/multi-db-load-tester-0.2.jar"

# Default values - modify as needed
DB2_HOST="${DB2_HOST:-localhost}"
DB2_PORT="${DB2_PORT:-50000}"
DB2_DATABASE="${DB2_DATABASE:-testdb}"
DB2_USER="${DB2_USER:-db2inst1}"
DB2_PASSWORD="${DB2_PASSWORD:-test_pass}"

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
echo "IBM DB2 Load Test (HikariCP)"
echo "=========================================="
echo "Host: $DB2_HOST:$DB2_PORT"
echo "Database: $DB2_DATABASE"
echo "User: $DB2_USER"
echo "Threads: $THREAD_COUNT"
echo "Duration: ${TEST_DURATION}s"
echo "Mode: $MODE"
echo "Pool Size: $MIN_POOL_SIZE - $MAX_POOL_SIZE"
echo "=========================================="

java -Xms512m -Xmx2g \
    -jar "$JAR_FILE" \
    --db-type db2 \
    --host "$DB2_HOST" \
    --port "$DB2_PORT" \
    --database "$DB2_DATABASE" \
    --user "$DB2_USER" \
    --password "$DB2_PASSWORD" \
    --thread-count "$THREAD_COUNT" \
    --test-duration "$TEST_DURATION" \
    --mode "$MODE" \
    --min-pool-size "$MIN_POOL_SIZE" \
    --max-pool-size "$MAX_POOL_SIZE" \
    "$@"
