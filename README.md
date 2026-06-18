# Multi-Database Load Tester (Java) v0.2.6

High-performance multi-threaded database load testing tool supporting Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2, and SingleStore (HikariCP-based)

## Key Features

- **7 Database Support**: Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2, SingleStore
- **HikariCP Connection Pool**: High-performance JDBC connection pooling
- **High-Performance Multi-threading**: Up to 1,000 concurrent sessions supported
- **6 Operation Modes**: full, insert-only, select-only, update-only, delete-only, mixed
- **Sub-second Transaction Measurement**: Real-time sub-second TPS monitoring
- **Latency Measurement**: P50/P95/P99 response time statistics
- **Warmup Period**: Statistics-excluded warmup support (default 30 seconds)
- **Schema Reuse**: Automatic reuse of existing tables/sequences
- **Table Initialization**: Clean state testing with `--truncate` option (recommended)
- **Gradual Load Increase**: Ramp-up functionality
- **TPS Limiting**: Token Bucket-based rate limiting
- **Batch INSERT**: Bulk data insertion optimization
- **Result Export**: CSV/JSON format support
- **Graceful Shutdown**: Safe Ctrl+C termination
- **Leak Detection**: HikariCP built-in connection leak detection

---

## Prerequisites

### 1. Java Development Kit (JDK) 17+

This tool requires Java 17 or higher.

#### Version Check

```bash
java -version
# openjdk version "17.0.x" or higher required
```

#### Installation

**macOS (Homebrew)**

```bash
brew install openjdk@17
# Or latest LTS version
brew install openjdk@21

# Environment variable setup
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH
```

**Ubuntu/Debian**

```bash
sudo apt update
sudo apt install openjdk-17-jdk

# Environment variable setup (add to ~/.bashrc or ~/.zshrc)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

**RHEL/CentOS/Rocky Linux**

```bash
sudo yum install java-17-openjdk-devel
# Or
sudo dnf install java-17-openjdk-devel

# Environment variable setup
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

**Windows**

1. Download JDK 17+ from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)
2. Set environment variables after installation:
   - `JAVA_HOME`: JDK installation path (e.g., `C:\Program Files\Java\jdk-17`)
   - Add `%JAVA_HOME%\bin` to `PATH`

---

### 2. Apache Maven 3.6+

Maven is required for project builds.

#### Version Check

```bash
mvn -version
# Apache Maven 3.6.x or higher required
```

#### Installation

**macOS (Homebrew)**

```bash
brew install maven
```

**Ubuntu/Debian**

```bash
sudo apt update
sudo apt install maven
```

**RHEL/CentOS/Rocky Linux**

```bash
sudo yum install maven
# Or
sudo dnf install maven
```

**Windows**

1. Download Binary zip from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to desired location (e.g., `C:\Program Files\Apache\maven`)
3. Set environment variables:
   - `MAVEN_HOME`: Maven installation path
   - Add `%MAVEN_HOME%\bin` to `PATH`

---

### 3. Database Requirements

#### Supported Database Versions

| Database   | Minimum Version | Recommended Version | Default Port |
| ---------- | --------------- | ------------------- | ------------ |
| Oracle     | 19c             | 21c+                | 1521         |
| PostgreSQL | 11              | 15+                 | 5432         |
| MySQL      | 5.7             | 8.0+                | 3306         |
| SQL Server | 2016            | 2019+               | 1433         |
| Tibero     | 6               | 7                   | 8629         |
| IBM DB2    | 11.1            | 11.5+               | 50000        |
| SingleStore| 7.5             | 8.0+                | 3306         |

#### Database User Privileges

The following privileges are required to run tests:

**Oracle**

```sql
-- Create test user (connect as SYS or SYSTEM)
CREATE USER test_user IDENTIFIED BY test_pass;
GRANT CONNECT, RESOURCE TO test_user;
GRANT CREATE TABLE, CREATE SEQUENCE TO test_user;
GRANT UNLIMITED TABLESPACE TO test_user;
```

**PostgreSQL**

```sql
-- Create test database and user
CREATE USER test_user WITH PASSWORD 'test_pass';
CREATE DATABASE testdb OWNER test_user;
GRANT ALL PRIVILEGES ON DATABASE testdb TO test_user;
```

**MySQL**

