# 다중 데이터베이스 HA 워크로드 테스트 도구 - 프로젝트 구조

## 개요

**버전**: 0.1
**언어**: Java 17
**빌드 도구**: Maven
**목적**: HikariCP 커넥션 풀링을 활용한 엔터프라이즈급 다중 데이터베이스 부하 테스트 도구

## 디렉토리 구조

```
Multidb-HA-Workload-Testing/
├── README.md                           # 프로젝트 문서
├── PROJECT_STRUCTURE.md                # 현재 파일
│
└── java/                               # 메인 Java 소스 디렉토리
    ├── pom.xml                         # Maven 설정
    ├── build.sh                        # 빌드 자동화 스크립트
    ├── run_mysql_test.sh               # MySQL 테스트 실행기
    ├── run_oracle_test.sh              # Oracle 테스트 실행기
    ├── run_postgresql_test.sh          # PostgreSQL 테스트 실행기
    ├── run_sqlserver_test.sh           # SQL Server 테스트 실행기
    ├── run_tibero_test.sh              # Tibero 테스트 실행기
    ├── run_db2_test.sh                 # IBM DB2 테스트 실행기
    │
    ├── src/
    │   └── main/
    │       ├── java/com/loadtest/      # 핵심 Java 클래스
    │       │   ├── MultiDBLoadTester.java
    │       │   ├── LoadTestWorker.java
    │       │   ├── MonitorThread.java
    │       │   ├── PerformanceCounter.java
    │       │   ├── RateLimiter.java
    │       │   ├── ResultExporter.java
    │       │   ├── DatabaseConfig.java
    │       │   ├── WorkMode.java
    │       │   ├── DatabaseAdapter.java
    │       │   ├── AbstractDatabaseAdapter.java
    │       │   ├── OracleAdapter.java
    │       │   ├── PostgreSQLAdapter.java
    │       │   ├── MySQLAdapter.java
    │       │   ├── SQLServerAdapter.java
    │       │   ├── TiberoAdapter.java
    │       │   └── DB2Adapter.java
    │       │
    │       └── resources/              # 설정 리소스
    │           └── logback.xml         # 로깅 설정
    │
    └── target/                         # 빌드 출력
        ├── classes/                    # 컴파일된 .class 파일
        └── multi-db-load-tester-0.1.jar  # 실행 가능한 JAR (~19MB)
```

## 핵심 컴포넌트

### 메인 클래스

| 클래스 | 라인 수 | 설명 |
|--------|---------|------|
| `MultiDBLoadTester.java` | ~415 | 메인 진입점. CLI 파싱, 테스트 오케스트레이션, 스레드 풀 관리 |
| `LoadTestWorker.java` | ~272 | 트랜잭션 실행 로직을 구현하는 워커 스레드 |
| `MonitorThread.java` | ~88 | 실시간 성능 모니터링 데몬 스레드 |
| `PerformanceCounter.java` | ~308 | 스레드 안전 메트릭 집계 (TPS, 레이턴시 백분위수) |
| `RateLimiter.java` | ~76 | TPS 속도 제한을 위한 토큰 버킷 알고리즘 |
| `ResultExporter.java` | ~94 | CSV/JSON 결과 내보내기 |
| `DatabaseConfig.java` | ~238 | 빌더 패턴 설정 객체 |
| `WorkMode.java` | ~32 | 6가지 작업 모드 정의 열거형 |

### 데이터베이스 어댑터

| 어댑터 | 데이터베이스 | 주요 특징 |
|--------|--------------|-----------|
| `DatabaseAdapter.java` | 인터페이스 | CRUD 작업 계약 정의 |
| `AbstractDatabaseAdapter.java` | 기본 클래스 | HikariCP 풀 관리 |
| `OracleAdapter.java` | Oracle 19c+ | 시퀀스 기반 ID, 파티션 지원 |
| `PostgreSQLAdapter.java` | PostgreSQL 11+ | SERIAL 자동 증가 |
| `MySQLAdapter.java` | MySQL 5.7+ | AUTO_INCREMENT |
| `SQLServerAdapter.java` | SQL Server 2016+ | IDENTITY 컬럼 |
| `TiberoAdapter.java` | Tibero 6+ | Oracle 호환 시퀀스 |
| `DB2Adapter.java` | IBM DB2 11.1+ | 시퀀스 기반 ID |

## 작업 모드

| 모드 | 설명 | 사용 사례 |
|------|------|-----------|
| `FULL` | INSERT → SELECT → UPDATE → DELETE | 전체 CRUD 사이클 테스트 |
| `INSERT_ONLY` | INSERT → COMMIT | 최대 쓰기 처리량 |
| `SELECT_ONLY` | SELECT만 수행 | 읽기 성능 테스트 |
| `UPDATE_ONLY` | UPDATE → COMMIT | 업데이트 처리량 |
| `DELETE_ONLY` | DELETE → COMMIT | 삭제 처리량 |
| `MIXED` | 60% INSERT, 20% SELECT, 15% UPDATE, 5% DELETE | 실제 워크로드 시뮬레이션 |

## 의존성

### 핵심 라이브러리

| 라이브러리 | 버전 | 용도 |
|------------|------|------|
| HikariCP | 5.1.0 | 고성능 JDBC 커넥션 풀링 |
| SLF4J API | 2.0.9 | 로깅 파사드 |
| Logback Classic | 1.4.14 | SLF4J 구현체 |
| Apache Commons CLI | 1.6.0 | 명령줄 인자 파싱 |
| GSON | 2.10.1 | JSON 처리 |

