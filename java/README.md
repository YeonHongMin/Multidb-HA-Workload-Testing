# Multi-Database Load Tester v2.1 (Java + HikariCP)

고성능 멀티스레드 데이터베이스 부하 테스트 도구 (HikariCP 커넥션 풀 기반)

## 주요 특징

- **5개 데이터베이스 지원**: Oracle, PostgreSQL, MySQL, SQL Server, Tibero
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

## 시스템 요구사항

- Java JDK 17+
- Maven 3.6+
- 지원 데이터베이스:
  - Oracle 19c+
  - PostgreSQL 11+
  - MySQL 5.7+
  - SQL Server 2016+
  - Tibero 6+

## 빌드

```bash
cd java
./build.sh
```

또는:

```bash
mvn clean package -DskipTests
```

## 사용법

### 기본 사용법

```bash
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test_user --password test_pass \
    --thread-count 100 --test-duration 60
```

### 작업 모드 (--mode)

| 모드 | 설명 | 사용 사례 |
|------|------|----------|
| `full` | INSERT → COMMIT → SELECT (기본값) | 데이터 무결성 검증 |
| `insert-only` | INSERT → COMMIT만 | 최대 쓰기 처리량 측정 |
| `select-only` | SELECT만 | 읽기 성능 측정 |
| `update-only` | UPDATE → COMMIT | 업데이트 성능 측정 |
| `delete-only` | DELETE → COMMIT | 삭제 성능 측정 |
| `mixed` | INSERT/UPDATE/DELETE 혼합 (60:20:15:5) | 실제 워크로드 시뮬레이션 |

### 데이터베이스별 예제

#### Oracle
```bash
./run_oracle_test.sh

# 또는 직접 실행
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type oracle \
    --host 192.168.0.100 --port 1521 --sid ORCL \
    --user test_user --password pass \
    --thread-count 200 --test-duration 300
```

#### PostgreSQL
```bash
./run_postgresql_test.sh

# 또는 직접 실행
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type postgresql \
    --host localhost --port 5432 --database testdb \
    --user test_user --password pass \
    --thread-count 200
```

#### MySQL
```bash
./run_mysql_test.sh

# 또는 직접 실행
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type mysql \
    --host localhost --port 3306 --database testdb \
    --user root --password pass \
    --thread-count 50
```

> **Note**: MySQL의 커넥션 풀 크기는 기본적으로 32개로 제한됩니다.

#### SQL Server
```bash
./run_sqlserver_test.sh

# 또는 직접 실행
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type sqlserver \
    --host localhost --port 1433 --database testdb \
    --user sa --password pass \
    --thread-count 200
```

#### Tibero
```bash
./run_tibero_test.sh

# 또는 직접 실행
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type tibero \
    --host 192.168.0.140 --port 8629 --sid tibero \
    --user test_user --password pass \
    --thread-count 200
```

### 고급 기능

#### 워밍업 + Ramp-up + Rate Limiting
```bash
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type postgresql \
    --host localhost --port 5432 --database testdb \
    --user test --password pass \
    --warmup 30 \
    --ramp-up 60 \
    --target-tps 5000 \
    --thread-count 200 --test-duration 300
```

#### 배치 INSERT
```bash
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type mysql \
    --host localhost --port 3306 --database testdb \
    --user root --password pass \
    --mode insert-only \
    --batch-size 100 \
    --thread-count 50
```

#### 결과 내보내기
```bash
# JSON 형식
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test --password pass \
    --output-format json \
    --output-file results/test_result.json

# CSV 형식
java -jar target/multi-db-load-tester-2.1.0.jar \
    --db-type oracle \
    --host localhost --port 1521 --sid XEPDB1 \
    --user test --password pass \
    --output-format csv \
    --output-file results/test_result.csv
```

## 명령행 옵션

### 필수 옵션

| 옵션 | 설명 |
|------|------|
| `--db-type` | 데이터베이스 타입 (oracle, postgresql, mysql, sqlserver, tibero) |
| `--host` | 데이터베이스 호스트 |
| `--user` | 사용자명 |
| `--password` | 비밀번호 |

### 연결 옵션

| 옵션 | 설명 |
|------|------|
| `--port` | 포트 번호 |
| `--database` | 데이터베이스명 (PostgreSQL, MySQL, SQL Server) |
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

### 모니터링 옵션

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `--monitor-interval` | 5.0 | 모니터 출력 간격 (초) |
| `--sub-second-interval` | 100 | Sub-second 측정 윈도우 (ms) |

### 기타

| 옵션 | 설명 |
|------|------|
| `--print-ddl` | DDL 스크립트 출력 후 종료 |
| `-h, --help` | 도움말 출력 |
| `-v, --version` | 버전 출력 |

## 모니터링 출력 예시

```
[Monitor] TXN: 45,230 | INS: 45,230 | SEL: 45,230 | UPD: 0 | DEL: 0 | ERR: 0 |
Avg TPS: 1507.67 | RT TPS: 1523.00 | Lat(p95/p99): 4.5/8.2ms | Pool: 95/100
```

## Tibero JDBC 드라이버 설치

Tibero JDBC 드라이버는 Maven Central에 없으므로 수동 설치가 필요합니다:

```bash
mvn install:install-file \
    -Dfile=tibero7-jdbc.jar \
    -DgroupId=com.tmax.tibero \
    -DartifactId=tibero-jdbc \
    -Dversion=7.0 \
    -Dpackaging=jar
```

설치 후 pom.xml에 의존성 추가:

```xml
<dependency>
    <groupId>com.tmax.tibero</groupId>
    <artifactId>tibero-jdbc</artifactId>
    <version>7.0</version>
</dependency>
```

## 환경 변수

스크립트에서 환경 변수를 사용하여 설정할 수 있습니다:

```bash
# Oracle
export ORACLE_HOST=localhost
export ORACLE_PORT=1521
export ORACLE_SID=XEPDB1
export ORACLE_USER=test_user
export ORACLE_PASSWORD=test_pass

# 공통 설정
export THREAD_COUNT=200
export TEST_DURATION=300
export MODE=full
export MIN_POOL_SIZE=100
export MAX_POOL_SIZE=200

./run_oracle_test.sh
```

## 문제 해결

### HikariCP 커넥션 풀 오류
- `--max-pool-size` 값이 데이터베이스 `max_connections` 설정보다 작은지 확인
- 네트워크 연결 및 방화벽 설정 확인

### Leak Detection 경고
- 트랜잭션 처리 시간이 `--leak-detection-threshold`를 초과하는 경우
- 장시간 트랜잭션이 예상되는 경우 임계값 증가

### OutOfMemoryError
- JVM 힙 크기 증가: `-Xmx4g`
- 스레드 수 감소

## 라이선스

MIT License