```sql
-- Create test database and user
CREATE DATABASE testdb;
CREATE USER 'test_user'@'%' IDENTIFIED BY 'test_pass';
GRANT ALL PRIVILEGES ON testdb.* TO 'test_user'@'%';
FLUSH PRIVILEGES;

-- Check max_connections setting (for high thread counts)
SHOW VARIABLES LIKE 'max_connections';
-- Increase if needed: SET GLOBAL max_connections = 500;
```

**SQL Server**

```sql
-- Create test database and user
CREATE DATABASE testdb;
USE testdb;
CREATE LOGIN test_user WITH PASSWORD = 'test_pass';
CREATE USER test_user FOR LOGIN test_user;
ALTER ROLE db_owner ADD MEMBER test_user;
```

**Tibero**

```sql
-- Create test user
CREATE USER test_user IDENTIFIED BY test_pass;
GRANT CONNECT, RESOURCE TO test_user;
GRANT CREATE TABLE, CREATE SEQUENCE TO test_user;
```

**IBM DB2**

```sql
-- Create test database and user
CREATE DATABASE testdb;
CONNECT TO testdb;
CREATE USER test_user;
GRANT CONNECT, CREATETAB, IMPLICIT_SCHEMA ON DATABASE TO USER test_user;
```

**SingleStore**

```sql
-- Create test database and user
CREATE DATABASE testdb;
CREATE USER 'test_user'@'%' IDENTIFIED BY 'test_pass';
GRANT ALL PRIVILEGES ON testdb.* TO 'test_user'@'%';
```

#### Database Server Configuration

Database server settings may need adjustment for high concurrency testing:

**Oracle**

```sql
-- Check maximum sessions/processes
SHOW PARAMETER sessions;
SHOW PARAMETER processes;

-- Increase if needed (requires restart)
ALTER SYSTEM SET sessions=1000 SCOPE=SPFILE;
ALTER SYSTEM SET processes=500 SCOPE=SPFILE;
```

**PostgreSQL** (`postgresql.conf`)

```ini
max_connections = 500
shared_buffers = 256MB
```

**MySQL** (`my.cnf` or `my.ini`)

```ini
[mysqld]
max_connections = 500
max_user_connections = 0
```

**SQL Server**

```sql
-- Supports 32,767 connections by default
-- Check memory settings
EXEC sp_configure 'max server memory';
```

---

### 4. Network Requirements

#### Firewall Configuration

The following ports must be open for test client access to the database server:

| Database   | Port  | Firewall Command (Linux)                        |
| ---------- | ----- | ----------------------------------------------- |
| Oracle     | 1521  | `firewall-cmd --add-port=1521/tcp --permanent`  |
| PostgreSQL | 5432  | `firewall-cmd --add-port=5432/tcp --permanent`  |
| MySQL      | 3306  | `firewall-cmd --add-port=3306/tcp --permanent`  |
| SQL Server | 1433  | `firewall-cmd --add-port=1433/tcp --permanent`  |
| Tibero     | 8629  | `firewall-cmd --add-port=8629/tcp --permanent`  |
| IBM DB2    | 50000 | `firewall-cmd --add-port=50000/tcp --permanent` |
| SingleStore| 3306  | `firewall-cmd --add-port=3306/tcp --permanent`  |

#### Connection Test

```bash
# Port connection test
nc -zv <host> <port>
# Or
telnet <host> <port>

# Example
nc -zv 192.168.0.100 1521
```

---

### 5. System Resource Requirements

#### Minimum Specifications

| Item  | Minimum      | Recommended |
| ----- | ------------ | ----------- |
| CPU   | 2 cores      | 4 cores+    |
| RAM   | 2GB          | 8GB+        |
| Disk  | 1GB (install)| SSD recommended |

#### JVM Memory Settings

JVM heap memory adjustment is required for high-load testing:

```bash
# Default execution (2GB heap)
java -Xms1g -Xmx2g -jar multi-db-load-tester-0.2.6.jar ...

# High-load testing (4GB heap, 500+ threads)
java -Xms2g -Xmx4g -jar multi-db-load-tester-0.2.6.jar ...

# Ultra high-load testing (8GB heap, 1000+ threads)
java -Xms4g -Xmx8g -XX:+UseG1GC -jar multi-db-load-tester-0.2.6.jar ...
```

#### Recommended Resources by Thread Count

| Thread Count | RAM  | JVM Heap | Connection Pool |
| ------------ | ---- | -------- | --------------- |
| ~100         | 4GB  | 2GB      | 100-150         |
| ~200         | 8GB  | 4GB      | 200-250         |
| ~500         | 16GB | 8GB      | 500-600         |
| ~1000        | 32GB | 16GB     | 1000-1200       |

