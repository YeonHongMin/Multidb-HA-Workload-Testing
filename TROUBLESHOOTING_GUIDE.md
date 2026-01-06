# 문제 해결 가이드
# 멀티 데이터베이스 워크로드 테스트 도구

## 문서 정보
| 항목 | 설명 |
|------|-------------|
| 문서 버전 | 1.0 |
| 프로젝트 버전 | v0.2.4 |
| 최종 업데이트 | 2025-12-29 |
| 문서 관리자 | 개발팀 |

---

## 1. 개요

본 문서는 멀티 데이터베이스 워크로드 테스트 도구 사용 시 발생할 수 있는 일반적인 문제들과 해결 방법을 설명합니다.

---

## 2. 연결 관련 문제

### 2.1 데이터베이스 연결 실패

#### 증상
```
Error: Failed to create connection pool
SQLState: 08001
VendorCode: 0
Message: Connection refused
```

#### 원인
- 데이터베이스가 실행 중이지 않음
- 잘못된 호스트/포트 정보
- 방화벽 문제
- 네트워크 연결 불가

#### 해결 방법

1. **데이터베이스 상태 확인**
```bash
# PostgreSQL
psql -h localhost -p 5432 -U testuser -d testdb

# MySQL
mysql -h localhost -P 3306 -u testuser -p

# Oracle
sqlplus testuser/password@localhost:1521/XEPDB1

# SQL Server
sqlcmd -S localhost -U testuser -P password
```

2. **포트 확인**
```bash
netstat -an | findstr "5432"  # Windows
netstat -an | grep 5432       # Linux/macOS
telnet localhost 5432
```

3. **방화벽 확인**
```bash
# Linux
sudo ufw status
sudo ufw allow 5432

# Windows Firewall
netsh advfirewall firewall add rule name="PostgreSQL" dir=in action=allow protocol=TCP localport=5432
```

4. **연결 테스트**
```bash
java -jar multi-db-load-tester.jar --db-type postgresql \
    --host localhost --port 5432 --database testdb \
    --user testuser --password testpass --print-ddl
```

---

### 2.2 인증 실패

#### 증상
```
Error: FATAL: password authentication failed
SQLState: 28P01
```

#### 원인
- 잘못된 사용자명/비밀번호
- 사용자에게 데이터베이스 접근 권한 없음
- 인증 방식 불일치

#### 해결 방법

1. **자격 증명 확인**
```bash
# PostgreSQL
psql -U testuser -d testdb -h localhost

# MySQL
mysql -u testuser -p -h localhost
```

2. **사용자 권한 확인**
```sql
-- PostgreSQL
GRANT ALL PRIVILEGES ON DATABASE testdb TO testuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO testuser;

-- MySQL
GRANT ALL PRIVILEGES ON testdb.* TO 'testuser'@'%';
FLUSH PRIVILEGES;

-- Oracle
GRANT CONNECT, RESOURCE TO testuser;
```

3. **인증 방식 확인 (PostgreSQL)**
```sql
-- pg_hba.conf 수정
# TYPE  DATABASE  USER  ADDRESS  METHOD
host    all       all   all       md5
```

---

### 2.3 커넥션 풀 고갈

#### 증상
```
Connection timeout after 30000ms (waiting for pool)
HikariPool-1 - Connection is not available
```

#### 원인
- `--max-pool-size`가 너무 낮음
- 너무 많은 스레드 실행
- 커넥션 누수 발생

#### 해결 방법

1. **풀 크기 증가**
```bash
java -jar multi-db-load-tester.jar \
    --max-pool-size 500 \
    --thread-count 400 \
    ...
```

2. **스레드 수 감소**
```bash
java -jar multi-db-load-tester.jar \
    --thread-count 100 \
    --max-pool-size 200 \
    ...
```

3. **커넥션 누수 감지 활성화**
```bash
java -jar multi-db-load-tester.jar \
    --leak-detection-threshold 30 \
    ...
```

4. **풀 모니터링**
- 로그에서 커넥션 풀 상태 확인
```log
Pool Stats: Active=195, Idle=5, Total=200, WaitTimeMs=25
```

---

## 3. 성능 관련 문제

### 3.1 낮은 TPS

#### 증상
```
Average TPS: 150 (예상: 5000)
```

#### 원인
- 데이터베이스 리소스 부족 (CPU, 메모리, I/O)
- 인덱스 누락
- 트랜잭션 로그 병목
- 네트워크 지연

