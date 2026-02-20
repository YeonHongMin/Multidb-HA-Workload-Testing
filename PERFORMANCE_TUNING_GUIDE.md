# 성능 튜닝 가이드
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

본 문서는 멀티 데이터베이스 워크로드 테스트 도구의 성능을 최적화하는 방법을 설명합니다. 클라이언트 측(JVM, 커넥션 풀)과 데이터베이스 측 튜닝 방법을 다룹니다.

---

## 2. 테스트 도구 튜닝

### 2.1 JVM 힙 크기 설정

#### 기본 설정
```bash
java -Xmx2g -Xms2g -jar multi-db-load-tester.jar ...
```

#### 권장 설정

| 스레드 수 | 최소 힙 | 최대 힙 | 설명 |
|-----------|---------|---------|------|
| 1-100 | 1GB | 2GB | 소규모 테스트 |
| 100-500 | 2GB | 4GB | 중규모 튜너링 |
| 500-1000 | 4GB | 8GB | 대규모 테스트 |
| 1000+ | 8GB | 16GB | 초대규모 테스트 |

#### GC 튜닝

```bash
# G1GC (Java 9+ 기본)
java -Xmx4g -Xms4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -jar multi-db-load-tester.jar ...

# ZGC (Java 15+)
java -Xmx4g -Xms4g \
    -XX:+UseZGC \
    -jar multi-db-load-tester.jar ...
```

---

### 2.2 커넥션 풀 튜닝

#### 풀 크기 계산

```
maxPoolSize = threadCount * 1.2 ~ 1.5
minPoolSize = threadCount * 0.5 ~ 0.8
```

#### 예시 설정

| 스레드 수 | min-pool-size | max-pool-size | 목표 TPS |
|-----------|---------------|---------------|----------|
| 100 | 50 | 150 | 5,000+ |
| 200 | 100 | 300 | 10,000+ |
| 500 | 250 | 750 | 25,000+ |
| 1000 | 500 | 1500 | 50,000+ |

#### 커넥션 수명 관리

```bash
# 일반적인 웹 애플리케이션
--max-lifetime 1800    # 30분
--idle-timeout 30      # 30초
--keepalive-time 30    # 30초

# 고빈도 단기 테스트
--max-lifetime 600     # 10분
--idle-timeout 10      # 10초
--keepalive-time 30    # 30초

# 장기 테스트
--max-lifetime 3600    # 60분
--idle-timeout 60      # 60초
--keepalive-time 30    # 30초
```

#### 누수 감지

```bash
# 커넥션 누수 감지 활성화
--leak-detection-threshold 30    # 30초 후 감지
```

---

### 2.3 스레드 튜닝

#### 스레드 수 결정

```
최적 스레드 수 = CPU 코어 수 * (1 + 대기 시간 / CPU 시간)
```

#### 실무 가이드라인

| 데이터베이스 유형 | CPU 바운드 | I/O 바운드 |
|------------------|------------|------------|
| 계산 집약적 SELECT | 코어 수 | 코어 수 * 2 |
| 단순 SELECT | 코어 수 * 2 | 코어 수 * 4 |
| INSERT/UPDATE | 코어 수 * 2 | 코어 수 * 8 |
| 혼합 작업 | 코어 수 * 3 | 코어 수 * 6 |

#### 램프업 사용

```bash
# 점진적 스레드 증가
--ramp-up 30            # 30초 동안 점진적 증가
--thread-count 500       # 최종 500개 스레드
```

---

### 2.4 TPS 제어

#### 토큰 버킷 사용

```bash
# 목표 TPS 제한
--target-tps 5000       # 초당 5,000 트랜잭션 제한
--thread-count 200       # 여유 있는 스레드 수
```

#### 무제한 모드

```bash
# 최대 성능 측정
--target-tps 0          # 무제한 (기본값)
--thread-count 500
```

---

### 2.5 배치 크기 튜닝

#### INSERT 성능 최적화

| 배치 크기 | 사용 사례 | 권장 |
|-----------|-----------|------|
| 1 | OLTP, 실시간 작업 | 낮은 레이턴시 우선 |
| 10-50 | 일반 배치 처리 | 균형 |
| 100-500 | 대량 로드 | 높은 TPS 우선 |
| 1000+ | 초기 데이터 로드 | 최대 처리량 |

#### 예시

```bash
# OLTP 시뮬레이션
--batch-size 1
--mode full

# 배치 로드 테스트
--batch-size 100
--mode insert-only

# 초기 데이터 로드
--batch-size 500
--mode insert-only
```

---

### 2.6 워밍업 및 테스트 기간

#### 워밍업 기간

