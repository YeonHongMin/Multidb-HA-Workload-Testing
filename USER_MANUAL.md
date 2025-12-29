# 사용자 매뉴얼
# 멀티 데이터베이스 워크로드 테스트 도구

## 문서 정보
| 항목 | 설명 |
|------|-------------|
| 문서 버전 | 1.0 |
| 프로젝트 버전 | v0.2.3 |
| 최종 업데이트 | 2025-12-29 |
| 문서 관리자 | 개발팀 |

---

## 1. 개요

본 문서는 멀티 데이터베이스 워크로드 테스트 도구의 사용 방법을 단계별로 설명합니다. 설치부터 테스트 실행, 결과 분석까지 포괄적으로 다룹니다.

---

## 2. 요구사항

### 2.1 소프트웨어 요구사항

| 항목 | 최소 버전 | 권장 버전 |
|------|-----------|-----------|
| Java JDK | 11 | 17+ |
| 운영체제 | Windows 10+, Linux, macOS | 최신 LTS |
| RAM | 2GB | 4GB+ |
| 디스크 | 1GB | 2GB+ |

### 2.2 데이터베이스 요구사항

| 데이터베이스 | 최소 버전 | JDBC 드라이버 |
|------------|-----------|--------------|
| Oracle | 11g | ojdbc10.jar |
| PostgreSQL | 10+ | postgresql-42.2.9.jar |
| MySQL | 5.7+ | mysql-connector-j-9.5.0.jar |
| SQL Server | 2016+ | mssql-jdbc-13.2.1.jar |
| Tibero | 6+ | tibero7-jdbc.jar |
| IBM DB2 | 9.7+ | db2jcc.jar |
| SingleStore | 7.0+ | singlestore-jdbc-1.2.1.jar |

---

## 3. 설치

### 3.1 빌드

#### 소스 코드에서 빌드

```bash
# 프로젝트 디렉토리로 이동
cd java/

# Maven으로 빌드
mvn clean package

# 생성된 JAR 파일 확인
ls -la target/multi-db-load-tester-*.jar
```

### 3.2 JAR 파일

빌드 완료 후 다음 JAR 파일이 생성됩니다:
- `target/multi-db-load-tester-0.2.3.jar`

### 3.3 JDBC 드라이버

JDBC 드라이버는 `java/jre/` 디렉토리에 이미 포함되어 있습니다:
```
java/jre/
├── db2/
├── mysql/
├── oracle/
├── postgresql/
├── singlestore/
├── sqlserver/
└── tibero/
```

---

## 4. 빠른 시작

### 4.1 PostgreSQL 예시

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 100 \
    --test-duration 300 \
    --mode full
```

### 4.2 MySQL 예시

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type mysql \
    --host localhost \
    --port 3306 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 100 \
    --test-duration 300 \
    --mode full
```

### 4.3 실행 스크립트 사용

```bash
# PostgreSQL
./java/run_postgresql_test.sh

# MySQL
./java/run_mysql_test.sh

# Oracle
./java/run_oracle_test.sh

# SQL Server
./java/run_sqlserver_test.sh

# Tibero
./java/run_tibero_test.sh

# DB2
./java/run_db2_test.sh

# SingleStore
./java/run_singlestore_test.sh
```

---

## 5. 명령행 옵션

### 5.1 필수 옵션

| 옵션 | 설명 | 예시 |
|------|------|------|
| `--db-type` | 데이터베이스 유형 | `postgresql`, `mysql`, `oracle`, `sqlserver`, `tibero`, `db2`, `singlestore` |
| `--host` | 호스트 주소 | `localhost`, `192.168.1.100` |
| `--user` | 사용자명 | `testuser` |
| `--password` | 비밀번호 | `testpass` |

### 5.2 데이터베이스 연결 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--port` | 포트 번호 | DB 기본 포트 |
| `--database` | 데이터베이스 이름 | 필수 (Oracle, Tibero 제외) |
| `--sid` | SID/Service 이름 | 필수 (Oracle, Tibero) |

### 5.3 테스트 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--thread-count` | 워커 스레드 수 | 100 |
| `--test-duration` | 테스트 시간 (초) | 300 |
| `--mode` | 작업 모드 | `full` |
| `--truncate` | 테이블 비우기 | `false` |
| `--batch-size` | 배치 크기 | 1 |