#### File Descriptor Limits (Linux/macOS)

File descriptor limit increases may be needed for high-load testing:

```bash
# Check current limit
ulimit -n

# Temporary increase
ulimit -n 65535

# Permanent setting (/etc/security/limits.conf)
*    soft    nofile    65535
*    hard    nofile    65535
```

---

### 6. JDBC Drivers (Included Automatically)

All JDBC drivers are included in the `java/jre/` directory and automatically packaged into the JAR during build.

#### Included Drivers

| Database   | Driver File                 | Location                |
| ---------- | --------------------------- | ----------------------- |
| Oracle     | ojdbc10.jar                 | `java/jre/oracle/`      |
| PostgreSQL | postgresql-42.2.9.jar       | `java/jre/postgresql/`  |
| MySQL      | mysql-connector-j-9.5.0.jar | `java/jre/mysql/`       |
| SQL Server | mssql-jdbc-13.2.1.jre11.jar | `java/jre/sqlserver/`   |
| Tibero     | tibero7-jdbc.jar            | `java/jre/tibero/`      |
| IBM DB2    | jcc-12.1.3.0.jar            | `java/jre/db2/`         |
| SingleStore| singlestore-jdbc-1.2.1.jar  | `java/jre/singlestore/` |

#### Build

```bash
cd java
./build.sh
```

The build script automatically:

1. Installs local JDBC drivers to the Maven local repository
2. Creates an executable JAR containing all drivers

> **Note**: All JDBC drivers are included in the `java/jre/` directory.

---

## Quick Start

### 1. Build

```bash
cd java
./build.sh
```

Or:

```bash
cd java
mvn clean package -DskipTests
```

### 2. Run

```bash
# Default execution (start clean with --truncate, default warmup 30 seconds)
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test_user --password test_pass \
    --truncate \
    --thread-count 100 \
    --test-duration 60

# Run without warmup
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test_user --password test_pass \
    --truncate \
    --thread-count 100 \
    --test-duration 60 \
    --warmup 0
```

> **Note**: The `--truncate` option deletes table data and resets IDs/sequences before testing. The default `--warmup` is 30 seconds.

### 3. Help

```bash
java -jar java/target/multi-db-load-tester-0.2.6.jar --help
```

---

## Operation Modes (--mode)

| Mode          | Description                                | Use Case                    |
| ------------- | ------------------------------------------ | --------------------------- |
| `full`        | INSERT → SELECT → UPDATE → DELETE (default)| Full CRUD cycle validation  |
| `insert-only` | INSERT → COMMIT only                       | Maximum write throughput    |
| `select-only` | SELECT only                                | Read performance measurement|
| `update-only` | UPDATE → COMMIT                            | Update performance measurement|
| `delete-only` | DELETE → COMMIT                            | Delete performance measurement|
| `mixed`       | INSERT/UPDATE/DELETE mixed (60:20:15:5)    | Realistic workload simulation|

### ⚠️ Caution: When using update-only / delete-only / select-only modes

`update-only`, `delete-only`, and `select-only` modes **require existing data**.

Since v0.2, if tables already exist, they are **automatically reused** (no DROP).
Therefore, consecutive runs are possible without additional options.

#### Correct Usage Example

```bash
# Step 1: Insert data with insert-only (start clean with --truncate)
java -jar target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --sid ORCL \
    --user test --password pass \
    --truncate \
    --mode insert-only \
    --test-duration 60 \
    --warmup 10

# Step 2: Run update-only (keep existing data without --truncate)
java -jar target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --sid ORCL \
    --user test --password pass \
    --mode update-only \
    --test-duration 60 \
    --warmup 10

# Step 3: Run delete-only (keep existing data without --truncate)
java -jar target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --sid ORCL \
    --user test --password pass \
    --mode delete-only \
    --test-duration 60 \
    --warmup 10
```

> **Note**: For most tests, it is recommended to use the `--truncate` option to start from a consistent initial state. Only omit `--truncate` when existing data is needed for update-only/delete-only/select-only modes.

---

## Database-Specific Examples

### Oracle

```bash
# SID format
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --sid ORCL \
    --user test_user --password pass \
    --truncate \
    --thread-count 200 \
    --test-duration 300 \
    --warmup 30

# Service Name format
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --service-name XEPDB1 \
    --user test_user --password pass \
    --truncate \
    --thread-count 200 \
    --test-duration 300 \
    --warmup 30
```

### PostgreSQL