| 테스트 유형 | 권장 워밍업 | 이유 |
|------------|-------------|------|
| 소규모 테스트 (최대 1,000 TPS) | 30초 | 캐시 워밍 |
| 중규모 테스트 (1,000-10,000 TPS) | 60초 | JIT 컴파일, 캐시 |
| 대규모 테스트 (10,000+ TPS) | 120초 | 시스템 안정화 |

#### 테스트 기간

| 테스트 유형 | 최소 기간 | 권장 기간 |
|------------|-----------|-----------|
| 빠른 테스트 | 5분 | 10분 |
| 표준 테스트 | 10분 | 20분 |
| 장기 안정성 | 30분 | 60분 |

---

## 3. 데이터베이스 튜닝

### 3.1 PostgreSQL 튜닝

#### 연결 설정 (postgresql.conf)

```ini
# 최대 커넥션 수
max_connections = 200

# 공유 버퍼 (시스템 메모리의 25%)
shared_buffers = 2GB

# 효과적 캐시 크기 (시스템 메모리의 50%)
effective_cache_size = 4GB

# 유지 보수 작업 메모리
maintenance_work_mem = 512MB

# WAL 버퍼
wal_buffers = 16MB

# 체크포인트 타겟
checkpoint_completion_target = 0.9

# 통계 샘플링
default_statistics_target = 100

# 병렬 쿼리
max_parallel_workers_per_gather = 4
```

#### 쿼리 튜닝

```sql
-- 인덱스 확인
EXPLAIN ANALYZE SELECT * FROM load_test WHERE id = 1000;

-- 테이블 분석
ANALYZE load_test;

-- VACUUM
VACUUM ANALYZE load_test;
```

#### 트랜잭션 튜닝

```sql
-- 자동 커밋 비활성화 (JDBC 설정)
-- 자동 VACUUM 설정
autovacuum = on
autovacuum_analyze_scale_factor = 0.1
```

---

### 3.2 MySQL 튜닝

#### InnoDB 설정 (my.cnf)

```ini
[mysqld]
# 연결 설정
max_connections = 200

# InnoDB 버퍼 풀 (시스템 메모리의 70-80%)
innodb_buffer_pool_size = 4G

# 로그 파일 크기
innodb_log_file_size = 1G
innodb_log_buffer_size = 256M

# 플러시 방법
innodb_flush_method = O_DIRECT

# 플러시 타이밍
innodb_flush_log_at_trx_commit = 2

# 동시 스레드
innodb_thread_concurrency = 0

# I/O 용량
innodb_io_capacity = 2000
innodb_io_capacity_max = 4000
```

#### 쿼리 캐시

```ini
# 쿼리 캐시 비활성화 (MySQL 8.0 이후 제거됨)
query_cache_size = 0
query_cache_type = 0
```

#### 테이블 최적화

```sql
-- 테이블 분석
ANALYZE TABLE load_test;

-- 테이블 최적화
OPTIMIZE TABLE load_test;

-- 인덱스 확인
SHOW INDEX FROM load_test;

-- 실행 계획
EXPLAIN SELECT * FROM load_test WHERE id = 1000;
```

---

### 3.3 Oracle 튜닝

#### 초기화 파라미터

```sql
-- PGA (Process Global Area)
ALTER SYSTEM SET pga_aggregate_target = 2G SCOPE=SPFILE;

-- SGA (System Global Area)
ALTER SYSTEM SET sga_target = 4G SCOPE=SPFILE;

-- 공유 풀
ALTER SYSTEM SET shared_pool_size = 1G SCOPE=SPFILE;

-- 데이터베이스 버퍼 캐시
ALTER SYSTEM SET db_cache_size = 2G SCOPE=SPFILE;

-- 커서 수
ALTER SYSTEM SET open_cursors = 1000 SCOPE=SPFILE;

-- 프로세스 수
ALTER SYSTEM SET processes = 500 SCOPE=SPFILE;
```

#### UNDO 테이블스페이스

```sql
-- UNDO 테이블스페이스 크기 증가
ALTER SYSTEM SET undo_tablespace = undotbs1;

-- UNDO 보존 시간
ALTER SYSTEM SET undo_retention = 900; -- 15분
```

#### 테이블 스페이스

```sql
-- 테이블 스페이스 자동 세그먼트 관리
ALTER DATABASE DEFAULT TABLESPACE USERS;

-- 대형 세그먼트 관리
ALTER SYSTEM SET db_block_size = 8192;
```

---

### 3.4 SQL Server 튜닝

#### 메모리 설정

```sql
-- 최대 서버 메모리
EXEC sp_configure 'max server memory (MB)', 4096;
RECONFIGURE;

-- 최소 서버 메모리
EXEC sp_configure 'min server memory (MB)', 2048;
RECONFIGURE;
```

#### 복구 모델

```sql
-- SIMPLE 모드 (벤치마킹용)
ALTER DATABASE testdb SET RECOVERY SIMPLE;

-- 완전 모드 (프로덕션용)
ALTER DATABASE testdb SET RECOVERY FULL;
```