#### 해결 방법

1. **데이터베이스 리소스 확인**
```sql
-- PostgreSQL
SELECT * FROM pg_stat_activity;

-- MySQL
SHOW PROCESSLIST;

-- Oracle
SELECT * FROM v$session;
```

2. **인덱스 확인**
```sql
-- PostgreSQL
EXPLAIN ANALYZE SELECT * FROM load_test WHERE id = 1000;

-- MySQL
EXPLAIN SELECT * FROM load_test WHERE id = 1000;
```

3. **트랜잭션 로그 최적화**
```sql
-- PostgreSQL
ALTER SYSTEM SET wal_buffers = '256MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;

-- MySQL
SET GLOBAL innodb_log_buffer_size = 268435456;
```

4. **배치 크기 증가**
```bash
java -jar multi-db-load-tester.jar \
    --batch-size 100 \
    --mode insert-only \
    ...
```

5. **워밍업 기간 증가**
```bash
java -jar multi-db-load-tester.jar \
    --warmup 60 \
    ...
```

---

### 3.2 높은 레이턴시

#### 증상
```
Latency: P95=850ms, P99=2100ms (목표: P95<100ms)
```

#### 원인
- 쿼리 최적화 필요
- 커넥션 풀 대기 시간 길음
- 데이터베이스 잠금
- 디스크 I/O 병목

#### 해결 방법

1. **쿼리 분석 및 최적화**
```sql
-- PostgreSQL
EXPLAIN ANALYZE SELECT * FROM load_test ORDER BY id LIMIT 100;

-- MySQL
EXPLAIN SELECT * FROM load_test ORDER BY id LIMIT 100;
```

2. **커넥션 풀 튜닝**
```bash
java -jar multi-db-load-tester.jar \
    --min-pool-size 200 \
    --max-pool-size 400 \
    --idle-timeout 10 \
    ...
```

3. **잠금 모니터링**
```sql
-- PostgreSQL
SELECT * FROM pg_stat_activity WHERE wait_event_type = 'Lock';

-- MySQL
SHOW ENGINE INNODB STATUS;
```

4. **디스크 I/O 확인**
```bash
# Linux
iostat -x 1

# Windows
typeperf -s "\PhysicalDisk(*)\*"
```

---

### 3.3 메모리 부족

#### 증상
```
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
```

#### 원인
- JVM 힙 크기 부족
- 너무 많은 스레드
- 대규모 데이터 처리

#### 해결 방법

1. **JVM 힙 크기 증가**
```bash
java -Xmx4g -Xms2g -jar multi-db-load-tester.jar ...
```

2. **스레드 수 감소**
```bash
java -jar multi-db-load-tester.jar \
    --thread-count 50 \
    ...
```

3. **GC 로깅 활성화**
```bash
java -Xmx4g -Xms2g \
    -XX:+PrintGCDetails \
    -XX:+PrintGCTimeStamps \
    -jar multi-db-load-tester.jar ...
```

4. **메모리 사용량 모니터링**
```bash
# Linux
jmap -heap <pid>
jstat -gcutil <pid> 1000

# Windows
jmap -heap <pid>
```

---

## 4. 데이터베이스 특정 문제

### 4.1 Oracle

#### ORA-12541: TNS:no listener
```
Error: ORA-12541: TNS:no listener
```

**해결 방법**
```bash
# 리스너 시작
lsnrctl start

# 리스너 상태 확인
lsnrctl status
```

#### ORA-01000: maximum open cursors exceeded
```
Error: ORA-01000: maximum open cursors exceeded
```

**해결 방법**
```sql
-- 최대 커서 수 증가
ALTER SYSTEM SET open_cursors = 1000 SCOPE=BOTH;

-- 현재 설정 확인
SHOW PARAMETER open_cursors;
```

---

### 4.2 PostgreSQL

#### FATAL: remaining connection slots are reserved
```
Error: FATAL: remaining connection slots are reserved for non-replication superuser connections
```

**해결 방법**
```sql
-- 최대 커넥션 수 증가
ALTER SYSTEM SET max_connections = 200;

-- 슈퍼유저 예약 슬롯 증가
ALTER SYSTEM SET superuser_reserved_connections = 3;

-- 재시작 필요
```

#### FATAL: database "testdb" does not exist
```
Error: FATAL: database "testdb" does not exist
```

