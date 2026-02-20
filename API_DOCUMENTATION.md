# API 문서화
# 멀티 데이터베이스 워크로드 테스트 도구

## 문서 정보
| 항목 | 설명 |
|------|-------------|
| 문서 버전 | 1.0 |
| 프로젝트 버전 | v0.2.5 |
| 최종 업데이트 | 2025-12-29 |
| 문서 관리자 | 개발팀 |

---

## 1. 개요

본 문서는 멀티 데이터베이스 워크로드 테스트 도구의 주요 API 인터페이스와 클래스를 설명합니다.

---

## 2. 핵심 인터페이스

### 2.1 DatabaseAdapter

**패키지**: `com.loadtest.DatabaseAdapter`

데이터베이스 공통 인터페이스로, 모든 데이터베이스 어댑터가 구현해야 하는 기본 메서드를 정의합니다.

#### 메서드 목록

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `createConnectionPool(DatabaseConfig config)` | void | HikariCP 커넥션 풀 생성 |
| `getConnection()` | Connection | 커넥션 획득 |
| `releaseConnection(Connection conn, boolean isError)` | void | 커넥션 반환 (에러 발생 여부 전달) |
| `closePool()` | void | 커넥션 풀 종료 |
| `getPoolStats()` | Map\<String, Object\> | 풀 상태 통계 조회 |
| `executeInsert(Connection conn, String threadId, String randomData)` | long | INSERT 실행 및 생성된 레코드 ID 반환 |
| `executeBatchInsert(Connection conn, String threadId, int batchSize)` | int | 배치 INSERT 실행 및 삽입된 레코드 수 반환 |
| `executeSelect(Connection conn, long recordId)` | Object[] | SELECT 실행 |
| `executeRandomSelect(Connection conn, long maxId)` | Object[] | 랜덤 SELECT 실행 |
| `executeUpdate(Connection conn, long recordId)` | boolean | UPDATE 실행 |
| `executeDelete(Connection conn, long recordId)` | boolean | DELETE 실행 |
| `getMaxId(Connection conn)` | long | 최대 ID 조회 |
| `getRandomId(long maxId)` | long | 랜덤 ID 생성 (1 ~ maxId) |
| `commit(Connection conn)` | void | 트랜잭션 커밋 |
| `rollback(Connection conn)` | void | 트랜잭션 롤백 |
| `getDDL()` | String | DDL 스크립트 반환 |
| `setupSchema(Connection conn)` | void | 스키마 설정 (테이블 생성) |
| `truncateTable(Connection conn)` | void | 테이블 TRUNCATE (데이터 삭제, 스키마 유지) |
| `buildJdbcUrl(DatabaseConfig config)` | String | JDBC URL 생성 |
| `getDriverClassName()` | String | 드라이버 클래스명 반환 |
| `generateRandomData(int length)` | String (default) | 랜덤 데이터 생성 (알파벳+숫자) |

#### 구현 클래스

- `OracleAdapter`
- `PostgreSQLAdapter`
- `MySQLAdapter`
- `SQLServerAdapter`
- `TiberoAdapter`
- `DB2Adapter`
- `SingleStoreAdapter`

---

## 3. 핵심 클래스

### 3.1 DatabaseConfig

**패키지**: `com.loadtest.DatabaseConfig`

데이터베이스 연결 설정을 담는 불변 클래스입니다.

#### 생성자

DatabaseConfig는 Builder 패턴을 사용하여 생성합니다.

```java
DatabaseConfig config = DatabaseConfig.builder()
    .dbType("oracle")
    .host("localhost")
    .port(1521)
    .database("orcl")
    .user("test")
    .password("password")
    .minPoolSize(100)
    .maxPoolSize(200)
    .build();
```

#### Builder 메서드