#### 통계 업데이트

```sql
-- 통계 업데이트
UPDATE STATISTICS load_test WITH FULLSCAN;

-- 인덱스 재구성
ALTER INDEX ALL ON load_test REORGANIZE;

-- 인덱스 재구성
ALTER INDEX ALL ON load_test REBUILD;
```

---

### 3.5 Tibero 튜닝

#### 메모리 설정

```sql
-- 시스템 영역
ALTER SYSTEM SET MEMORY_TARGET = 4G;

-- 공유 풀
ALTER SYSTEM SET SHARED_POOL_SIZE = 1G;

-- 데이터베이스 버퍼 캐시
ALTER SYSTEM SET DB_CACHE_SIZE = 2G;

-- PGA
ALTER SYSTEM SET PGA_AGGREGATE_TARGET = 1G;
```

#### 연결 설정

```sql
-- 최대 세션 수
ALTER SYSTEM SET SESSIONS = 500;

-- 프로세스 수
ALTER SYSTEM SET PROCESSES = 400;
```

---

### 3.6 SingleStore 튜닝

#### 리소스 관리

```sql
-- 메모리 제한 설정
SET GLOBAL max_allowed_packet = 256M;

-- 정렬 버퍼
SET GLOBAL sort_buffer_size = 256M;

-- 조인 버퍼
SET GLOBAL join_buffer_size = 256M;
```

#### 커넥션 제한

```sql
-- 최대 커넥션 수
SET GLOBAL max_connections = 200;
```

---

## 4. OS 튜닝

### 4.1 Linux 커널 튜닝

#### 파일 디스크립터 제한

```bash
# /etc/security/limits.conf
* soft nofile 65536
* hard nofile 65536
```

#### 커널 파라미터 (/etc/sysctl.conf)

```bash
# 네트워크 최적화
net.core.somaxconn = 65535
net.core.netdev_max_backlog = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 30

# 메모리 관리
vm.swappiness = 10
vm.dirty_ratio = 15
vm.dirty_background_ratio = 5
```

#### 디스크 스케줄러

```bash
# SSD용
echo deadline > /sys/block/sda/queue/scheduler

# NVMe SSD용
echo none > /sys/block/nvme0n1/queue/scheduler
```

---

### 4.2 Windows 튜닝

#### 가상 메모리

1. 시스템 속성 → 고급 → 성능 → 가상 메모리
2. 페이징 파일 크기: 실제 메모리의 1.5-2배
3. 드라이브: 고속 SSD

#### 서비스 최적화

- 불필요한 서비스 비활성화
- Windows Defender 실시간 보호 (테스트 중일 때만 비활성화)

---

## 5. 모니터링 및 분석

### 5.1 JVM 모니터링

#### jstat

```bash
# GC 통계 모니터링
jstat -gcutil <pid> 1000

# 힙 통계
jstat -gc <pid> 1000
```

#### jmap

```bash
# 힙 덤프
jmap -dump:format=b,file=heap.hprof <pid>

# 힙 사용량
jmap -heap <pid>
```

#### JConsole / JVisualVM

```
JConsole: jconsole
JVisualVM: jvisualvm
```

---

### 5.2 데이터베이스 모니터링

#### PostgreSQL

```sql
-- 활성 세션
SELECT * FROM pg_stat_activity;

-- 테이블 크기
SELECT relname, pg_size_pretty(pg_total_relation_size(relid)) 
FROM pg_catalog.pg_statio_user_tables;

-- 쿼리 성능
SELECT query, calls, total_time, mean_time 
FROM pg_stat_statements 
ORDER BY total_time DESC LIMIT 10;
```

#### MySQL

```sql
-- 프로세스 목록
SHOW PROCESSLIST;

-- 연결 통계
SHOW STATUS LIKE 'Threads%';
SHOW STATUS LIKE 'Connections';

-- InnoDB 상태
SHOW ENGINE INNODB STATUS;
```

---

### 5.3 OS 모니터링

#### Linux

```bash
# CPU 사용량
top
htop

# 메모리 사용량
free -h

# 디스크 I/O
iostat -x 1

# 네트워크
iftop
nethogs

# 시스템 로드
uptime
```

#### Windows

```bash
# 작업 관리자
taskmgr

# 성능 모니터
perfmon

# 리소스 모니터
resmon
```

---

## 6. 성능 벤치마킹 절차

### 6.1 테스트 전 준비

1. **베이스라인 측정**
   - 기본 설정으로 테스트 실행
   - 결과 기록

2. **변경 후 테스트**
   - 단일 변경 사항 적용
   - 동일 조건으로 테스트 실행

3. **비교 및 분석**
   - 변경 전후 결과 비교
   - 개선 효과 확인

---