### JDBC 드라이버

| 데이터베이스 | 드라이버 | 버전 |
|--------------|----------|------|
| Oracle | ojdbc10 | 로컬 |
| PostgreSQL | postgresql | 42.2.9 |
| MySQL | mysql-connector-j | 9.5.0 |
| SQL Server | mssql-jdbc | 13.2.1.jre11 |
| Tibero | tibero-jdbc | 7.0 |
| IBM DB2 | jcc | 12.1.3.0 |

## 실행 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                    MultiDBLoadTester (메인)                      │
├─────────────────────────────────────────────────────────────────┤
│  1. CLI 인자 파싱                                                │
│     └── --db-type, --host, --user, --password 등               │
│                                                                 │
│  2. DatabaseAdapter 생성 (db-type 기반)                         │
│                                                                 │
│  3. HikariCP 커넥션 풀 초기화                                    │
│     └── 최소: 100, 최대: 200, 누수 감지: 60초                   │
│                                                                 │
│  4. 데이터베이스 스키마 설정 (--skip-schema-setup 없을 시)       │
│     └── CREATE TABLE, SEQUENCE, INDEX                          │
│                                                                 │
│  5. PerformanceCounter 및 RateLimiter 초기화                    │
│                                                                 │
│  6. [선택] 워밍업 단계 (--warmup N초)                           │
│     └── 최종 통계에서 제외되는 메트릭                           │
│                                                                 │
│  7. [선택] 램프업 단계 (--ramp-up N초)                          │
│     └── 점진적으로 스레드 수 증가                               │
│                                                                 │
│  8. 메인 테스트 단계 (--test-duration N초)                      │
│     ├── LoadTestWorker 스레드들이 트랜잭션 실행                 │
│     └── MonitorThread가 1초마다 보고                            │
│                                                                 │
│  9. 우아한 종료 (Ctrl+C)                                        │
│     └── 진행 중인 트랜잭션 완료                                 │
│                                                                 │
│ 10. 결과 내보내기 (--output-format csv|json)                    │
│                                                                 │
│ 11. HikariCP 풀 종료 및 프로그램 종료                           │
└─────────────────────────────────────────────────────────────────┘
```

## 워커 스레드 로직

```
LoadTestWorker.call()
│
├── 초기화: 풀에서 커넥션 획득
│
└── While (타임아웃 아님 && 종료 아님):
    │
    ├── 속도 제한: rateLimiter.acquire()
    │
    ├── 트랜잭션 실행 (WorkMode 기반):
    │   ├── FULL: INSERT → SELECT → UPDATE → DELETE
    │   ├── INSERT_ONLY: INSERT (배치 지원)
    │   ├── SELECT_ONLY: 랜덤 SELECT
    │   ├── UPDATE_ONLY: 랜덤 UPDATE
    │   ├── DELETE_ONLY: 랜덤 DELETE
    │   └── MIXED: 확률 기반 선택 (60:20:15:5)
    │
    ├── 메트릭 기록: 레이턴시, TPS
    │
    └── 오류 처리:
        └── 5회 연속 오류 → 재연결
```

## 모니터링 출력

```
[Monitor] TXN: 45,230 | INS: 45,230 | SEL: 45,230 | UPD: 45,230 | DEL: 45,230 | ERR: 0 |
Avg TPS: 1507.67 | RT TPS: 1523.00 | Lat(p95/p99): 4.5/8.2ms | Pool: 95/100
```

> **Note**: `--mode full` 사용 시 모든 CRUD 작업이 수행됩니다.

| 메트릭 | 설명 |
|--------|------|
| TXN | 완료된 총 트랜잭션 수 |
| INS/SEL/UPD/DEL | 작업별 카운트 |
| ERR | 오류 수 |
| Avg TPS | 평균 초당 트랜잭션 |
| RT TPS | 실시간 TPS (최근 1초) |
| Lat(p95/p99) | 레이턴시 백분위수 (밀리초) |
| Pool | 풀 내 활성/전체 커넥션 |

## 사용 예제

```bash
# 빌드
cd java && mvn clean package

# Oracle 부하 테스트 실행
java -jar target/multi-db-load-tester-0.1.jar \
    --db-type oracle \
    --host 192.168.0.100 \
    --port 1521 \
    --sid ORCL \
    --user test_user \
    --password pass \
    --thread-count 200 \
    --test-duration 300 \
    --mode full \
    --warmup 30 \
    --ramp-up 60 \
    --target-tps 5000 \
    --min-pool-size 100 \
    --max-pool-size 200 \
    --output-format json \
    --output-file results/test.json
```

## 주요 기능

- **다중 데이터베이스 지원**: Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2
- **HikariCP 커넥션 풀링**: 누수 감지 기능이 있는 고성능 풀
- **동시 실행**: 1000개 이상 스레드 지원
- **6가지 작업 모드**: Full ACID, 단일 작업, 혼합 워크로드
- **고급 부하 제어**: 워밍업, 램프업, TPS 속도 제한
- **실시간 모니터링**: 초단위 TPS, 레이턴시 백분위수 (P50/P95/P99)
- **배치 작업**: 설정 가능한 배치 INSERT
- **우아한 종료**: 진행 중인 트랜잭션 완료와 함께 Ctrl+C 처리
- **결과 내보내기**: 시계열 데이터와 함께 CSV 및 JSON 형식
