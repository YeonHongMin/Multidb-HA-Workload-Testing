#!/bin/bash
# Build script for Multi-Database Load Tester (HikariCP)

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "Building Multi-Database Load Tester"
echo "=========================================="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven first."
    exit 1
fi

# Clean and package
echo "Running Maven build..."
mvn clean package -DskipTests

if [ -f "target/multi-db-load-tester-2.1.0.jar" ]; then
    echo ""
    echo "=========================================="
    echo "Build successful!"
    echo "=========================================="
    echo "JAR file: target/multi-db-load-tester-2.1.0.jar"
    echo ""
    echo "Usage example:"
    echo "  java -jar target/multi-db-load-tester-2.1.0.jar --help"
    echo ""
else
    echo "Build failed!"
    exit 1
fi