### 6.2 테스트 체크리스트

- [ ] 데이터베이스 재시작
- [ ] OS 튜닝 적용
- [ ] 데이터베이스 튜닝 적용
- [ ] 캐시 비우기
- [ ] 베이스라인 테스트 실행
- [ ] 워밍업 기간 충분히 설정
- [ ] 반복 테스트 (최소 3회)
- [ ] 결과 평균 및 표준 편차 계산

---

### 6.3 결과 해석

#### TPS 향상

```
TPS 향상률 = (새 TPS - 기존 TPS) / 기존 TPS * 100%
```

| 향상률 | 해석 |
|--------|------|
| < 5% | 미미한 개선 |
| 5-20% | 유의미한 개선 |
| 20-50% | 큰 개선 |
| > 50% | 매우 큰 개선 |

#### 레이턴시 감소

```
레이턴시 감소율 = (기존 레이턴시 - 새 레이턴시) / 기존 레이턴시 * 100%
```

---

## 7. 일반적인 튜닝 팁

### 7.1 커넥션 풀

```bash
# 너무 큰 풀은 역효과
# 스레드 수보다 1.2-1.5배 정도가 적절

# 유휴 타임아웃은 너무 길지 않게
--idle-timeout 30
```

### 7.2 배치 크기

```bash
# 배치 크기가 너무 크면 메모리 부족
# 적절한 배치 크기: 10-100
--batch-size 50
```

### 7.3 워밍업

```bash
# 워밍업은 필수
# 최소 30초, 대규모 테스트는 120초 이상
--warmup 60
```

### 7.4 반복 테스트

```bash
# 단일 테스트는 신뢰할 수 없음
# 최소 3회 반복 후 평균 계산
```

---

## 8. 고급 튜닝 시나리오

### 8.1 초고성능 TPS 테스트

#### 설정

```bash
# JVM
-Xmx16g -Xms16g -XX:+UseG1GC -XX:MaxGCPauseMillis=100

# 커넥션 풀
--min-pool-size 500
--max-pool-size 800
--thread-count 600

# 테스트 설정
--warmup 120
--test-duration 300
--batch-size 10
--mode insert-only
```

#### 데이터베이스 튜닝

```sql
-- PostgreSQL
ALTER SYSTEM SET max_connections = 1000;
ALTER SYSTEM SET shared_buffers = 8GB;
ALTER SYSTEM SET wal_buffers = 64MB;
```

---

### 8.2 낮은 레이턴시 테스트

#### 설정

```bash
# JVM
-Xmx4g -Xms4g -XX:+UseZGC

# 커넥션 풀
--min-pool-size 200
--max-pool-size 300
--thread-count 200

# 테스트 설정
--warmup 60
--test-duration 300
--batch-size 1
--mode select-only
```

#### 데이터베이스 튜닝

```sql
-- 쿼리 캐시 활용 (MySQL)
-- 메모리 할당 늘리기
-- 인덱스 확인 및 최적화
```

---

## 9. 성능 병목 식별

### 9.1 CPU 병목

#### 증상
- CPU 사용량 100%
- TPS가 증가하지 않음
- 스레드 수 증가 시 성능 저하

#### 해결 방법
- 스레드 수 감소
- 배치 크기 증가
- 데이터베이스 쿼리 최적화

---

### 9.2 I/O 병목

#### 증상
- 디스크 I/O 사용량 100%
- 디스크 대기 시간 길음
- 높은 레이턴시

#### 해결 방법
- SSD 사용
- WAL 버퍼 증가
- 배치 크기 증가
- 인덱스 최적화

---

### 9.3 네트워크 병목

#### 증상
- 네트워크 지연 길음
- 원격 데이터베이스에서 낮은 TPS
- 패킷 손실

#### 해결 방법
- 네트워크 최적화
- 클라이언트와 DB 서버 근접 배치
- 배치 크기 증가

---

### 9.4 메모리 병목

#### 증상
- OutOfMemoryError
- 높은 GC 비율
- 스와핑 발생

#### 해결 방법
- 힙 크기 증가
- 스레드 수 감소
- GC 튜닝

---

## 10. 요약

### 10.1 빠른 튜닝 가이드

1. **JVM 튜닝**
   ```bash
   java -Xmx4g -Xms4g -XX:+UseG1GC -jar multi-db-load-tester.jar ...
   ```

2. **커넥션 풀**
   ```bash
   --max-pool-size 300 --thread-count 200
   ```

3. **워밍업**
   ```bash
   --warmup 60
   ```

4. **반복 테스트**
   - 최소 3회 실행 후 평균 계산

---

### 10.2 모범 사례

- 워밍업 필수 (최소 30초)
- 반복 테스트 (최소 3회)
- 단일 변수 변경
- 베이스라인 기록
- 결과 문서화

---

**문서 종료**
