#!/bin/bash
# Tibero Load Test Script (HikariCP)
#
# Note: Tibero JDBC driver must be installed manually:
# mvn install:install-file -Dfile=tibero7-jdbc.jar \
#     -DgroupId=com.tmax.tibero -DartifactId=tibero-jdbc \
#     -Dversion=7.0 -Dpackaging=jar

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/multi-db-load-tester-0.2.2.jar"

# Default values - modify as needed
TIBERO_HOST="${TIBERO_HOST:-localhost}"
TIBERO_PORT="${TIBERO_PORT:-8629}"
TIBERO_SID="${TIBERO_SID:-tibero}"
TIBERO_USER="${TIBERO_USER:-test_user}"
TIBERO_PASSWORD="${TIBERO_PASSWORD:-test_pass}"

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
echo "Tibero Load Test (HikariCP)"
echo "=========================================="
echo "Host: $TIBERO_HOST:$TIBERO_PORT"
echo "SID: $TIBERO_SID"
echo "User: $TIBERO_USER"
echo "Threads: $THREAD_COUNT"
echo "Duration: ${TEST_DURATION}s"
echo "Mode: $MODE"
echo "Pool Size: $MIN_POOL_SIZE - $MAX_POOL_SIZE"
echo "=========================================="

java -Xms512m -Xmx2g \
    -jar "$JAR_FILE" \
    --db-type tibero \
    --host "$TIBERO_HOST" \
    --port "$TIBERO_PORT" \
    --sid "$TIBERO_SID" \
    --user "$TIBERO_USER" \
    --password "$TIBERO_PASSWORD" \
    --thread-count "$THREAD_COUNT" \
    --test-duration "$TEST_DURATION" \
    --mode "$MODE" \
    --min-pool-size "$MIN_POOL_SIZE" \
    --max-pool-size "$MAX_POOL_SIZE" \
    "$@"