### 5.4 워밍업 및 부하 제어

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--warmup` | 워밍업 시간 (초) | 30 |
| `--ramp-up` | 램프업 시간 (초) | 0 |
| `--target-tps` | 목표 TPS (0 = 무제한) | 0 |

### 5.5 커넥션 풀 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--min-pool-size` | 최소 풀 크기 | 100 |
| `--max-pool-size` | 최대 풀 크기 | 200 |
| `--max-lifetime` | 커넥션 최대 수명 (초) | 1800 |
| `--leak-detection-threshold` | 누수 감지 임계값 (초) | 60 |
| `--idle-timeout` | 유휴 타임아웃 (초) | 30 |
| `--keepalive-time` | Keepalive 간격 (초) | 30 |

### 5.6 모니터링 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--monitor-interval` | 모니터링 출력 간격 (초) | 1.0 |
| `--sub-second-interval` | 서브초 메트릭 윈도우 (ms) | 100 |

### 5.7 결과 내보내기 옵션

| 옵션 | 설명 |
|------|------|
| `--output-format` | 결과 형식 (`csv`, `json`) |
| `--output-file` | 결과 파일 경로 |

### 5.8 유틸리티 옵션

| 옵션 | 설명 |
|------|------|
| `--print-ddl` | DDL 스크립트 출력 후 종료 |
| `--help` | 도움말 출력 |

---

## 6. 데이터베이스 준비

### 6.1 PostgreSQL

#### 사용자 및 데이터베이스 생성

```sql
-- 사용자 생성
CREATE USER testuser WITH PASSWORD 'testpass';

-- 데이터베이스 생성
CREATE DATABASE testdb OWNER testuser;

-- 권한 부여
GRANT ALL PRIVILEGES ON DATABASE testdb TO testuser;
```

### 6.2 MySQL

#### 사용자 및 데이터베이스 생성

```sql
-- 사용자 생성
CREATE USER 'testuser'@'%' IDENTIFIED BY 'testpass';

-- 데이터베이스 생성
CREATE DATABASE testdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 권한 부여
GRANT ALL PRIVILEGES ON testdb.* TO 'testuser'@'%';
FLUSH PRIVILEGES;
```

### 6.3 Oracle

#### 사용자 및 권한 생성

```sql
-- 사용자 생성
CREATE USER testuser IDENTIFIED BY testpass;

-- 권한 부여
GRANT CONNECT, RESOURCE TO testuser;
GRANT CREATE SESSION TO testuser;
GRANT CREATE TABLE TO testuser;
GRANT CREATE SEQUENCE TO testuser;
```

### 6.4 SQL Server

#### 사용자 및 데이터베이스 생성

```sql
-- 데이터베이스 생성
CREATE DATABASE testdb;

-- 로그인 생성
CREATE LOGIN testuser WITH PASSWORD = 'testpass';

-- 사용자 생성
USE testdb;
CREATE USER testuser FOR LOGIN testuser;

-- 권한 부여
ALTER ROLE db_owner ADD MEMBER testuser;
```

### 6.5 Tibero

#### 사용자 및 권한 생성

```sql
-- 사용자 생성
CREATE USER testuser IDENTIFIED BY testpass;

-- 권한 부여
GRANT CONNECT, RESOURCE TO testuser;
GRANT CREATE SESSION TO testuser;
GRANT CREATE TABLE TO testuser;
GRANT CREATE SEQUENCE TO testuser;
```

### 6.6 DB2

#### 사용자 및 권한 생성

```sql
-- 사용자 생성
CREATE USER testuser PASSWORD 'testpass';

-- 권한 부여
GRANT DBADM ON DATABASE TO USER testuser;
GRANT CONNECT ON DATABASE TO USER testuser;
```

### 6.7 SingleStore

#### 사용자 및 데이터베이스 생성

```sql
-- 사용자 생성
CREATE USER 'testuser'@'%' IDENTIFIED BY 'testpass';

-- 데이터베이스 생성
CREATE DATABASE testdb CHARACTER SET utf8mb4;

-- 권한 부여
GRANT ALL PRIVILEGES ON testdb.* TO 'testuser'@'%';
FLUSH PRIVILEGES;
```

---

## 7. 작업 모드

### 7.1 FULL (전체 작업)

모든 작업(INSERT, SELECT, UPDATE, DELETE)을 수행합니다.

```bash
--mode full
```

### 7.2 INSERT_ONLY

INSERT만 수행합니다.

```bash
--mode insert-only
```

### 7.3 SELECT_ONLY

SELECT만 수행합니다.

```bash
--mode select-only
```

