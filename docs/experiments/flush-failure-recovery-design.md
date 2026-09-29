# 설계 — flush() Bulk 실패 시 배치 유실 방지

[REPORT.md](./REPORT.md)의 ES refresh/동기-비동기 실험과는 별개로, 실제 운영 코드
`multicast-server`의 `SyncScheduleHelper.flush()`에서 발견된 구조적 공백(배치 유실)에
대한 해결 설계다.

## 문제

`SyncScheduleHelper.flush()`(10초 주기 `@Scheduled`)는 큐에서 이벤트를 꺼내 로컬
`batch`에 담고 ES에 벌크 색인한다. 실패 시(전체 예외 또는 `BulkResponse.errors()`로
드러나는 부분 아이템 실패) **로그만 남기고 종료**된다 — 이미 큐에서 꺼낸 이벤트는
재시도도, 재큐잉도 되지 않고 그대로 사라진다.

기존에 안전망이 없는 것은 아니다:

- `GhostCheckTasklet`/`ValidateItemWriter`(`validateSyncJob`) — ES↔RDB 전수 대조로
  GHOST(ES에만 존재)/MISSING(RDB에만 존재) 탐지, `validate_report` 테이블에 기록
- `RepairService`(`/repair`) — 가장 최근 `run_at`의 MISSING 항목을 RDB에서 재조회해
  ES에 재색인

문제는 이 둘 다 **수동 트리거**라는 점이다. 코드베이스 전체에서 `@Scheduled`는
`SyncService`의 10초 flush 하나뿐이고(`SyncController` 확인 결과 `/validate`,
`/repair` 모두 `@PostMapping`), 자동으로 도는 정기 검증 배치가 없다. 즉 flush
실패~운영자가 수동으로 `/validate` → `/repair`를 실행할 때까지 **불일치 창의 길이가
전적으로 운영자 개입에 의존**한다.

## 목표 / 비범위

**목표**: 새 인프라(DLQ, 아웃박스, 메시지 큐 등) 없이, 기존 컴포넌트
(`validate_report` 테이블, `ValidateReportHelper`, `RepairService`)를 재사용해서

1. 일시적 오류(네트워크 hiccup, 순간적인 429 등)는 flush 내부에서 자체 복구
2. 재시도로도 못 살린 건은 다음 정기 검증을 기다리지 않고 **즉시** `validate_report`에
   기록해 `/repair`로 바로 복구 가능하게 만든다

**비범위**:

- 무한 재시도/재큐잉 — 큐가 `ArrayBlockingQueue`(용량 제한)라 실패 이벤트를 다시
  큐에 넣는 방식은 용량 초과 시 또 다른 유실을 만든다. 채택하지 않는다.
- `validate`/`repair` 자동 스케줄링 도입 — 이번 설계 범위 밖. "알려진 한계"에서
  별도로 언급.
- GHOST(삭제 실패) 자동 복구(ES 문서 삭제) — 아래 "알려진 한계" 참고. 현재
  `GhostCheckTasklet`이 탐지한 GHOST도 자동 삭제되지 않으므로, 이번 설계도 동일한
  수준(기록까지만)으로 범위를 맞춘다.

## 설계

### 1. 유한 재시도

`flush()` 내부에서 bulk 실패 시, **실패한 이벤트만** 추려 최대 2회 재시도(백오프
300ms → 800ms).

- 전체 예외(현재 `catch (ElasticsearchException e)` / `catch (Exception e)`) →
  batch 전체가 재시도 대상
- 부분 실패(`result.errors() == true`) → `item.error() != null`인 아이템만 재시도
  대상. `BulkResponseItem.id()`는 색인/삭제 오퍼레이션에 넣은 `event.id().toString()`과
  동일하므로 `Map<String, ProductEvent>`로 실패 이벤트를 복원할 수 있다.

10초 주기 스케줄러가 이 앱에서 유일한 `@Scheduled` 작업이라 재시도 백오프로 인한
짧은(최대 ~1.1초) 블로킹이 다른 스케줄 작업과 경합하지 않는다 — 별도 스레드풀/비동기
재시도 없이 동기 루프로 충분하다.

### 2. 재시도 소진 시 즉시 기록

재시도를 다 썼는데도 실패한 이벤트는 `ValidateReportHelper`의 기존 메서드를 그대로
재사용해 기록한다(신규 SQL/메서드 없음):

- `CREATE`/`UPDATE` 실패 → `insertMissing(id, runAt)` — RDB에는 있는데 ES엔 없는
  상태와 정확히 일치
- `DELETE` 실패 → `insertGhost(id, runAt)` — ES에 여전히 남아있으면 안 되는 상태와
  정확히 일치(기존 `GhostCheckTasklet`이 탐지하는 것과 같은 issue_type)

`runAt`은 `ValidateBatchHelper`와 동일 포맷(`yyyy-MM-dd'T'HH:mm:ss`)으로 현재 시각을
새로 생성한다 — 이 값이 `validate_report`에서 새 `run_at` 그룹이 되고,
`RepairService.repair()`가 "가장 최근 run_at"을 그대로 읽으므로 **별도 연동 코드 없이**
`/repair` 한 번으로 CREATE/UPDATE 실패 건이 복구된다.

### 3. 의사 코드

