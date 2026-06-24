# ES-RDB 정합성 검증 설계

**날짜:** 2026-06-24  
**대상 모듈:** `multicast-server`

---

## 배경 및 목적

현재 구조는 이벤트 기반 실시간 동기화(HTTP 전송 → 인메모리 큐 → Bulk 색인)와 Full Sync Batch(mysqlToEsJob)를 병행하지만, 전송 실패 시 재시도 없이 이벤트가 유실될 수 있다.

이를 **사전 방어(전송 보강)** 대신 **사후 감지(휴먼 검증) 방향**으로 접근한다. 어떤 데이터가 유실됐는지 탐지하고, 사람이 확인 후 누락분만 재색인하는 흐름이다.

---

## 탐지 대상

| 유형 | 정의 |
|------|------|
| MISSING | RDB에 존재하는데 ES에 없음 (CREATE/UPDATE 이벤트 유실) |
| GHOST | ES에 존재하는데 RDB에 없음 (DELETE 이벤트 유실) |

값 불일치(VALUE_MISMATCH) 탐지는 이번 범위에서 제외한다.

---

## 전체 구조

감지와 보정을 완전히 분리한다.

```
[감지] validateSyncJob  →  validate_report 테이블 기록
[보정] POST /api/internal/sync/repair  →  사람이 리포트 확인 후 수동 트리거
```

`validateSyncJob`은 `mysqlToEsJob`과 동일한 Spring Batch 골격을 사용한다. 청크 단위 처리·재시작·Lock 정책을 그대로 활용한다는 점이 핵심이다.

---

## validateSyncJob 상세

### Step 1 — rdbToEsCheckStep (누락 탐지)

```
ItemReader  : JdbcPagingItemReader
              SELECT id FROM product ORDER BY id  (청크 1000건)

ItemWriter  : ValidateItemWriter
              청크의 ID 목록 → ES mget(_source: false)
              존재하지 않는 ID → validate_report에 MISSING 기록
```

`_source: false`로 본문 없이 존재 여부만 확인해 ES 부하를 최소화한다.

### Step 2 — ghostCheckStep (고스트 탐지)

Scroll API(deprecated) 대신 **PIT + search_after** 방식을 사용한다.

```
① openPointInTime(index: "products", keep_alive: "1m")  → pit_id 획득
② loop:
     search(pit_id, sort: _id asc, search_after: [last_id], size: 1000)
     → 청크 _id 목록 → RDB IN 쿼리
     → RDB에 없는 ID → validate_report에 GHOST 기록
     → 결과 < 1000 이면 종료
③ closePointInTime(pit_id)
```

PIT를 열면 검색 시점의 인덱스 스냅샷이 고정되어, 페이지 이동 중 변경 영향을 받지 않는다.

### JobListener.afterJob()

- 수집된 MISSING/GHOST ID를 `validate_report`에 bulk insert
- 결과 요약 로그 출력 (건수 단위)
- Lock 해제

---

## validate_report 테이블

```sql
CREATE TABLE validate_report (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    run_at      DATETIME    NOT NULL,
    issue_type  ENUM('MISSING', 'GHOST') NOT NULL,
    product_id  BIGINT      NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_issue_type_product_id (issue_type, product_id)
);
```

---

## 보정 — repair 엔드포인트

```
POST /api/internal/sync/repair
```

사람이 `validate_report`를 확인한 뒤 수동 호출한다.

- `validate_report`에서 `MISSING` 항목의 product_id 목록 조회
- RDB에서 해당 ID들의 데이터 읽기
- ES Bulk API로 재색인
- GHOST는 삭제 여부를 사람이 판단하므로 자동 처리하지 않음

---

## Lock 정책

| 상황 | 처리 |
|------|------|
| validateSyncJob 실행 중 mysqlToEsJob 요청 | `fullSyncInProgress` Lock으로 차단 |
| validateSyncJob 실행 중 실시간 이벤트 색인 | 허용 (막을 수 없음) |
| 대조 중 변경된 데이터 | 다음 검증 주기에서 수렴 — 허용 |

기존 `SyncLockHelper`의 `fullSyncInProgress` AtomicBoolean을 그대로 재사용한다.

---

## 에러 처리

| 상황 | 처리 |
|------|------|
| ES mget 실패 (청크 단위) | 해당 청크 skip, 로그 기록 후 다음 청크 계속 |
| PIT 연결 끊김 | Step 실패 → Batch 재시작 시 해당 Step부터 재개 |
| repair Bulk 일부 실패 | 실패 ID 로그 + validate_report 재기록 |

---

## 트리거 방식

`validateSyncJob`은 수동 트리거(REST 엔드포인트)로 실행한다. 기존 `mysqlToEsJob`과 동일하게 `SyncController`에 엔드포인트 추가.

```
POST /api/internal/sync/validate
```

---

## 구현 파일 목록 (예상)

**multicast-server:**
- `BatchConfig` — `validateSyncJob`, Step 빈 추가
- `ValidateItemWriter` — ES mget + MISSING 수집
- `GhostCheckWriter` — PIT + search_after + GHOST 수집
- `ValidateJobListener` — afterJob 리포트 저장
- `RepairService` — validate_report 읽기 + Bulk 재색인
- `SyncController` — `/validate`, `/repair` 엔드포인트 추가

**storage/db-core:**
- `ValidateReport` 엔티티 + 리포지토리
- `validate_report` 테이블 마이그레이션