| 메서드 | 타입 | 기본값 | 설명 |
|--------|------|--------|------|
| `dbType(String dbType)` | String | 필수 | 데이터베이스 유형 (oracle, postgresql, mysql, sqlserver, tibero, db2, singlestore) |
| `host(String host)` | String | 필수 | 데이터베이스 호스트 주소 |
| `port(int port)` | int | 0 | 데이터베이스 포트 (0일 경우 기본 포트 사용) |
| `database(String database)` | String | null | 데이터베이스 이름 (PostgreSQL, MySQL, SQL Server, SingleStore) |
| `sid(String sid)` | String | null | SID (Oracle, Tibero) - SID 형식 접속 |
| `serviceName(String serviceName)` | String | null | Service Name (Oracle, Tibero) - Service Name 형식 접속 |
| `jdbcUrl(String jdbcUrl)` | String | null | 직접 JDBC URL 지정 (host/port/database 등 무시) |
| `user(String user)` | String | 필수 | 데이터베이스 사용자명 |
| `password(String password)` | String | 필수 | 데이터베이스 비밀번호 |
| `minPoolSize(int minPoolSize)` | int | 100 | 최소 커넥션 풀 크기 |
| `maxPoolSize(int maxPoolSize)` | int | 200 | 최대 커넥션 풀 크기 |
| `maxLifetimeSeconds(int maxLifetime)` | int | 1800 | 커넥션 최대 수명 (초) |
| `leakDetectionThresholdSeconds(int threshold)` | int | 60 | 커넥션 누수 감지 임계값 (초) |
| `idleCheckIntervalSeconds(int interval)` | int | 30 | 유휴 연결 검사 간격 (초) |
| `idleTimeoutSeconds(int timeout)` | int | 30 | 유휴 연결 타임아웃 (초) |
| `keepaliveTimeSeconds(int keepalive)` | int | 30 | 유휴 연결 keepalive 간격 (초, 최소 30초) |

#### Getter 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `getDbType()` | String | 데이터베이스 유형 |
| `getHost()` | String | 호스트 주소 |
| `getPort()` | int | 포트 번호 |
| `getDatabase()` | String | 데이터베이스 이름 |
| `getSid()` | String | SID/Service 이름 |
| `getUser()` | String | 사용자명 |
| `getPassword()` | String | 비밀번호 |
| `getMinPoolSize()` | int | 최소 풀 크기 |
| `getMaxPoolSize()` | int | 최대 풀 크기 |
| `getMaxLifetimeSeconds()` | int | 커넥션 최대 수명 |
| `getLeakDetectionThresholdSeconds()` | int | 누수 감지 임계값 |
| `getIdleCheckIntervalSeconds()` | int | 유휴 검사 간격 |
| `getIdleTimeoutSeconds()` | int | 유휴 타임아웃 |
| `getKeepaliveTimeSeconds()` | int | Keepalive 간격 |

---

### 3.2 MultiDBLoadTester

**패키지**: `com.loadtest.MultiDBLoadTester`

메인 테스터 클래스로, 데이터베이스 부하 테스트를 실행합니다.

#### 생성자

```java
public MultiDBLoadTester(DatabaseConfig config)
```

#### 주요 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `printDDL()` | void | DDL 스크립트 출력 |
| `runLoadTest(int threadCount, int durationSeconds, WorkMode mode, ...)` | void | 부하 테스트 실행 |

#### runLoadTest 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `threadCount` | int | 100 | 워커 스레드 수 |
| `durationSeconds` | int | 300 | 테스트 지속 시간 (초) |
| `mode` | WorkMode | full | 작업 모드 |
| `truncateTable` | boolean | false | 테스트 전 테이블 비우기 |
| `monitorInterval` | double | 1.0 | 모니터링 출력 간격 (초) |
| `subSecondIntervalMs` | int | 100 | 서브초 메트릭 윈도우 (밀리초) |
| `warmupSeconds` | int | 30 | 워밍업 기간 (초) |
| `rampUpSeconds` | int | 0 | 램프업 기간 (초) |
| `targetTps` | int | 0 | 목표 TPS (0 = 무제한) |
| `batchSize` | int | 1 | 배치 INSERT 크기 |
| `outputFormat` | String | null | 결과 내보내기 형식 (csv, json) |
| `outputFile` | String | null | 결과 파일 경로 |

---

### 3.3 PerformanceCounter

**패키지**: `com.loadtest.PerformanceCounter`

성능 메트릭을 수집하고 관리하는 클래스입니다.