**해결 방법**
```sql
-- 데이터베이스 생성
CREATE DATABASE testdb;

-- 사용자 생성 및 권한 부여
CREATE USER testuser WITH PASSWORD 'testpass';
GRANT ALL PRIVILEGES ON DATABASE testdb TO testuser;
```

---

### 4.3 MySQL

#### Too many connections
```
Error: Too many connections
```

**해결 방법**
```sql
-- 최대 커넥션 수 확인
SHOW VARIABLES LIKE 'max_connections';

-- 최대 커넥션 수 증가
SET GLOBAL max_connections = 500;

-- 영구 설정 (my.cnf)
[mysqld]
max_connections = 500
```

#### Connection timeout
```
Error: Communications link failure due to underlying exception
```

**해결 방법**
```bash
# 연결 타임아웃 증가
java -jar multi-db-load-tester.jar \
    --max-lifetime 3600 \
    --idle-timeout 60 \
    ...
```

---

### 4.4 SQL Server

#### Login failed for user
```
Error: Login failed for user 'testuser'
```

**해결 방법**
```sql
-- 사용자 생성
CREATE LOGIN testuser WITH PASSWORD = 'testpass';

-- 데이터베이스 사용자 생성
USE testdb;
CREATE USER testuser FOR LOGIN testuser;

-- 권한 부여
ALTER ROLE db_owner ADD MEMBER testuser;
```

#### TCP/IP 연결 실패
```
Error: The TCP/IP connection to the host localhost, port 1433 has failed
```

**해결 방법**
1. SQL Server Configuration Manager에서 TCP/IP 활성화
2. 방화벽에서 포트 1433 허용
```bash
netsh advfirewall firewall add rule name="SQL Server" \
    dir=in action=allow protocol=TCP localport=1433
```

---

### 4.5 Tibero

#### Connection refused
```
Error: Connection refused
```

**해결 방법**
```bash
# Tibero 서버 상태 확인
tbdown -v
tbboot -v

# 리스너 시작
tbboot nomount
tbmount
```

#### 데이터베이스 이름 오류
```
Error: ORA-12154: TNS:could not resolve the connect identifier
```

**해결 방법**
- `--sid` 옵션 사용 (SID 형식)
```bash
java -jar multi-db-load-tester.jar \
    --db-type tibero \
    --host localhost \
    --port 8629 \
    --sid tibero \
    ...
```

- `--service-name` 옵션 사용 (Service Name 형식)
```bash
java -jar multi-db-load-tester.jar \
    --db-type tibero \
    --host localhost \
    --port 8629 \
    --service-name tibero_svc \
    ...
```

- `--jdbc-url` 옵션 사용 (직접 URL 지정)
```bash
java -jar multi-db-load-tester.jar \
    --db-type tibero \
    --jdbc-url "jdbc:tibero:thin:@//localhost:8629/tibero" \
    ...
```

---

### 4.6 SingleStore

#### Authentication failed
```
Error: Access denied for user 'testuser'@'localhost'
```

**해결 방법**
```sql
-- 사용자 생성
CREATE USER 'testuser'@'%' IDENTIFIED BY 'testpass';

-- 권한 부여
GRANT ALL PRIVILEGES ON testdb.* TO 'testuser'@'%';
FLUSH PRIVILEGES;
```

#### Pool size limitation
```
Error: SingleStore pool size limited to 32
```

**해결 방법**
- SingleStore는 기본적으로 풀 크기가 32개로 제한됨
```bash
java -jar multi-db-load-tester.jar \
    --max-pool-size 32 \
    --thread-count 30 \
    ...
```

---

## 5. 테스트 실행 관련 문제

### 5.1 테스트 중단되지 않음

#### 증상
- Ctrl+C를 눌러도 테스트가 종료되지 않음

#### 원인
- 워커 스레드가 중단되지 않음
- 커넥션이 올바르게 반환되지 않음

#### 해결 방법
```bash
# 프로세스 강제 종료 (마지막 수단)
# Linux/macOS
kill -9 <pid>

# Windows
taskkill /F /PID <pid>

# JVM 옵션으로 우아한 종료 개선
java -Djava.awt.headless=true -jar multi-db-load-tester.jar ...
```

---

### 5.2 결과 파일 생성 실패

#### 증상
```
Error: Failed to export results to file
```

#### 원인
- 파일 경로가 존재하지 않음
- 쓰기 권한 없음
- 디스크 공간 부족

#### 해결 방법

1. **디렉토리 생성**
```bash
# Linux/macOS
mkdir -p results
chmod 755 results

# Windows
mkdir results
```

