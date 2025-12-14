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

# Install local JDBC drivers to Maven local repository
echo ""
echo "Installing local JDBC drivers..."
echo ""

# Oracle JDBC
if [ -f "jre/oracle/ojdbc10.jar" ]; then
    echo "Installing Oracle JDBC driver..."
    mvn install:install-file -Dfile=jre/oracle/ojdbc10.jar \
        -DgroupId=com.oracle.database.jdbc -DartifactId=ojdbc10 \
        -Dversion=local -Dpackaging=jar -q
fi

# PostgreSQL JDBC
if [ -f "jre/postgresql/postgresql-42.2.9.jar" ]; then
    echo "Installing PostgreSQL JDBC driver..."
    mvn install:install-file -Dfile=jre/postgresql/postgresql-42.2.9.jar \
        -DgroupId=org.postgresql -DartifactId=postgresql \
        -Dversion=local -Dpackaging=jar -q
fi

# MySQL JDBC
if [ -f "jre/mysql/mysql-connector-j-9.5.0.jar" ]; then
    echo "Installing MySQL JDBC driver..."
    mvn install:install-file -Dfile=jre/mysql/mysql-connector-j-9.5.0.jar \
        -DgroupId=com.mysql -DartifactId=mysql-connector-j \
        -Dversion=local -Dpackaging=jar -q
fi

# SQL Server JDBC
if [ -f "jre/sqlserver/mssql-jdbc-13.2.1.jre11.jar" ]; then
    echo "Installing SQL Server JDBC driver..."
    mvn install:install-file -Dfile=jre/sqlserver/mssql-jdbc-13.2.1.jre11.jar \
        -DgroupId=com.microsoft.sqlserver -DartifactId=mssql-jdbc \
        -Dversion=local -Dpackaging=jar -q
fi

# Tibero JDBC
if [ -f "jre/tibero/tibero7-jdbc.jar" ]; then
    echo "Installing Tibero JDBC driver..."
    mvn install:install-file -Dfile=jre/tibero/tibero7-jdbc.jar \
        -DgroupId=com.tmax.tibero -DartifactId=tibero-jdbc \
        -Dversion=local -Dpackaging=jar -q
fi

# IBM DB2 JDBC
if [ -f "jre/db2/jcc-12.1.3.0.jar" ]; then
    echo "Installing IBM DB2 JDBC driver..."
    mvn install:install-file -Dfile=jre/db2/jcc-12.1.3.0.jar \
        -DgroupId=com.ibm.db2 -DartifactId=jcc \
        -Dversion=local -Dpackaging=jar -q
fi

echo ""
echo "Local JDBC drivers installed successfully!"
echo ""

# Clean and package
echo "Running Maven build..."
mvn clean package -DskipTests

if [ -f "target/multi-db-load-tester-2.1.0.jar" ]; then
    echo ""
    echo "=========================================="
    echo "Build successful!"
    echo "=========================================="
    echo "JAR file: target/multi-db-load-tester-2.1.0.jar"
    echo "Size: $(ls -lh target/multi-db-load-tester-2.1.0.jar | awk '{print $5}')"
    echo ""
    echo "Included JDBC drivers:"
    echo "  - Oracle (ojdbc10)"
    echo "  - PostgreSQL (postgresql-42.2.9)"
    echo "  - MySQL (mysql-connector-j-9.5.0)"
    echo "  - SQL Server (mssql-jdbc-13.2.1)"
    echo "  - Tibero (tibero7-jdbc)"
    echo "  - IBM DB2 (jcc-12.1.3.0)"
    echo ""
    echo "Usage example:"
    echo "  java -jar target/multi-db-load-tester-2.1.0.jar --help"
    echo ""
else
    echo "Build failed!"
    exit 1
fi