#### 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `incrementSuccess()` | void | 성공 트랜잭션 수 증가 |
| `incrementError()` | void | 에러 트랜잭션 수 증가 |
| `incrementInserts()` | void | INSERT 수 증가 |
| `incrementSelects()` | void | SELECT 수 증가 |
| `incrementUpdates()` | void | UPDATE 수 증가 |
| `incrementDeletes()` | void | DELETE 수 증가 |
| `recordLatency(long latencyMs)` | void | 레이턴시 기록 (밀리초) |
| `getStats()` | Map\<String, Object\> | 통계 반환 |
| `getLatencyStats()` | Map\<String, Double\> | 레이턴시 통계 반환 |
| `getTimeSeries()` | List\<Map\<String, Object\>\> | 시계열 데이터 반환 |
| `setWarmupEndTime(long epochMilli)` | void | 워밍업 종료 시간 설정 |
| `reset()` | void | 카운터 리셋 |

#### 통계 메트릭

**getStats() 반환 항목**:

| 항목 | 타입 | 설명 |
|------|------|------|
| `totalTransactions` | Long | 전체 트랜잭션 수 |
| `totalInserts` | Long | 전체 INSERT 수 |
| `totalSelects` | Long | 전체 SELECT 수 |
| `totalUpdates` | Long | 전체 UPDATE 수 |
| `totalDeletes` | Long | 전체 DELETE 수 |
| `totalErrors` | Long | 전체 에러 수 |
| `elapsedSeconds` | Double | 경과 시간 (초) |
| `avgTps` | Double | 평균 TPS |
| `postWarmupTps` | Double | 워밍업 제외 평균 TPS |
| `currentTps` | Double | 현재 TPS |

**getLatencyStats() 반환 항목**:

| 항목 | 타입 | 설명 |
|------|------|------|
| `avg` | Double | 평균 레이턴시 (ms) |
| `p50` | Double | 50번째 백분위 레이턴시 (ms) |
| `p95` | Double | 95번째 백분위 레이턴시 (ms) |
| `p99` | Double | 99번째 백분위 레이턴시 (ms) |
| `min` | Double | 최소 레이턴시 (ms) |
| `max` | Double | 최대 레이턴시 (ms) |

---

### 3.4 RateLimiter

**패키지**: `com.loadtest.RateLimiter`

토큰 버킷 알고리즘을 사용한 TPS 제한 기능입니다.

#### 생성자

```java
public RateLimiter(int targetTps)
```

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `targetTps` | int | 목표 TPS |

#### 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `acquire()` | void | 토큰 획득 (TPS 제한 대기) |
| `tryAcquire()` | boolean | 토큰 획득 시도 (대기하지 않음) |

---

### 3.5 WorkMode

**패키지**: `com.loadtest.WorkMode`

작업 모드를 정의하는 열거형입니다.

#### 값

| 값 | 설명 |
|----|------|
| `FULL` | 모든 작업 (INSERT, SELECT, UPDATE, DELETE) |
| `INSERT_ONLY` | INSERT만 수행 |
| `SELECT_ONLY` | SELECT만 수행 |
| `UPDATE_ONLY` | UPDATE만 수행 |
| `DELETE_ONLY` | DELETE만 수행 |
| `MIXED` | 혼합 작업 |

#### 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `getValue()` | String | 작업 모드 값 반환 |
| `fromString(String mode)` | WorkMode | 문자열로부터 WorkMode 반환 |

---

### 3.6 MonitorThread

**패키지**: `com.loadtest.MonitorThread`

실시간 모니터링을 수행하는 스레드 클래스입니다.

#### 생성자

```java
public MonitorThread(double interval, Instant endTime, DatabaseAdapter dbAdapter,
                    PerformanceCounter perfCounter, AtomicBoolean shutdownRequested)
```

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `interval` | double | 모니터링 간격 (초) |
| `endTime` | Instant | 테스트 종료 시간 |
| `dbAdapter` | DatabaseAdapter | 데이터베이스 어댑터 |
| `perfCounter` | PerformanceCounter | 성능 카운터 |
| `shutdownRequested` | AtomicBoolean | 종료 요청 플래그 |

#### 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `start()` | void | 모니터링 스레드 시작 |
| `stopMonitor()` | void | 모니터링 스레드 중지 |
| `logFinalSnapshot()` | void | 최종 스냅샷 출력 |

---

### 3.7 LoadTestWorker

**패키지**: `com.loadtest.LoadTestWorker`

부하 테스트 워커 스레드로, 실제 데이터베이스 작업을 수행합니다.

#### 생성자