```bash
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type postgresql \
    --host localhost --port 5432 --database testdb \
    --user test_user --password pass \
    --truncate \
    --thread-count 200 \
    --test-duration 300 \
    --warmup 30
```

### MySQL

```bash
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type mysql \
    --host localhost --port 3306 --database testdb \
    --user root --password pass \
    --truncate \
    --thread-count 50 \
    --test-duration 300 \
    --warmup 30
```

> **Note**: MySQL connection pool size is limited to 32 by default.

### SQL Server

```bash
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type sqlserver \
    --host localhost --port 1433 --database testdb \
    --user sa --password pass \
    --truncate \
    --thread-count 200 \
    --test-duration 300 \
    --warmup 30
```

### Tibero

```bash
# SID format
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type tibero \
    --host 192.168.0.140 --port 8629 --sid tibero \
    --user test_user --password pass \
    --truncate \
    --thread-count 200 \
    --test-duration 300 \
    --warmup 30

# Service Name format
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type tibero \
    --host 192.168.0.140 --port 8629 --service-name tibero_svc \
    --user test_user --password pass \
    --truncate \
    --thread-count 200 \
    --test-duration 300 \
    --warmup 30
```

### IBM DB2

```bash
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type db2 \
    --host localhost --port 50000 --database testdb \
    --user db2inst1 --password pass \
    --truncate \
    --thread-count 200 \
    --test-duration 300 \
    --warmup 30
```

### SingleStore

```bash
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type singlestore \
    --host localhost --port 3306 --database testdb \
    --user root --password pass \
    --truncate \
    --thread-count 100 \
    --test-duration 300 \
    --warmup 30
```

> **Note**: SingleStore connection pool size is limited to 32 by default.

---

## Advanced Features

### Direct JDBC URL Usage (--jdbc-url)

You can specify a direct JDBC URL for complex connection strings or RAC/SCAN environments.

```bash
# Oracle RAC/SCAN environment
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --jdbc-url "jdbc:oracle:thin:@//scan-ip:1521/SERVICE_NAME" \
    --user test --password pass \
    --truncate \
    --thread-count 100 \
    --test-duration 60

# Oracle TNS format
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --jdbc-url "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=host1)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=host2)(PORT=1521))(LOAD_BALANCE=yes)(CONNECT_DATA=(SERVICE_NAME=PROD)))" \
    --user test --password pass \
    --thread-count 100

# PostgreSQL with SSL
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type postgresql \
    --jdbc-url "jdbc:postgresql://localhost:5432/testdb?ssl=true&sslmode=require" \
    --user test --password pass \
    --thread-count 50

# MySQL with options
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type mysql \
    --jdbc-url "jdbc:mysql://localhost:3306/testdb?useSSL=false&serverTimezone=UTC&rewriteBatchedStatements=true" \
    --user root --password pass \
    --thread-count 50

# SQL Server with instance name
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type sqlserver \
    --jdbc-url "jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=testdb;encrypt=false" \
    --user sa --password pass \
    --thread-count 100

# Tibero
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type tibero \
    --jdbc-url "jdbc:tibero:thin:@//192.168.0.100:8629/tibero" \
    --user test --password pass \
    --thread-count 100

# IBM DB2
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type db2 \
    --jdbc-url "jdbc:db2://localhost:50000/testdb:currentSchema=DB2INST1;" \
    --user db2inst1 --password pass \
    --thread-count 100

# SingleStore
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type singlestore \
    --jdbc-url "jdbc:singlestore://localhost:3306/testdb?allowPublicKeyRetrieval=true" \
    --user root --password pass \
    --thread-count 50
```

> **Note**: When using the `--jdbc-url` option, `--host`, `--port`, `--database`, `--sid`, and `--service-name` options are ignored.

---

### Test After Table Initialization (--truncate)

```bash
# Delete existing data and start test from clean state
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test --password pass \
    --truncate \
    --thread-count 100 \
    --test-duration 60
```

> **Note**: The `--truncate` option deletes all data from the table and restarts IDs/sequences from 1.

### Warmup + Ramp-up + Rate Limiting

```bash
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type postgresql \
    --host localhost --port 5432 --database testdb \
    --user test --password pass \
    --truncate \
    --warmup 30 \
    --ramp-up 60 \
    --target-tps 5000 \
    --thread-count 200 \
    --test-duration 300
```

### Batch INSERT

```bash
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type mysql \
    --host localhost --port 3306 --database testdb \
    --user root --password pass \
    --truncate \
    --mode insert-only \
    --batch-size 100 \
    --thread-count 50
```