```java
private static final int MAX_RETRY = 2;
private static final long[] BACKOFF_MS = {300, 800};

private void flush() {
    List<ProductEvent> batch = drainQueue();
    if (batch.isEmpty()) return;

    List<ProductEvent> remaining = batch;
    for (int attempt = 0; attempt <= MAX_RETRY && !remaining.isEmpty(); attempt++) {
        if (attempt > 0) {
            sleep(BACKOFF_MS[attempt - 1]);
            log.warn("flush 재시도 {}/{} — 대상 {}건", attempt, MAX_RETRY, remaining.size());
        }
        remaining = tryBulk(remaining); // 성공하면 빈 리스트, 실패분만 반환
    }

    if (!remaining.isEmpty()) {
        recordUnrecovered(remaining);
    }
}

// 기존 bulk 호출 로직 + 부분 실패 아이템만 골라 반환하도록 변경
private List<ProductEvent> tryBulk(List<ProductEvent> events) {
    try {
        BulkRequest.Builder br = new BulkRequest.Builder();
        Map<String, ProductEvent> byId = events.stream()
            .collect(Collectors.toMap(e -> e.id().toString(), e -> e, (a, b) -> b));
        // ... 기존과 동일하게 br.operations(...) 구성 ...

        BulkResponse result = elasticsearchClient.bulk(br.build());

        if (!result.errors()) {
            return List.of();
        }

        return result.items().stream()
            .filter(item -> item.error() != null)
            .map(item -> byId.get(item.id()))
            .filter(Objects::nonNull)
            .toList();

    } catch (Exception e) {
        log.error("flush Bulk 요청 실패 — {}건 재시도 대상", events.size(), e);
        return events; // 전체 예외는 batch 전체를 재시도 대상으로
    }
}

private void recordUnrecovered(List<ProductEvent> events) {
    String runAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    for (ProductEvent e : events) {
        if (e.type() == EventType.DELETE) {
            reportHelper.insertGhost(e.id(), runAt);
        } else {
            reportHelper.insertMissing(e.id(), runAt);
        }
    }
    List<Long> ids = events.stream().map(ProductEvent::id).toList();
    log.error("[CRITICAL] flush 재시도 소진 — {}건 validate_report 기록(run_at={}), " +
        "ID: {}. CREATE/UPDATE는 /repair로 자동 복구 가능, DELETE(GHOST)는 수동 확인 필요.",
        events.size(), runAt, ids);
}
```

`tryBulk`는 기존 `flush()`의 벌크 구성/호출 로직을 그대로 옮기되, "성공이면 빈
리스트, 부분 실패면 실패 이벤트만" 반환하도록 리턴 타입만 바꾸는 정도의 변경이다 —
기존 오퍼레이션 등록 로직, 429 로깅, 404 alias 로깅은 그대로 유지한다.

## 같은 상품에 대한 중복 이벤트 — 알려진 동작 (신규 결함 아님)

한 flush 주기(10초) 안에 같은 상품 ID로 여러 이벤트(예: 생성 직후 수정)가 들어오면
`Map<String, ProductEvent>`에서 나중 이벤트가 먼저 것을 덮어쓴다. 이는 현재 코드에서도
이미 벌크 요청에 동일 id로 두 오퍼레이션이 그대로 들어가는 것과 동일한 특성이며, 이번
설계로 새로 생기는 문제가 아니다. 재시도 대상 매핑에서도 최신 이벤트 기준으로만
재시도되므로 기존 동작과 일관된다.

## 알려진 한계 (이번 설계 범위 밖)

1. **`RepairService.repair()`는 "가장 최근 run_at" 한 그룹만 처리한다.**
   `validate_report`에 "처리 완료" 상태 컬럼이 없어서다. 예를 들어 flush 실패로
   MISSING이 기록된 직후 `/validate`가 실행되어 새 `run_at`이 생기면, `/repair`는
   새 `run_at`만 보게 된다. 다만 flush가 유실시킨 상품은 실제로 ES에 없는 상태이므로
   그 다음 `/validate`도 동일 상품을 다시 MISSING으로 잡아낸다 — 데이터가
   조용히 누락되는 것은 아니고, "한 번에 처리되지 않고 다음 사이클로 넘어갈 수
   있다" 정도의 지연이다.
2. **GHOST(삭제 실패)는 기록만 되고 자동 삭제되지 않는다.** 기존
   `GhostCheckTasklet`이 탐지하는 GHOST도 동일하게 기록까지만 하므로, 이번 설계는
   기존 수준에 맞춘 것이다. `/repair`가 GHOST를 처리하도록 확장하는 것은 별도
   설계/승인이 필요한 후속 작업으로 남긴다.
3. **`/validate`, `/repair` 모두 자동 스케줄이 없다.** 이번 설계로 flush 실패는
   "다음 스케줄"이 아니라 "즉시" 기록되므로 이 한계의 영향은 크게 줄어들지만(운영자가
   `/repair`만 실행하면 되고 `/validate`를 먼저 돌릴 필요가 없어짐), 근본적으로
   `/repair` 자체를 자동으로 트리거하는 스케줄은 여전히 없다.

## 변경 파일 (구현 시)

- `SyncScheduleHelper.java` — 재시도 루프, `tryBulk` 리턴 타입 변경, `recordUnrecovered` 추가
- 생성자에 `ValidateReportHelper` 의존성 추가 필요(현재 `ElasticsearchClient`, `Queue<ProductEvent>`만 주입받음)

신규 클래스/테이블/설정 없음 — 전부 기존 컴포넌트 재사용.