```java
public LoadTestWorker(int workerId, DatabaseAdapter dbAdapter, Instant endTime,
                      WorkMode mode, long maxIdCache, int batchSize,
                      RateLimiter rateLimiter, PerformanceCounter perfCounter,
                      AtomicBoolean shutdownRequested)
```

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `workerId` | int | 워커 ID |
| `dbAdapter` | DatabaseAdapter | 데이터베이스 어댑터 |
| `endTime` | Instant | 테스트 종료 시간 |
| `mode` | WorkMode | 작업 모드 |
| `maxIdCache` | long | 최대 ID 캐시 |
| `batchSize` | int | 배치 크기 |
| `rateLimiter` | RateLimiter | 속도 제한기 (null = 무제한) |
| `perfCounter` | PerformanceCounter | 성능 카운터 |
| `shutdownRequested` | AtomicBoolean | 종료 요청 플래그 |

#### 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `call()` | Integer | 워커 실행 및 트랜잭션 수 반환 |

---

### 3.8 ResultExporter

**패키지**: `com.loadtest.ResultExporter`

테스트 결과를 파일로 내보내는 유틸리티 클래스입니다.

#### 정적 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `exportCsv(String filepath, Map\<String, Object\> stats, ...)` | void | CSV 형식으로 내보내기 |
| `exportJson(String filepath, Map\<String, Object\> stats, ...)` | void | JSON 형식으로 내보내기 |

---

## 4. 데이터베이스 어댑터 상세

### 4.1 OracleAdapter

Oracle 데이터베이스용 어댑터입니다.

#### 특징

- JDBC Driver: `oracle.jdbc.OracleDriver`
- 기본 포트: 1521
- SID 형식 및 Service Name 형식 지원
- 직접 JDBC URL 지정 지원
- 시퀀스 사용 (LOAD_TEST_SEQ)

#### 주요 구현

```java
String getDriverClassName() {
    return "oracle.jdbc.OracleDriver";
}

String buildJdbcUrl(DatabaseConfig config) {
    // service_name이 설정된 경우 service_name 형식 사용
    if (config.getServiceName() != null && !config.getServiceName().isEmpty()) {
        return String.format("jdbc:oracle:thin:@//%s:%d/%s",
                config.getHost(), config.getDefaultPort(), config.getServiceName());
    }
    // SID 형식 사용 (기존 방식)
    String sid = config.getSid() != null ? config.getSid() : config.getDatabase();
    return String.format("jdbc:oracle:thin:@%s:%d:%s",
            config.getHost(), config.getDefaultPort(), sid);
}
```

#### 연결 URL 형식

| 옵션 | JDBC URL 형식 |
|------|---------------|
| `--sid` | `jdbc:oracle:thin:@host:port:SID` |
| `--service-name` | `jdbc:oracle:thin:@//host:port/SERVICE_NAME` |
| `--jdbc-url` | 사용자 지정 URL 그대로 사용 |

---

### 4.2 PostgreSQLAdapter

PostgreSQL 데이터베이스용 어댑터입니다.

#### 특징

- JDBC Driver: `org.postgresql.Driver`
- 기본 포트: 5432
- 데이터베이스 이름 필수
- Serial 자동 증가 사용

#### 주요 구현

```java
String getDriverClassName() {
    return "org.postgresql.Driver";
}

String buildJdbcUrl(DatabaseConfig config) {
    return String.format("jdbc:postgresql://%s:%d/%s",
        config.getHost(), config.getPort(), config.getDatabase());
}
```

---

### 4.3 MySQLAdapter

MySQL 데이터베이스용 어댑터입니다.

#### 특징

- JDBC Driver: `com.mysql.cj.jdbc.Driver`
- 기본 포트: 3306
- AUTO_INCREMENT 사용
- 풀 크기 기본 제한: 32개

#### 주요 구현

```java
String getDriverClassName() {
    return "com.mysql.cj.jdbc.Driver";
}

String buildJdbcUrl(DatabaseConfig config) {
    return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
        config.getHost(), config.getPort(), config.getDatabase());
}
```

---

### 4.4 SQLServerAdapter

SQL Server 데이터베이스용 어댑터입니다.

#### 특징

- JDBC Driver: `com.microsoft.sqlserver.jdbc.SQLServerDriver`
- 기본 포트: 1433
- IDENTITY 컬럼 사용

#### 주요 구현