### Result Export

```bash
# JSON format
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test --password pass \
    --truncate \
    --output-format json \
    --output-file results/test_result.json

# CSV format
java -jar java/target/multi-db-load-tester-0.2.6.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test --password pass \
    --truncate \
    --output-format csv \
    --output-file results/test_result.csv
```

---

## Command-Line Options

### Required Options

| Option       | Description                                                                         |
| ------------ | ----------------------------------------------------------------------------------- |
| `--db-type`  | Database type (oracle, postgresql, mysql, sqlserver, tibero, db2, singlestore)      |
| `--host`     | Database host                                                                       |
| `--user`     | Username                                                                            |
| `--password` | Password                                                                            |

### Connection Options

| Option             | Description                                                |
| ------------------ | ---------------------------------------------------------- |
| `--port`           | Port number                                                |
| `--database`       | Database name (PostgreSQL, MySQL, SQL Server, DB2)         |
| `--sid`            | SID (Oracle, Tibero) - SID format connection               |
| `--service-name`   | Service Name (Oracle, Tibero) - Service Name format connection |
| `--jdbc-url`       | Direct JDBC URL specification (ignores host/port/database, etc.) |

### Test Options

| Option              | Default | Description                                                               |
| ------------------- | ------- | ------------------------------------------------------------------------- |
| `--thread-count`    | 100     | Number of worker threads                                                  |
| `--test-duration`   | 300     | Test duration (seconds)                                                   |
| `--mode`            | full    | Operation mode                                                            |
| `--truncate`        | false   | TRUNCATE table before test (delete data, reset sequence/ID) - **Recommended** |

> **Recommended**: Always use the `--truncate` option for consistent test results.

### Warmup and Load Control

| Option           | Default | Description                                             |
| ---------------- | ------- | ------------------------------------------------------- |
| `--warmup`       | 30      | Warmup period (seconds), set to 0 to start without warmup |
| `--ramp-up`      | 0       | Gradual load increase period (seconds)                  |
| `--target-tps`   | 0       | Target TPS limit (0=unlimited)                          |
| `--batch-size`   | 1       | Batch INSERT size                                       |

### HikariCP Pool Settings

| Option                       | Default | Description                                  |
| ---------------------------- | ------- | -------------------------------------------- |
| `--min-pool-size`            | 100     | Minimum pool size                            |
| `--max-pool-size`            | 200     | Maximum pool size                            |
| `--max-lifetime`             | 1800    | Connection maximum lifetime (seconds, 30 min)|
| `--leak-detection-threshold` | 60      | Leak detection threshold (seconds)           |
| `--idle-check-interval`      | 30      | Idle connection check interval (seconds)     |
| `--idle-timeout`             | 30      | Idle connection removal time (seconds)       |
| `--keepalive-time`           | 30      | Idle connection validation interval (seconds, minimum 30 seconds) |

> **Note**: HikariCP automatically disables `keepalive-time` if it is less than 30 seconds. It must be set to 30 seconds or more.

#### idle-timeout Setting Impact

| Direction | Advantages                                                               | Disadvantages                                                            |
| --------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------ |
| **Increase** | Increased connection reuse, reduced connection creation cost, improved peak time response | Increased memory usage, DB session resource occupation, possibility of dead connections remaining |
| **Decrease** | Resource efficiency, quick DB session return                             | Increased connection recreation frequency, performance degradation during traffic fluctuations |

**Recommended values by environment:**

| Environment                | idle-timeout | Reason                        |
| -------------------------- | ------------ | ----------------------------- |
| High traffic variation     | 300~600 sec  | Maintain connections for peaks |
| Stable traffic             | 60~120 sec   | Resource efficiency           |
| DB session limited environment | 30~60 sec | DB resource conservation      |
| Load testing               | 600+ sec     | Minimize connection recreation overhead |

> **Note**: `idle-timeout` only applies to idle connections exceeding `minPoolSize`.

#### keepalive-time Setting Impact

| Direction | Advantages                                                     | Disadvantages                                         |
| --------- | -------------------------------------------------------------- | ----------------------------------------------------- |
| **Increase** | Reduced DB load (fewer validation queries), reduced network traffic | Delayed dead connection detection, HA failover response delay |
| **Decrease** | Quick failure detection, quick recovery in HA environment      | Increased DB load, validation query overhead          |

**Recommended values by environment:**