### 7.4 UPDATE_ONLY

UPDATE만 수행합니다.

```bash
--mode update-only
```

### 7.5 DELETE_ONLY

DELETE만 수행합니다.

```bash
--mode delete-only
```

### 7.6 MIXED

혼합 작업을 수행합니다.

```bash
--mode mixed
```

---

## 8. 실습 예제

### 8.1 기본 테스트

#### PostgreSQL 100 스레드, 5분 테스트

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 100 \
    --test-duration 300 \
    --mode full
```

### 8.2 고성능 테스트

#### 500 스레드, 10분, INSERT_ONLY

```bash
java -Xmx4g -Xms4g -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type mysql \
    --host localhost \
    --port 3306 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 500 \
    --test-duration 600 \
    --mode insert-only \
    --batch-size 50
```

### 8.3 TPS 제한 테스트

#### 목표 5000 TPS로 제한

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 200 \
    --test-duration 300 \
    --mode full \
    --target-tps 5000
```

### 8.4 램프업 사용

#### 30초 램프업으로 점진적 부하 증가

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type oracle \
    --host localhost \
    --port 1521 \
    --sid XEPDB1 \
    --user testuser \
    --password testpass \
    --thread-count 500 \
    --test-duration 300 \
    --mode full \
    --ramp-up 30
```

### 8.5 결과 내보내기

#### JSON 형식으로 결과 저장

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 100 \
    --test-duration 300 \
    --mode full \
    --output-format json \
    --output-file results.json
```

#### CSV 형식으로 결과 저장

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 100 \
    --test-duration 300 \
    --mode full \
    --output-format csv \
    --output-file results.csv
```

### 8.6 테이블 비우기

#### 테스트 전 테이블 비우기

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 100 \
    --test-duration 300 \
    --mode full \
    --truncate
```

### 8.7 커넥션 풀 튜닝

#### 커넥션 풀 최적화

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 500 \
    --test-duration 300 \
    --mode full \
    --min-pool-size 300 \
    --max-pool-size 600 \
    --idle-timeout 30 \
    --keepalive-time 30
```

---

## 9. 결과 해석

### 9.1 콘솔 출력

#### 실시간 모니터링

```
[WARMUP] Time: 0s | TPS: 1,234 | Errors: 0 | Pool: Active=95, Idle=5
[WARMUP] Time: 1s | TPS: 4,567 | Errors: 0 | Pool: Active=98, Idle=2
...
[RUNNING] Time: 30s | TPS: 12,345 | Errors: 0 | Pool: Active=100, Idle=0
[RUNNING] Time: 31s | TPS: 11,890 | Errors: 0 | Pool: Active=99, Idle=1
...
```

#### 최종 결과

```
========================================
           FINAL RESULTS
========================================
Total Transactions:    345,678
Total Inserts:         86,419
Total Selects:         86,420
Total Updates:         86,419
Total Deletes:         86,420
Total Errors:          0

Elapsed Time:          300.0s
Avg TPS:               1,152
Post-Warmup TPS:       1,180
Peak TPS:              1,500

Latency Statistics (ms):
  Average: 86.5
  P50:     80.2
  P95:     95.1
  P99:     110.5
  Min:     10.2
  Max:     200.8

Connection Pool Statistics:
  Active Connections: 100
  Idle Connections:   100
  Total Connections:  200
  Wait Time (ms):     5.2
```

### 9.2 TPS (Transactions Per Second)

- **Average TPS**: 전체 기간 평균 TPS
- **Post-Warmup TPS**: 워밍업 제외 평균 TPS (주요 지표)
- **Peak TPS**: 최고 TPS

### 9.3 레이턴시 (Latency)

| 지표 | 설명 | 목표 |
|------|------|------|
| Average | 평균 응답 시간 | 낮을수록 좋음 |
| P50 | 50% 요청 응답 시간 | 100ms 이하 |
| P95 | 95% 요청 응답 시간 | 200ms 이하 |
| P99 | 99% 요청 응답 시간 | 500ms 이하 |
| Min/Max | 최소/최대 응답 시간 | 범위 확인 |

### 9.4 커넥션 풀 상태

- **Active Connections**: 현재 사용 중인 커넥션
- **Idle Connections**: 유휴 커넥션
- **Total Connections**: 전체 커넥션 수
- **Wait Time (ms)**: 커넥션 대기 시간 (낮을수록 좋음)

---

## 10. 일반적인 사용 사례

### 10.1 OLTP 워크로드 시뮬레이션

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 200 \
    --test-duration 600 \
    --mode full \
    --warmup 60
```