```java
String getDriverClassName() {
    return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
}

String buildJdbcUrl(DatabaseConfig config) {
    return String.format("jdbc:sqlserver://%s:%d;databaseName=%s",
        config.getHost(), config.getPort(), config.getDatabase());
}
```

---

### 4.5 TiberoAdapter

Tibero 데이터베이스용 어댑터입니다.

#### 특징

- JDBC Driver: `com.tmax.tibero.jdbc.TbDriver`
- 기본 포트: 8629
- SID 형식 및 Service Name 형식 지원
- 직접 JDBC URL 지정 지원
- 시퀀스 사용 (LOAD_TEST_SEQ)

#### 주요 구현

```java
String getDriverClassName() {
    return "com.tmax.tibero.jdbc.TbDriver";
}

String buildJdbcUrl(DatabaseConfig config) {
    // service_name이 설정된 경우 service_name 형식 사용
    if (config.getServiceName() != null && !config.getServiceName().isEmpty()) {
        return String.format("jdbc:tibero:thin:@//%s:%d/%s",
                config.getHost(), config.getDefaultPort(), config.getServiceName());
    }
    // SID 형식 사용 (기존 방식)
    String sid = config.getSid() != null ? config.getSid() : config.getDatabase();
    return String.format("jdbc:tibero:thin:@%s:%d:%s",
            config.getHost(), config.getDefaultPort(), sid);
}
```

#### 연결 URL 형식

| 옵션 | JDBC URL 형식 |
|------|---------------|
| `--sid` | `jdbc:tibero:thin:@host:port:SID` |
| `--service-name` | `jdbc:tibero:thin:@//host:port/SERVICE_NAME` |
| `--jdbc-url` | 사용자 지정 URL 그대로 사용 |

---

### 4.6 DB2Adapter

IBM DB2 데이터베이스용 어댑터입니다.

#### 특징

- JDBC Driver: `com.ibm.db2.jcc.DB2Driver`
- 기본 포트: 50000
- IDENTITY 컬럼 사용

#### 주요 구현

```java
String getDriverClassName() {
    return "com.ibm.db2.jcc.DB2Driver";
}

String buildJdbcUrl(DatabaseConfig config) {
    return String.format("jdbc:db2://%s:%d/%s",
        config.getHost(), config.getPort(), config.getDatabase());
}
```

---

### 4.7 SingleStoreAdapter

SingleStore 데이터베이스용 어댑터입니다.

#### 특징

- JDBC Driver: `com.singlestore.jdbc.Driver`
- 기본 포트: 3306
- AUTO_INCREMENT 사용
- 풀 크기 기본 제한: 32개

#### 주요 구현

```java
String getDriverClassName() {
    return "com.singlestore.jdbc.Driver";
}

String buildJdbcUrl(DatabaseConfig config) {
    return String.format("jdbc:singlestore://%s:%d/%s?useSSL=false&serverTimezone=UTC",
        config.getHost(), config.getPort(), config.getDatabase());
}
```

---

## 5. 예제 코드

### 5.1 기본 사용법

```java
// 데이터베이스 설정 생성
DatabaseConfig config = DatabaseConfig.builder()
    .dbType("postgresql")
    .host("localhost")
    .port(5432)
    .database("testdb")
    .user("testuser")
    .password("testpass")
    .minPoolSize(50)
    .maxPoolSize(100)
    .build();

// 테스터 생성
MultiDBLoadTester tester = new MultiDBLoadTester(config);

// 부하 테스트 실행
tester.runLoadTest(
    100,              // 스레드 수
    300,              // 테스트 시간 (초)
    WorkMode.FULL,    // 작업 모드
    false,            // truncate
    1.0,              // 모니터링 간격 (초)
    100,              // 서브초 윈도우 (ms)
    30,               // 워밍업 (초)
    10,               // 램프업 (초)
    5000,             // 목표 TPS
    10,               // 배치 크기
    "json",           // 출력 형식
    "results.json"    // 출력 파일
);
```

### 5.2 커스텀 어댑터 사용