| Environment          | keepalive-time              | Reason                |
| -------------------- | --------------------------- | --------------------- |
| HA/Failover environment | 30 sec (default)         | Quick failure detection needed |
| Stable single DB     | 60~120 sec                  | Reduce DB load        |
| Firewall environment | Half of firewall timeout or less | Prevent session disconnections |

> **Note**: Since the current implementation performs `Connection.isValid()` validation at the worker level, the risk of using dead connections in transactions is low even if `keepalive-time` is increased.

#### Recommended Relationship Between Settings

```
idle-timeout > keepalive-time (recommended)
```

| Setting          | Role                          | Recommended Value         |
| ---------------- | ----------------------------- | ------------------------- |
| `keepalive-time` | Idle connection validation interval | 30 sec              |
| `idle-timeout`   | Idle connection removal time  | keepalive-time × 2~3 or more |
| `max-lifetime`   | Connection maximum lifetime   | 1800 sec (30 min)         |

### Result Output

| Option                    | Default | Description                        |
| ----------------------- | ------- | ---------------------------------- |
| `--output-format`       | none    | Result format (csv, json)          |
| `--output-file`         | -       | Result file path                   |
| `--monitor-interval`    | 1.0     | Monitor output interval (seconds)  |
| `--sub-second-interval` | 100     | Sub-second measurement window (ms) |

### Other

| Option            | Description                      |
| ----------------- | -------------------------------- |
| `--print-ddl`     | Print DDL script and exit        |
| `-h, --help`      | Print help                       |
| `-v, --version`   | Print version                    |

---

## Execution Scripts

Database-specific execution scripts are provided:

```bash
cd java

# Grant permissions
chmod +x *.sh

# Run
./run_oracle_test.sh
./run_postgresql_test.sh
./run_mysql_test.sh
./run_sqlserver_test.sh
./run_tibero_test.sh
./run_db2_test.sh
```

Configurable via environment variables:

```bash
export ORACLE_HOST=192.168.0.100
export ORACLE_PORT=1521
export ORACLE_SID=ORCL
export ORACLE_USER=test_user
export ORACLE_PASSWORD=test_pass
export THREAD_COUNT=200
export TEST_DURATION=300

./run_oracle_test.sh
```

---

## Monitoring Output Example

### Test Start (Schema Setup)

```
# First run - schema creation
Setting up database schema...
Tibero schema created successfully

# Re-run - reuse existing schema
Setting up database schema...
Tibero schema already exists - reusing existing schema
  (DROP objects manually to recreate, or use --truncate to clear data only)
```

### During Warmup Period

```
================================================================================
Warmup period: 30 seconds (Avg TPS will be calculated after warmup)
Total test duration: 30 seconds (warmup) + 120 seconds (measurement) = 150 seconds
================================================================================
[Monitor] [WARMUP]  TXN: 1,234 | INS: 1,234 | SEL: 1,234 | UPD: 1,234 | DEL: 1,234 | ERR: 0 | Avg TPS: - | RT TPS: 1234.00 | Lat(p50/p95/p99): 1.2/2.1/3.5ms | Pool: 95/100
```

### After Warmup (Measurement Period)

```
================================================================================
[Monitor] *** WARMUP COMPLETED *** Starting measurement phase...
================================================================================
[Monitor] [RUNNING] TXN: 1,523 | INS: 1,523 | SEL: 1,523 | UPD: 1,523 | DEL: 1,523 | ERR: 0 | Avg TPS: 1507.67 | RT TPS: 1523.00 | Lat(p50/p95/p99): 2.3/4.5/8.2ms | Pool: 95/100
```

### Without Warmup (--warmup 0)

```
================================================================================
No warmup period. Test duration: 60 seconds
================================================================================
[Monitor] [RUNNING] TXN: 1,523 | INS: 1,523 | SEL: 1,523 | UPD: 1,523 | DEL: 1,523 | ERR: 0 | Avg TPS: 1507.67 | RT TPS: 1523.00 | Lat(p50/p95/p99): 2.3/4.5/8.2ms | Pool: 95/100
```

### Output Item Description

| Item                  | Description                                                       |
| --------------------- | ----------------------------------------------------------------- |
| `[WARMUP]`            | Displayed during warmup period                                    |
| `[RUNNING]`           | Displayed during measurement period (after warmup or without warmup) |
| `TXN/INS/SEL/UPD/DEL` | Change amount (delta) during the interval                         |
| `ERR`                 | Number of errors during the interval                              |
| `Avg TPS`             | Average TPS (Post-Warmup TPS if warmup exists, overall average otherwise) |
| `RT TPS`              | Real-time TPS (transactions in the last 1 second)                 |
| `Lat(p50/p95/p99)`    | Response time percentiles (milliseconds)                          |
| `Pool`                | Connection pool status (active/total)                             |