2. **권한 확인**
```bash
# Linux/macOS
ls -la results/

# Windows
icacls results
```

3. **디스크 공간 확인**
```bash
# Linux
df -h

# Windows
wmic logicaldisk get size,freespace,caption
```

---

### 5.3 DDL 출력 오류

#### 증상
```
Error: Failed to print DDL
```

#### 원인
- 잘못된 데이터베이스 유형
- 어댑터 구현 누락

#### 해결 방법

1. **지원하는 데이터베이스 유형 확인**
```bash
java -jar multi-db-load-tester.jar --help
```

2. **올바른 데이터베이스 유형 사용**
```bash
# 올바른 유형
--db-type postgresql
--db-type mysql
--db-type oracle
--db-type sqlserver
--db-type tibero
--db-type db2
--db-type singlestore
```

---

## 6. 로그 및 디버깅

### 6.1 로그 레벨 설정

#### logback.xml 수정
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- DEBUG 레벨로 변경 -->
    <logger name="com.loadtest" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### 6.2 HikariCP 로깅

```xml
<!-- HikariCP 내부 로깅 -->
<logger name="com.zaxxer.hikari" level="DEBUG"/>
```

### 6.3 JDBC 로깅

```bash
# JDBC 드라이버 로깅 활성화
java -Djava.util.logging.config.file=logging.properties \
    -jar multi-db-load-tester.jar ...
```

---

## 7. 벤치마킹 모범 사례

### 7.1 테스트 계획 수립

1. **워밍업 필수**
   - 최소 30초 워밍업 기간 설정
   - 캐시 워밍 및 JIT 컴파일 고려

2. **테스트 기간**
   - 최소 5분 이상 권장
   - 단기 테스트는 불안정한 결과 초래

3. **반복 테스트**
   - 동일한 조건으로 3회 이상 테스트
   - 결과의 평균과 표준 편차 계산

---

### 7.2 커넥션 풀 튜닝

```bash
# 일반적인 튜닝 가이드라인
--min-pool-size: 스레드 수의 50-80%
--max-pool-size: 스레드 수의 1.2-1.5배
--idle-timeout: 10-30초
--keepalive-time: 30초 (최소)
--max-lifetime: 1800-3600초
```

---

### 7.3 워밍업 및 램프업

```bash
# 점진적 부하 증가
java -jar multi-db-load-tester.jar \
    --warmup 60 \
    --ramp-up 30 \
    --thread-count 200 \
    ...
```

---

## 8. 일반적인 질문 (FAQ)

### Q1: 어떤 데이터베이스가 지원되나요?
A: Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2, SingleStore

### Q2: 최대 몇 개의 스레드까지 지원되나요?
A: 이론적으로 제한 없으나, 실제로는 500-1000개의 스레드가 권장됩니다.

### Q3: DB 재시장 시 테스트가 중단되나요?
A: 아니요, 자동으로 재연결을 시도합니다.

### Q4: 결과는 어디에 저장되나요?
A: `--output-file` 옵션으로 지정된 경로에 CSV 또는 JSON 형식으로 저장됩니다.

### Q5: 워밍업 기간은 왜 필요한가요?
A: JVM 최적화(JIT), 데이터베이스 캐시 워밍업, 커넥션 풀 안정화를 위해 필요합니다.

### Q6: 배치 INSERT는 언제 사용하나요?
A: 대량의 INSERT 작업 시 성능을 최적화하기 위해 사용합니다.

### Q7: 토큰 버킷(TPS 제한)은 어떻게 작동하나요?
A: 지정된 TPS를 초과하지 않도록 요청을 제어합니다.

### Q8: 테스트 결과는 어떻게 해석하나요?
A: Post-Warmup TPS가 주요 지표입니다. P95/P99 레이턴시도 함께 고려해야 합니다.

---

## 9. 지원

### 9.1 버그 보고

버그를 보고할 때 다음 정보를 포함해주세요:
- OS 및 버전
- Java 버전 (`java -version`)
- 데이터베이스 유형 및 버전
- 사용된 명령행 인자
- 전체 스택 트레이스
- 로그 파일

### 9.2 로그 위치

- 표준 출력: 콘솔
- 로그 파일: `logs/` 디렉토리

### 9.3 추가 리소스

- README.md: 빠른 시작 가이드
- PROJECT_STRUCTURE.md: 프로젝트 구조
- API_DOCUMENTATION.md: API 참조

---

**문서 종료**
