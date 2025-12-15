# Multi-Database Load Tester v0.2

Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2를 지원하는 고성능 멀티스레드 데이터베이스 부하 테스트 도구 (HikariCP 기반)

## 주요 특징

- **6개 데이터베이스 지원**: Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2
- **HikariCP 커넥션 풀**: 고성능 JDBC 커넥션 풀링
- **고성능 멀티스레딩**: 최대 1000개 동시 세션 지원
- **6가지 작업 모드**: full, insert-only, select-only, update-only, delete-only, mixed
- **1초 이내 트랜잭션 측정**: Sub-second TPS 실시간 모니터링
- **레이턴시 측정**: P50/P95/P99 응답시간 통계
- **워밍업 기간**: 통계 제외 워밍업 지원
- **점진적 부하 증가**: Ramp-up 기능
- **TPS 제한**: Token Bucket 기반 Rate Limiting
- **배치 INSERT**: 대량 데이터 삽입 최적화
- **결과 내보내기**: CSV/JSON 형식 지원
- **Graceful Shutdown**: Ctrl+C 안전 종료
- **Leak Detection**: HikariCP 내장 커넥션 누수 감지

---

## 사전 요구사항 (Prerequisites)

### 1. Java Development Kit (JDK) 17+

이 도구는 Java 17 이상이 필요합니다.

#### 버전 확인

```bash
java -version
# openjdk version "17.0.x" 또는 이상 버전 필요
```

#### 설치 방법

**macOS (Homebrew)**
```bash
brew install openjdk@17
# 또는 최신 LTS 버전
brew install openjdk@21

# 환경 변수 설정
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH
```

**Ubuntu/Debian**
```bash
sudo apt update
sudo apt install openjdk-17-jdk

# 환경 변수 설정 (~/.bashrc 또는 ~/.zshrc에 추가)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

**RHEL/CentOS/Rocky Linux**
```bash
sudo yum install java-17-openjdk-devel
# 또는
sudo dnf install java-17-openjdk-devel

# 환경 변수 설정
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