> **Note**: When using `--mode full`, INSERT, SELECT, UPDATE, and DELETE are all performed.

---

## Troubleshooting

### HikariCP Connection Pool Errors

- Ensure `--max-pool-size` is smaller than the database `max_connections` setting
- Check network connection and firewall settings

### DB Restart Recovery

**Improvements in v0.2:**

- Workers validate with `Connection.isValid()` before using connections
- When invalid connections are detected, immediately acquire new connections (up to 3 retries)
- Fast connection recreation on consecutive errors (threshold: 2 times, wait: 100ms)

**HikariCP keepaliveTime Limitation:**

- HikariCP **disables** `keepalive-time` if it is **less than 30 seconds**
- If set below 30 seconds, a warning message is displayed and it is ignored:
  ```
  HikariPool-TIBERO - keepaliveTime is less than 30000ms, disabling it.
  ```
- Set to 30 seconds or more if idle connection validation is needed

**How it works:**

1. Worker calls `connection.isValid(2)` before each transaction (2-second timeout)
2. If invalid, release connection and acquire new connection
3. Validate new connection before use (up to 3 retries)
4. Quickly switch to new connections after DB restart

### Leak Detection Warning

- Workers acquire and release a connection for each transaction, so HikariCP's "Apparent connection leak detected" warning should not appear during normal operation.
- If the warning still occurs, it indicates an actual long-running transaction or connection leak; increase `--leak-detection-threshold` or investigate the database/network.

### MySQL Pool Size Limit

- MySQL is limited to a maximum of 32 connections by default
- MySQL server `max_connections` setting also needs to be adjusted

### OutOfMemoryError

- Increase JVM heap size: `-Xmx4g`
- Decrease thread count

---

## Project Structure

```
.
├── README.md                          # This file
└── java/                              # Java source
    ├── pom.xml                        # Maven build configuration
    ├── build.sh                       # Build script
    ├── run_*_test.sh                  # Execution scripts
    └── src/main/java/com/loadtest/
        ├── MultiDBLoadTester.java     # Main class
        ├── DatabaseAdapter.java       # DB adapter interface
        ├── AbstractDatabaseAdapter.java # HikariCP-based abstract class
        ├── OracleAdapter.java         # Oracle adapter
        ├── PostgreSQLAdapter.java     # PostgreSQL adapter
        ├── MySQLAdapter.java          # MySQL adapter
        ├── SQLServerAdapter.java      # SQL Server adapter
        ├── TiberoAdapter.java         # Tibero adapter
        ├── DB2Adapter.java            # IBM DB2 adapter
        ├── SingleStoreAdapter.java    # SingleStore adapter
        ├── LoadTestWorker.java        # Load test worker
        ├── MonitorThread.java         # Monitoring thread
        ├── PerformanceCounter.java    # Performance counter
        ├── RateLimiter.java           # Rate Limiter
        ├── ResultExporter.java        # Result exporter
        ├── DatabaseConfig.java        # DB configuration
        └── WorkMode.java              # Work mode
```

---

## Version History

### v0.2.5 (2025-02-20)

**Tibero 6 JDBC Driver Auto-Fallback**

- Automatic detection of `JDBC-12030` error when connecting to Tibero 6 server, with automatic fallback to built-in Tibero 6 driver
- Embed `tibero6-jdbc.jar` as a resource in the fat JAR (as a JAR file, not a class)
- Automatic connection to both Tibero 6/7 servers without `--driver-path`
- Existing `--driver-path` option remains unchanged (explicit external driver takes priority)
- HikariCP `DriverDataSource` classloader conflict workaround: `SimpleDriverDataSource` wrapper to use drivers loaded from child-first classloader directly
- Automatic conversion of service name URL (`@//host:port/service`) not supported by Tibero 6 to SID format (`@host:port:sid`)
- No impact on other DBs (Oracle, PostgreSQL, MySQL, etc.)
- Verified: 355 TPS, 0 errors against Tibero 6 server

### v0.2.4 (2025-01-06)

**Oracle/Tibero Service Name Connection Support**

