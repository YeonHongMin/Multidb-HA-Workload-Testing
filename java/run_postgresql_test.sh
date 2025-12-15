#!/bin/bash
# PostgreSQL Load Test Script (HikariCP)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/multi-db-load-tester-0.2.jar"

# Default values - modify as needed
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DATABASE="${POSTGRES_DATABASE:-testdb}"
POSTGRES_USER="${POSTGRES_USER:-test_user}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-test_pass}"

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
echo "PostgreSQL Load Test (HikariCP)"
echo "=========================================="
echo "Host: $POSTGRES_HOST:$POSTGRES_PORT"
echo "Database: $POSTGRES_DATABASE"
echo "User: $POSTGRES_USER"
echo "Threads: $THREAD_COUNT"
echo "Duration: ${TEST_DURATION}s"
echo "Mode: $MODE"
echo "Pool Size: $MIN_POOL_SIZE - $MAX_POOL_SIZE"
echo "=========================================="

java -Xms512m -Xmx2g \
    -jar "$JAR_FILE" \
    --db-type postgresql \
    --host "$POSTGRES_HOST" \
    --port "$POSTGRES_PORT" \
    --database "$POSTGRES_DATABASE" \
    --user "$POSTGRES_USER" \
    --password "$POSTGRES_PASSWORD" \
    --thread-count "$THREAD_COUNT" \
    --test-duration "$TEST_DURATION" \
    --mode "$MODE" \
    --min-pool-size "$MIN_POOL_SIZE" \
    --max-pool-size "$MAX_POOL_SIZE" \
    "$@"