```java
// 어댑터 직접 생성
DatabaseAdapter adapter = new PostgreSQLAdapter();

// 커넥션 풀 생성
adapter.createConnectionPool(config);

// 스키마 설정
try (Connection conn = adapter.getConnection()) {
    adapter.setupSchema(conn);
}

// 작업 수행
try (Connection conn = adapter.getConnection()) {
    long newId = adapter.executeInsert(conn, "thread-1", "test-data");
    Object[] row = adapter.executeSelect(conn, newId);
    adapter.commit(conn);
} catch (SQLException e) {
    e.printStackTrace();
}

// 정리
adapter.closePool();
```

### 5.3 커넥션 풀 상태 조회

```java
DatabaseAdapter adapter = new MySQLAdapter();
adapter.createConnectionPool(config);

// 풀 상태 조회
Map<String, Object> stats = adapter.getPoolStats();
System.out.println("Active Connections: " + stats.get("active"));
System.out.println("Idle Connections: " + stats.get("idle"));
System.out.println("Total Connections: " + stats.get("total"));
System.out.println("Wait Time (ms): " + stats.get("waitTimeMs"));
```

---

## 6. 명령행 인터페이스

### 6.1 필수 옵션

| 옵션 | 설명 | 예시 |
|------|------|------|
| `--db-type` | 데이터베이스 유형 | `--db-type postgresql` |
| `--host` | 호스트 주소 | `--host localhost` |
| `--user` | 사용자명 | `--user testuser` |
| `--password` | 비밀번호 | `--password testpass` |

### 6.2 연결 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--port` | 포트 번호 | DB 기본 포트 |
| `--database` | 데이터베이스 이름 | 필수 (Oracle, Tibero 제외) |
| `--sid` | SID (Oracle, Tibero) - SID 형식 접속 | 필수 (Oracle, Tibero) |
| `--service-name` | Service Name (Oracle, Tibero) - Service Name 형식 접속 | --sid 대신 사용 가능 |
| `--jdbc-url` | 직접 JDBC URL 지정 | host/port/database 등 무시 |

### 6.3 테스트 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--thread-count` | 워커 스레드 수 | 100 |
| `--test-duration` | 테스트 시간 (초) | 300 |
| `--mode` | 작업 모드 | full |
| `--truncate` | 테이블 비우기 | false |
| `--batch-size` | 배치 크기 | 1 |

### 6.4 워밍업 및 부하 제어

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--warmup` | 워밍업 시간 (초) | 30 |
| `--ramp-up` | 램프업 시간 (초) | 0 |
| `--target-tps` | 목표 TPS (0 = 무제한) | 0 |

### 6.5 커넥션 풀 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--min-pool-size` | 최소 풀 크기 | 100 |
| `--max-pool-size` | 최대 풀 크기 | 200 |
| `--max-lifetime` | 커넥션 최대 수명 (초) | 1800 |
| `--idle-timeout` | 유휴 타임아웃 (초) | 30 |
| `--keepalive-time` | Keepalive 간격 (초) | 30 |

---

## 7. 예외 처리

### 7.1 주요 예외

| 예외 | 발생 상황 |
|------|----------|
| `IllegalArgumentException` | 지원하지 않는 데이터베이스 유형 |
| `SQLException` | 데이터베이스 연결 또는 쿼리 오류 |
| `ParseException` | 명령행 인자 파싱 오류 |

### 7.2 에러 처리 예시

```java
try {
    MultiDBLoadTester tester = new MultiDBLoadTester(config);
    tester.runLoadTest(...);
} catch (IllegalArgumentException e) {
    System.err.println("잘못된 데이터베이스 유형: " + e.getMessage());
} catch (SQLException e) {
    System.err.println("데이터베이스 오류: " + e.getMessage());
    e.printStackTrace();
}
```

---

## 8. 성능 메트릭

### 8.1 TPS (Transactions Per Second)

- **Average TPS**: 전체 기간 평균 TPS
- **Post-Warmup TPS**: 워밍업 제외 평균 TPS
- **Current TPS**: 현재 구간 TPS

### 8.2 레이턴시 (Latency)

- **Average**: 평균 레이턴시
- **P50**: 50% 요청 응답 시간
- **P95**: 95% 요청 응답 시간
- **P99**: 99% 요청 응답 시간
- **Min/Max**: 최소/최대 응답 시간

### 8.3 커넥션 풀 메트릭

- **Active**: 활성 커넥션 수
- **Idle**: 유휴 커넥션 수
- **Total**: 전체 커넥션 수
- **Wait Time**: 커넥션 대기 시간

---

**문서 종료**
