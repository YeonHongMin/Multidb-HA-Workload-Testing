# 작업 현황 (Tasks Status)

## 프로젝트 현황 요약

| 항목 | 상태 |
|------|------|
| **현재 버전** | v0.2.3 |
| **마지막 업데이트** | 2025-12-29 |
| **전체 진행률** | 95% |

---

## 완료된 작업

### v0.2.3 (2025-12-29)

- [x] SingleStore 데이터베이스 지원 추가
  - [x] SingleStore JDBC 드라이버 통합 (1.2.1)
  - [x] `SingleStoreAdapter.java` 구현
  - [x] 커넥션 풀 크기 제한 (32개)
  - [x] `run_singlestore_test.sh` 실행 스크립트 추가
  - [x] pom.xml에 SingleStore 의존성 추가
  - [x] README.md 업데이트
  - [x] PROJECT_STRUCTURE.md 업데이트

### v0.2.2 (2025-12-19)

- [x] 결과 통계 출력 기준 개선
  - [x] Average TPS: Warmup 제외(Post-Warmup) 기준 출력
  - [x] Latency 통계: Warmup 제외 기준 집계/출력

### v0.2 (2025-12-15)

- [x] 테이블 TRUNCATE 옵션 추가
  - [x] `--truncate` 옵션 구현
  - [x] 각 DB별 시퀀스/ID 리셋 로직 구현
- [x] HikariCP 커넥션 관리 개선
  - [x] `--idle-timeout` 옵션 추가
  - [x] `--keepalive-time` 옵션 추가
- [x] DB 재시작 복구 개선
  - [x] `Connection.isValid()` 검증 추가
  - [x] 연속 에러 임계값 조정
- [x] 모니터링 출력 개선
  - [x] 구간별 변화량(delta) 출력
  - [x] `[WARMUP]`/`[RUNNING]` 상태 표시
- [x] 스키마 관리 개선
  - [x] 기존 스키마 자동 재사용

### v0.1 (2025-12-14)

- [x] 초기 릴리스
  - [x] 6개 데이터베이스 지원 (Oracle, PostgreSQL, MySQL, SQL Server, Tibero, IBM DB2)
  - [x] HikariCP 기반 커넥션 풀링
  - [x] 6가지 작업 모드
  - [x] 워밍업, Ramp-up, Rate Limiting
  - [x] 배치 INSERT 지원
  - [x] CSV/JSON 결과 내보내기

---

## 진행 중인 작업

현재 진행 중인 작업 없음.

---

## 계획된 작업
*** 계획만 있음. 추가로 할 것 같지 않음 ***

### 우선순위: 높음 (High)

- [ ] 분산 부하 테스트 지원
  - [ ] 다중 클라이언트 조율
  - [ ] 결과 집계

### 우선순위: 중간 (Medium)

- [ ] 추가 데이터베이스 지원
  - [ ] MariaDB
  - [ ] CockroachDB
  - [ ] YugabyteDB
- [ ] Prometheus 메트릭 내보내기
  - [ ] `/metrics` 엔드포인트
  - [ ] Grafana 대시보드 템플릿

### 우선순위: 낮음 (Low)

- [ ] 웹 대시보드
  - [ ] 실시간 모니터링 UI
  - [ ] 테스트 결과 시각화
- [ ] Docker 이미지
  - [ ] Dockerfile 작성
  - [ ] Docker Compose 예제
- [ ] Kubernetes 지원
  - [ ] Helm 차트

---

## 알려진 이슈

| 이슈 ID | 설명 | 상태 | 우선순위 |
|---------|------|------|----------|
| - | 현재 알려진 이슈 없음 | - | - |

---

## 테스트 현황

### 단위 테스트

| 모듈 | 상태 | 커버리지 |
|------|------|----------|
| DatabaseAdapter | 수동 테스트 | - |
| LoadTestWorker | 수동 테스트 | - |
| PerformanceCounter | 수동 테스트 | - |

### 통합 테스트

| 데이터베이스 | 테스트 상태 |
|--------------|-------------|
| Oracle | ✅ 완료 |
| PostgreSQL | ✅ 완료 |
| MySQL | ✅ 완료 |
| SQL Server | ✅ 완료 |
| Tibero | ✅ 완료 |
| IBM DB2 | ✅ 완료 |
| SingleStore | ✅ 완료 |

---

## 릴리스 체크리스트

다음 릴리스 전 확인 사항:

- [ ] README.md 버전 업데이트
- [ ] PROJECT_STRUCTURE.md 버전 업데이트
- [ ] pom.xml 버전 업데이트
- [ ] 버전 히스토리 추가
- [ ] 빌드 테스트 (`mvn clean package`)
- [ ] 모든 DB 어댑터 동작 확인
- [ ] 실행 스크립트 테스트