**Windows**
1. [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 또는 [Adoptium](https://adoptium.net/) 에서 JDK 17+ 다운로드
2. 설치 후 환경 변수 설정:
   - `JAVA_HOME`: JDK 설치 경로 (예: `C:\Program Files\Java\jdk-17`)
   - `PATH`에 `%JAVA_HOME%\bin` 추가

---

### 2. Apache Maven 3.6+

프로젝트 빌드를 위해 Maven이 필요합니다.

#### 버전 확인

```bash
mvn -version
# Apache Maven 3.6.x 또는 이상 버전 필요
```

#### 설치 방법

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
# 또는
sudo dnf install maven
```

**Windows**
1. [Apache Maven](https://maven.apache.org/download.cgi) 에서 Binary zip 다운로드
2. 원하는 위치에 압축 해제 (예: `C:\Program Files\Apache\maven`)
3. 환경 변수 설정:
   - `MAVEN_HOME`: Maven 설치 경로
   - `PATH`에 `%MAVEN_HOME%\bin` 추가

---

### 3. 데이터베이스 요구사항

#### 지원 데이터베이스 버전

| 데이터베이스 | 최소 버전 | 권장 버전 | 기본 포트 |
|-------------|----------|----------|----------|
| Oracle | 19c | 21c+ | 1521 |
| PostgreSQL | 11 | 15+ | 5432 |
| MySQL | 5.7 | 8.0+ | 3306 |
| SQL Server | 2016 | 2019+ | 1433 |
| Tibero | 6 | 7 | 8629 |
| IBM DB2 | 11.1 | 11.5+ | 50000 |

#### 데이터베이스 사용자 권한

테스트를 실행하려면 다음 권한이 필요합니다:

**Oracle**
```sql
-- 테스트 사용자 생성 (SYS 또는 SYSTEM으로 접속)
CREATE USER test_user IDENTIFIED BY test_pass;
GRANT CONNECT, RESOURCE TO test_user;
GRANT CREATE TABLE, CREATE SEQUENCE TO test_user;
GRANT UNLIMITED TABLESPACE TO test_user;
```

**PostgreSQL**
```sql
-- 테스트 데이터베이스 및 사용자 생성
CREATE USER test_user WITH PASSWORD 'test_pass';
CREATE DATABASE testdb OWNER test_user;
GRANT ALL PRIVILEGES ON DATABASE testdb TO test_user;
```

**MySQL**
```sql
-- 테스트 데이터베이스 및 사용자 생성
CREATE DATABASE testdb;
CREATE USER 'test_user'@'%' IDENTIFIED BY 'test_pass';
GRANT ALL PRIVILEGES ON testdb.* TO 'test_user'@'%';
FLUSH PRIVILEGES;

-- max_connections 설정 확인 (높은 스레드 수 사용 시)
SHOW VARIABLES LIKE 'max_connections';
-- 필요시 증가: SET GLOBAL max_connections = 500;
```

**SQL Server**
```sql
-- 테스트 데이터베이스 및 사용자 생성
CREATE DATABASE testdb;
USE testdb;
CREATE LOGIN test_user WITH PASSWORD = 'test_pass';
CREATE USER test_user FOR LOGIN test_user;
ALTER ROLE db_owner ADD MEMBER test_user;
```

**Tibero**
```sql
-- 테스트 사용자 생성
CREATE USER test_user IDENTIFIED BY test_pass;
GRANT CONNECT, RESOURCE TO test_user;
GRANT CREATE TABLE, CREATE SEQUENCE TO test_user;
```

**IBM DB2**
```sql
-- 테스트 데이터베이스 및 사용자 생성
CREATE DATABASE testdb;
CONNECT TO testdb;
CREATE USER test_user;
GRANT CONNECT, CREATETAB, IMPLICIT_SCHEMA ON DATABASE TO USER test_user;
```

#### 데이터베이스 서버 설정

높은 동시성 테스트를 위해 데이터베이스 서버 설정 조정이 필요할 수 있습니다:

**Oracle**
```sql
-- 최대 세션/프로세스 수 확인
SHOW PARAMETER sessions;
SHOW PARAMETER processes;

-- 증가 필요 시 (재시작 필요)
ALTER SYSTEM SET sessions=1000 SCOPE=SPFILE;
ALTER SYSTEM SET processes=500 SCOPE=SPFILE;
```

**PostgreSQL** (`postgresql.conf`)
```ini
max_connections = 500
shared_buffers = 256MB
```

**MySQL** (`my.cnf` 또는 `my.ini`)
```ini
[mysqld]
max_connections = 500
max_user_connections = 0
```

**SQL Server**
```sql
-- 기본적으로 32,767 연결 지원
-- 메모리 설정 확인
EXEC sp_configure 'max server memory';
```

---

### 4. 네트워크 요구사항

#### 방화벽 설정

테스트 클라이언트에서 데이터베이스 서버로의 접속을 위해 해당 포트가 열려 있어야 합니다:

| 데이터베이스 | 포트 | 방화벽 명령 (Linux) |
|-------------|------|-------------------|
| Oracle | 1521 | `firewall-cmd --add-port=1521/tcp --permanent` |
| PostgreSQL | 5432 | `firewall-cmd --add-port=5432/tcp --permanent` |
| MySQL | 3306 | `firewall-cmd --add-port=3306/tcp --permanent` |
| SQL Server | 1433 | `firewall-cmd --add-port=1433/tcp --permanent` |
| Tibero | 8629 | `firewall-cmd --add-port=8629/tcp --permanent` |
| IBM DB2 | 50000 | `firewall-cmd --add-port=50000/tcp --permanent` |

#### 연결 테스트

```bash
# 포트 연결 테스트
nc -zv <호스트> <포트>
# 또는
telnet <호스트> <포트>

# 예시
nc -zv 192.168.0.100 1521
```

---

### 5. 시스템 리소스 요구사항

#### 최소 사양

| 항목 | 최소 | 권장 |
|------|------|------|
| CPU | 2코어 | 4코어+ |
| RAM | 2GB | 8GB+ |
| 디스크 | 1GB (설치용) | SSD 권장 |

#### JVM 메모리 설정

고부하 테스트 시 JVM 힙 메모리 조정이 필요합니다:

```bash
# 기본 실행 (2GB 힙)
java -Xms1g -Xmx2g -jar multi-db-load-tester-0.2.jar ...

# 고부하 테스트 (4GB 힙, 500+ 스레드)
java -Xms2g -Xmx4g -jar multi-db-load-tester-0.2.jar ...

# 초고부하 테스트 (8GB 힙, 1000+ 스레드)
java -Xms4g -Xmx8g -XX:+UseG1GC -jar multi-db-load-tester-0.2.jar ...
```

#### 스레드 수에 따른 권장 리소스

| 스레드 수 | RAM | JVM 힙 | 커넥션 풀 |
|----------|-----|--------|----------|
| ~100 | 4GB | 2GB | 100-150 |
| ~200 | 8GB | 4GB | 200-250 |
| ~500 | 16GB | 8GB | 500-600 |
| ~1000 | 32GB | 16GB | 1000-1200 |

#### 파일 디스크립터 제한 (Linux/macOS)

고부하 테스트 시 파일 디스크립터 제한 증가가 필요할 수 있습니다:

```bash
# 현재 제한 확인
ulimit -n

# 임시 증가
ulimit -n 65535

# 영구 설정 (/etc/security/limits.conf)
*    soft    nofile    65535
*    hard    nofile    65535
```

---

### 6. JDBC 드라이버 (자동 포함)

모든 JDBC 드라이버가 `java/jre/` 디렉토리에 포함되어 있으며, 빌드 시 자동으로 JAR에 포함됩니다.

#### 포함된 드라이버

| 데이터베이스 | 드라이버 파일 | 위치 |
|-------------|-------------|------|
| Oracle | ojdbc10.jar | `java/jre/oracle/` |
| PostgreSQL | postgresql-42.2.9.jar | `java/jre/postgresql/` |
| MySQL | mysql-connector-j-9.5.0.jar | `java/jre/mysql/` |
| SQL Server | mssql-jdbc-13.2.1.jre11.jar | `java/jre/sqlserver/` |
| Tibero | tibero7-jdbc.jar | `java/jre/tibero/` |
| IBM DB2 | jcc-12.1.3.0.jar | `java/jre/db2/` |

#### 빌드 방법

```bash
cd java
./build.sh
```

빌드 스크립트가 자동으로:
1. 로컬 JDBC 드라이버를 Maven 로컬 저장소에 설치
2. 모든 드라이버를 포함한 실행 가능한 JAR 생성

> **Note**: 모든 JDBC 드라이버가 `java/jre/` 디렉토리에 포함되어 있습니다.

---

## 빠른 시작

### 1. 빌드

```bash
cd java
./build.sh
```

또는:

```bash
cd java
mvn clean package -DskipTests
```

### 2. 실행

```bash
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test_user --password test_pass \
    --thread-count 100 --test-duration 60 \
    --warmup 30
```

> **Note**: `--warmup 30` 옵션은 처음 30초 동안을 워밍업 기간으로 설정하여 Avg TPS 계산에서 제외합니다.

### 3. 도움말

```bash
java -jar java/target/multi-db-load-tester-0.2.jar --help
```

---

## 작업 모드 (--mode)

| 모드 | 설명 | 사용 사례 |
|------|------|----------|
| `full` | INSERT → SELECT → UPDATE → DELETE (기본값) | 전체 CRUD 사이클 검증 |
| `insert-only` | INSERT → COMMIT만 | 최대 쓰기 처리량 측정 |
| `select-only` | SELECT만 | 읽기 성능 측정 |
| `update-only` | UPDATE → COMMIT | 업데이트 성능 측정 |
| `delete-only` | DELETE → COMMIT | 삭제 성능 측정 |
| `mixed` | INSERT/UPDATE/DELETE 혼합 (60:20:15:5) | 실제 워크로드 시뮬레이션 |

### ⚠️ 주의: update-only / delete-only / select-only 모드 사용 시

`update-only`, `delete-only`, `select-only` 모드는 **기존 데이터가 필요**합니다.

기본적으로 테스트 시작 시 `setupSchema()`가 실행되어 **테이블이 DROP 후 재생성**됩니다.
따라서 기존 데이터를 유지하려면 반드시 `--skip-schema-setup` 옵션을 사용해야 합니다.

#### 올바른 사용 예시

```bash
# 1단계: insert-only로 데이터 삽입 (테이블 생성됨)
java -jar target/multi-db-load-tester-0.2.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --sid ORCL \
    --user test --password pass \
    --mode insert-only \
    --test-duration 60 --warmup 10

# 2단계: update-only 실행 (--skip-schema-setup 필수!)
java -jar target/multi-db-load-tester-0.2.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --sid ORCL \
    --user test --password pass \
    --mode update-only \
    --skip-schema-setup \
    --test-duration 60 --warmup 10

# 3단계: delete-only 실행 (--skip-schema-setup 필수!)
java -jar target/multi-db-load-tester-0.2.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --sid ORCL \
    --user test --password pass \
    --mode delete-only \
    --skip-schema-setup \
    --test-duration 60 --warmup 10
```

> **Note**: `--skip-schema-setup` 옵션 없이 `update-only` 또는 `delete-only`를 실행하면 테이블이 비어있어 작업이 수행되지 않습니다.

---

## 데이터베이스별 예제

### Oracle

```bash
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --sid ORCL \
    --user test_user --password pass \
    --thread-count 200 --test-duration 300 \
    --warmup 30
```

### PostgreSQL

```bash
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type postgresql \
    --host localhost --port 5432 --database testdb \
    --user test_user --password pass \
    --thread-count 200 --test-duration 300 \
    --warmup 30
```

### MySQL

```bash
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type mysql \
    --host localhost --port 3306 --database testdb \
    --user root --password pass \
    --thread-count 50 --test-duration 300 \
    --warmup 30
```

> **Note**: MySQL의 커넥션 풀 크기는 기본적으로 32개로 제한됩니다.

### SQL Server

```bash
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type sqlserver \
    --host localhost --port 1433 --database testdb \
    --user sa --password pass \
    --thread-count 200 --test-duration 300 \
    --warmup 30
```

### Tibero

```bash
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type tibero \
    --host 192.168.0.140 --port 8629 --sid tibero \
    --user test_user --password pass \
    --thread-count 200 --test-duration 300 \
    --warmup 30
```

### IBM DB2

```bash
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type db2 \
    --host localhost --port 50000 --database testdb \
    --user db2inst1 --password pass \
    --thread-count 200 --test-duration 300 \
    --warmup 30
```

---

## 고급 기능

### 워밍업 + Ramp-up + Rate Limiting

```bash
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type postgresql \
    --host localhost --port 5432 --database testdb \
    --user test --password pass \
    --warmup 30 \
    --ramp-up 60 \
    --target-tps 5000 \
    --thread-count 200 --test-duration 300
```

### 배치 INSERT

```bash
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type mysql \
    --host localhost --port 3306 --database testdb \
    --user root --password pass \
    --mode insert-only \
    --batch-size 100 \
    --thread-count 50
```

### 결과 내보내기

```bash
# JSON 형식
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test --password pass \
    --output-format json \
    --output-file results/test_result.json

# CSV 형식
java -jar java/target/multi-db-load-tester-0.2.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test --password pass \
    --output-format csv \
    --output-file results/test_result.csv
```

---

## 명령행 옵션

### 필수 옵션

| 옵션 | 설명 |
|------|------|
| `--db-type` | 데이터베이스 타입 (oracle, postgresql, mysql, sqlserver, tibero, db2) |
| `--host` | 데이터베이스 호스트 |
| `--user` | 사용자명 |
| `--password` | 비밀번호 |

### 연결 옵션

| 옵션 | 설명 |
|------|------|
| `--port` | 포트 번호 |
| `--database` | 데이터베이스명 (PostgreSQL, MySQL, SQL Server, DB2) |
| `--sid` | SID/서비스명 (Oracle, Tibero) |

### 테스트 옵션

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `--thread-count` | 100 | 워커 스레드 수 |
| `--test-duration` | 300 | 테스트 시간 (초) |
| `--mode` | full | 작업 모드 |
| `--skip-schema-setup` | false | 스키마 생성 스킵 |

### 워밍업 및 부하 제어

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `--warmup` | 0 | 워밍업 기간 (초) |
| `--ramp-up` | 0 | 점진적 부하 증가 기간 (초) |
| `--target-tps` | 0 | 목표 TPS 제한 (0=무제한) |
| `--batch-size` | 1 | 배치 INSERT 크기 |

### HikariCP 풀 설정

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `--min-pool-size` | 100 | 최소 풀 크기 |
| `--max-pool-size` | 200 | 최대 풀 크기 |
| `--max-lifetime` | 1800 | 커넥션 최대 수명 (초, 30분) |
| `--leak-detection-threshold` | 60 | Leak 감지 임계값 (초) |
| `--idle-check-interval` | 30 | 유휴 커넥션 검사 주기 (초) |

### 결과 출력

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `--output-format` | none | 결과 형식 (csv, json) |
| `--output-file` | - | 결과 파일 경로 |
| `--monitor-interval` | 1.0 | 모니터 출력 간격 (초) |
| `--sub-second-interval` | 100 | Sub-second 측정 윈도우 (ms) |

### 기타

| 옵션 | 설명 |
|------|------|
| `--print-ddl` | DDL 스크립트 출력 후 종료 |
| `-h, --help` | 도움말 출력 |
| `-v, --version` | 버전 출력 |

---

## 실행 스크립트

각 데이터베이스별 실행 스크립트가 제공됩니다:

```bash
cd java

# 권한 부여
chmod +x *.sh

# 실행
./run_oracle_test.sh
./run_postgresql_test.sh
./run_mysql_test.sh
./run_sqlserver_test.sh
./run_tibero_test.sh
./run_db2_test.sh
```

환경 변수로 설정 가능:

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

## 모니터링 출력 예시

### Warmup 기간 중
```
[Monitor] [WARMUP] TXN: 1,234 | INS: 1,234 | SEL: 1,234 | UPD: 1,234 | DEL: 1,234 | ERR: 0 | Avg TPS: - | RT TPS: 1234.00 | Lat(p95/p99): 2.1/3.5ms | Pool: 95/100
```

### Warmup 종료 후
```
[Monitor] TXN: 1,523 | INS: 1,523 | SEL: 1,523 | UPD: 1,523 | DEL: 1,523 | ERR: 0 | Avg TPS: 1507.67 | RT TPS: 1523.00 | Lat(p95/p99): 4.5/8.2ms | Pool: 95/100
```

### 출력 항목 설명

| 항목 | 설명 |
|------|------|
| `[WARMUP]` | 워밍업 기간 중 표시 |
| `TXN/INS/SEL/UPD/DEL` | 해당 구간(interval) 동안의 변화량 (delta) |
| `ERR` | 해당 구간 동안의 에러 수 |
| `Avg TPS` | Warmup 이후의 평균 TPS (Warmup 중에는 `-` 표시) |
| `RT TPS` | 실시간 TPS (최근 1초간 트랜잭션 수) |
| `Lat(p95/p99)` | 응답시간 백분위수 (밀리초) |
| `Pool` | 커넥션 풀 상태 (활성/전체) |

> **Note**: `--mode full` 사용 시 INSERT, SELECT, UPDATE, DELETE가 모두 수행됩니다.

---

## 문제 해결

### HikariCP 커넥션 풀 오류
- `--max-pool-size` 값이 데이터베이스 `max_connections` 설정보다 작은지 확인
- 네트워크 연결 및 방화벽 설정 확인

### Leak Detection 경고
- 트랜잭션 처리 시간이 `--leak-detection-threshold`를 초과하는 경우
- 장시간 트랜잭션이 예상되는 경우 임계값 증가

### MySQL 풀 크기 제한
- MySQL은 기본적으로 최대 32개 커넥션으로 제한됨
- MySQL 서버의 `max_connections` 설정도 함께 조정 필요

### OutOfMemoryError
- JVM 힙 크기 증가: `-Xmx4g`
- 스레드 수 감소

---

## 프로젝트 구조

```
.
├── README.md                          # 이 파일
└── java/                              # Java 소스
    ├── pom.xml                        # Maven 빌드 설정
    ├── build.sh                       # 빌드 스크립트
    ├── run_*_test.sh                  # 실행 스크립트
    └── src/main/java/com/loadtest/
        ├── MultiDBLoadTester.java     # 메인 클래스
        ├── DatabaseAdapter.java       # DB 어댑터 인터페이스
        ├── AbstractDatabaseAdapter.java # HikariCP 기반 추상 클래스
        ├── OracleAdapter.java         # Oracle 어댑터
        ├── PostgreSQLAdapter.java     # PostgreSQL 어댑터
        ├── MySQLAdapter.java          # MySQL 어댑터
        ├── SQLServerAdapter.java      # SQL Server 어댑터
        ├── TiberoAdapter.java         # Tibero 어댑터
        ├── DB2Adapter.java            # IBM DB2 어댑터
        ├── LoadTestWorker.java        # 부하 테스트 워커
        ├── MonitorThread.java         # 모니터링 스레드
        ├── PerformanceCounter.java    # 성능 카운터
        ├── RateLimiter.java           # Rate Limiter
        ├── ResultExporter.java        # 결과 내보내기
        ├── DatabaseConfig.java        # DB 설정
        └── WorkMode.java              # 작업 모드
```

---

## 버전 히스토리

### v0.2 (2025-12-15)

**모니터링 출력 개선**
- 모니터링 출력이 누적 값에서 **구간별 변화량(delta)**으로 변경
  - TXN, INS, SEL, UPD, DEL, ERR: 이전 interval 대비 변화량 표시
- Avg TPS가 **Warmup 기간을 제외**하고 계산
  - Warmup 중: `Avg TPS: -` 표시
  - Warmup 후: Warmup 제외 평균 TPS 표시

**버그 수정**
- `--warmup 0` (기본값) 사용 시에도 Avg TPS가 정상 계산되도록 수정

### v0.1 (2025-12-14)

**초기 릴리스**
- 6개 데이터베이스 지원: Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2
- HikariCP 기반 고성능 커넥션 풀링
- 6가지 작업 모드: full, insert-only, select-only, update-only, delete-only, mixed
- 워밍업, Ramp-up, Rate Limiting 기능
- 배치 INSERT 지원
- CSV/JSON 결과 내보내기
- 실시간 모니터링 (TPS, 레이턴시 P95/P99)

---

## 라이선스

MIT License