### 10.2 배치 데이터 로드

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type mysql \
    --host localhost \
    --port 3306 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 100 \
    --test-duration 300 \
    --mode insert-only \
    --batch-size 100
```

### 10.3 읽기 전용 워크로드

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 500 \
    --test-duration 300 \
    --mode select-only
```

### 10.4 데이터베이스 마이그레이션 테스트

#### 이전 데이터베이스 (MySQL)

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type mysql \
    --host mysql-server \
    --port 3306 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 200 \
    --test-duration 300 \
    --mode full \
    --output-format json \
    --output-file mysql_results.json
```

#### 새로운 데이터베이스 (PostgreSQL)

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host postgresql-server \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --thread-count 200 \
    --test-duration 300 \
    --mode full \
    --output-format json \
    --output-file postgresql_results.json
```

---

## 11. 팁 및 모범 사례

### 11.1 테스트 전

1. **데이터베이스 상태 확인**
   - 충분한 디스크 공간
   - 적절한 메모리 할당
   - 불필요한 연결 종료

2. **시스템 리소스 확인**
   - CPU 사용량
   - 메모리 사용량
   - 네트워크 상태

### 11.2 테스트 중

1. **워밍업 필수**
   - 최소 30초 워밍업
   - 캐시 워밍 고려

2. **모니터링**
   - 실시간 TPS 확인
   - 에러 발생 여부 확인
   - 커넥션 풀 상태 확인

### 11.3 테스트 후

1. **결결 검증**
   - Post-Warmup TPS 확인
   - 레이턴시 분석
   - 에러 로그 확인

2. **데이터베이스 정리**
   - 필요시 테이블 비우기
   - 로그 확인
   - 통계 업데이트

---

## 12. 문제 해결

자세한 문제 해결 정보는 [문제 해결 가이드](TROUBLESHOOTING_GUIDE.md)를 참조하세요.

### 12.1 일반적인 문제

| 문제 | 해결 방법 |
|------|----------|
| 연결 실패 | 호스트, 포트, 자격 증명 확인 |
| 낮은 TPS | 워밍업 시간 증가, 커넥션 풀 튜닝 |
| 높은 레이턴시 | 배치 크기 최적화, 인덱스 확인 |
| 메모리 부족 | 힙 크기 증가 (`-Xmx4g`) |

---

## 13. 고급 사용법

### 13.1 프로그래밍 방식 사용

```java
import com.loadtest.DatabaseConfig;
import com.loadtest.MultiDBLoadTester;
import com.loadtest.WorkMode;

public class LoadTestExample {
    public static void main(String[] args) throws Exception {
        // 설정 생성
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

        // 테스트 실행
        tester.runLoadTest(
            100,              // 스레드 수
            300,              // 테스트 시간 (초)
            WorkMode.FULL,     // 작업 모드
            false,            // truncate
            1.0,              // 모니터링 간격 (초)
            100,              // 서브초 윈도우 (ms)
            30,               // 워밍업 (초)
            0,                // 램프업 (초)
            0,                // 목표 TPS
            1,                // 배치 크기
            null,             // 출력 형식
            null              // 출력 파일
        );
    }
}
```

### 13.2 DDL 출력

```bash
java -jar java/target/multi-db-load-tester-0.2.3.jar \
    --db-type postgresql \
    --host localhost \
    --port 5432 \
    --database testdb \
    --user testuser \
    --password testpass \
    --print-ddl
```

---

## 14. 추가 리소스

- [README.md](README.md) - 빠른 시작 가이드
- [API_DOCUMENTATION.md](API_DOCUMENTATION.md) - API 참조
- [TROUBLESHOOTING_GUIDE.md](TROUBLESHOOTING_GUIDE.md) - 문제 해결
- [PERFORMANCE_TUNING_GUIDE.md](PERFORMANCE_TUNING_GUIDE.md) - 성능 튜닝
- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - 프로젝트 구조

---

## 15. 지원

### 15.1 버그 보고

버그를 보고하려면 다음 정보를 포함하세요:
- OS 및 버전
- Java 버전
- 데이터베이스 유형 및 버전
- 사용된 명령행 인자
- 전체 스택 트레이스

### 15.2 질문

질문이 있는 경우 [GitHub Issues](https://github.com/YeonHongMin/Multidb-HA-Workload-Testing/issues)를 사용하세요.

---

**문서 종료**