- Added `--service-name` option: Service Name format connection support for Oracle, Tibero databases
- Can be used separately from existing `--sid` option
- JDBC URL formats:
  - With `--sid`: `jdbc:oracle:thin:@host:port:SID`
  - With `--service-name`: `jdbc:oracle:thin:@//host:port/SERVICE_NAME`
- Tibero supports the same formats

**Direct JDBC URL Specification Support**

- Added `--jdbc-url` option: Direct JDBC URL specification for all databases
- Supports complex connection strings such as RAC/SCAN, TNS, SSL
- `--host`, `--port`, `--database`, `--sid`, `--service-name` are ignored when using `--jdbc-url`
- Supported databases: Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2, SingleStore

**Usage Examples:**
```bash
# SID format (existing method)
java -jar multi-db-load-tester-0.2.6.jar --db-type oracle --host localhost \
    --port 1521 --sid ORCL --user test --password pass

# Service Name format
java -jar multi-db-load-tester-0.2.6.jar --db-type oracle --host localhost \
    --port 1521 --service-name XEPDB1 --user test --password pass

# Direct JDBC URL specification (RAC/SCAN environment)
java -jar multi-db-load-tester-0.2.6.jar --db-type oracle \
    --jdbc-url "jdbc:oracle:thin:@//scan-ip:1521/SERVICE" \
    --user test --password pass
```

### v0.2.3 (2025-12-29)

**SingleStore Database Support Added**

- Integrated SingleStore JDBC driver (1.2.1)
- Added `SingleStoreAdapter.java` (based on MySQL-compatible protocol)
- Default connection pool size limit: 32
- Added `run_singlestore_test.sh` execution script
- Expanded supported databases from 6 to 7

### v0.2.2 (2025-12-19)

**Improved Result Statistics Output**

- `Results` `Average TPS` output based on **excluding Warmup (Post-Warmup)**
- `Latency` statistics aggregated/output based on **excluding Warmup**

### v0.2 (2025-12-15)

**Table TRUNCATE Option Added**

- `--truncate`: Delete table data and reset ID/sequence before testing
  - Oracle, Tibero, DB2: Sequence DROP/CREATE to restart from 1
  - PostgreSQL: `TRUNCATE ... RESTART IDENTITY`
  - MySQL: AUTO_INCREMENT auto reset
  - SQL Server: IDENTITY auto reset

**HikariCP Connection Management Improvements**

- Added idle connection management options:
  - `--idle-timeout`: Idle connection removal time (default: 30 seconds)
  - `--keepalive-time`: Idle connection validation interval (default: 30 seconds, minimum 30 seconds)
- Display Idle Timeout, Keepalive Time in startup settings output

**DB Restart Recovery Improvements**

- Workers validate with `Connection.isValid()` before using connections
- When invalid connections are detected, immediately acquire new connections (up to 3 retries)
- Decreased consecutive error threshold (5 → 2) and reduced wait time (500ms → 100ms)
- Supports quick automatic recovery after DB restart

**Monitoring Output Improvements**

- Monitoring output changed from cumulative values to **interval-based delta (change amount)**
  - TXN, INS, SEL, UPD, DEL, ERR: Display change amount compared to previous interval
- Added status indicators:
  - `[WARMUP]`: During warmup period
  - `[RUNNING]`: During measurement period (after warmup)
- Improved warmup period information logging:
  - At start: Output warmup period and total test duration
  - At end: Output `*** WARMUP COMPLETED ***` message
- Avg TPS calculated **excluding warmup period**
  - During warmup: Display `Avg TPS: -`
  - After warmup: Display Post-Warmup TPS
  - Without warmup: Display overall average TPS

**Default Value Changes**

- `--warmup` default: `0` → `30` (30-second warmup)

**Schema Management Improvements**

- **Reuse without deletion** when tables/sequences already exist
- Message output when existing schema detected:
  ```
  Tibero schema already exists - reusing existing schema
    (DROP objects manually to recreate, or use --truncate to clear data only)
  ```
- Applied to all DB adapters: Oracle, PostgreSQL, MySQL, SQL Server, Tibero, DB2

**Bug Fixes**

- Fixed Avg TPS to display normally when using `--warmup 0`

### v0.1 (2025-12-14)

**Initial Release**

- 6 database support: Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2
- HikariCP-based high-performance connection pooling
- 6 operation modes: full, insert-only, select-only, update-only, delete-only, mixed
- Warmup, Ramp-up, Rate Limiting features
- Batch INSERT support
- CSV/JSON result export
- Real-time monitoring (TPS, latency P50/P95/P99)

---

## License

MIT License
