# ES 실시간 색인 alias 정합성 수정 설계

**날짜:** 2026-06-24  
**대상 모듈:** `multicast-server`

---

## 배경 및 목적

현재 `SyncScheduleHelper`는 실시간 이벤트를 `products_yyyy_MM_dd` (오늘 날짜 인덱스)에 직접 씁니다. `mysqlToEsJob`이 어제 실행됐다면 alias `products`는 `products_2026_06_23`을 가리키고, 오늘 실시간 색인분은 alias 밖 인덱스로 들어가 검색에서 누락됩니다.

```
[현재 — 버그]
mysqlToEsJob  → products_2026_06_23 ← alias: products
SyncScheduleHelper → products_2026_06_24  ← alias 밖, 검색 안 됨

[수정 후]
mysqlToEsJob  → products_2026_06_24 ← alias: products
SyncScheduleHelper → products (alias) → products_2026_06_24  ← 동일 인덱스
```

---

## 변경 내용

### 1. `SyncScheduleHelper` — alias 직접 write

실시간 bulk 대상을 `products` alias로 고정. ES가 alias → backing index 라우팅을 자동 처리.

```java
// 제거
String newIndexName = "products_" + LocalDateTime.now().format(...);

// 교체
String indexName = "products";
```

**alias 부재 시 catch 처리 — 예외 타입/status code 기반 분기:**

문자열 매칭(`getMessage().contains(...)`) 대신 `ElasticsearchException.status()`로 분기. ES Java 클라이언트는 index_not_found_exception 시 status 404를 반환.

유실 로그에는 이벤트 ID 목록을 포함해 `validateSyncJob` 보정 대상임을 명시.

```java
catch (ElasticsearchException e) {
    if (e.status() == 404) {
        List<Long> lostIds = batch.stream()
            .map(ProductEvent::id)
            .collect(Collectors.toList());
        log.error("[CRITICAL] 'products' alias 없음 — {}건 유실. " +
            "유실 ID: {}. validateSyncJob 실행 후 /repair로 보정 필요.", batch.size(), lostIds);
    } else {
        log.error("스케줄링 Bulk 요청 실패 — status: {}", e.status(), e);
    }
}
```

### 2. `SyncAliasManager.swapAlias()` — `is_write_index: true`

alias swap 시 새 인덱스에 `is_write_index: true` 명시. alias가 여러 인덱스를 가리키는 순간에도 쓰기 대상이 명확히 고정됨.

```java
// 기존
.add(ad -> ad.index(newIndexName).alias(aliasName))

// 수정
.add(ad -> ad.index(newIndexName).alias(aliasName).isWriteIndex(true))
```

`is_write_index` 없이 alias가 여러 인덱스를 가리키면 ES가 `IllegalArgumentException`을 반환. 단일 인덱스 상태라도 명시해 의도를 드러냄.

### 3. `EsAliasHealthChecker` — 기동 시 fail-fast

`ApplicationRunner` 구현체. 앱 시작 시 `products` alias 존재 여부를 확인하고, 없으면 즉시 예외를 던져 기동을 중단.

```java
@Component
@Slf4j
public class EsAliasHealthChecker implements ApplicationRunner {

    private final ElasticsearchClient esClient;

    public EsAliasHealthChecker(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean exists = esClient.indices()
            .existsAlias(a -> a.name("products")).value();
        if (!exists) {
            throw new IllegalStateException(
                "[FATAL] ES alias 'products' 없음 — mysqlToEsJob 선행 실행 필요");
        }
        log.info("ES alias 'products' 확인 완료.");
    }
}
```

**fail-fast 선택 근거 및 트레이드오프:**

이 앱의 핵심 역할은 ES 색인이다. alias가 없으면 모든 실시간 쓰기가 실패하고 데이터는 조용히 유실된다. "기동은 됐지만 이벤트가 쌓이지 않는 상태"는 장애인지 인지하기 어렵다.

| 방식 | 동작 | 위험 |
|------|------|------|
| **fail-fast (채택)** | alias 없으면 기동 차단 | 앱 전체가 뜨지 않음 — 재시작 불가, health check 실패 |
| degraded mode | alias 없어도 기동, flush마다 경고 로그 | 로그를 못 보면 이벤트 무한 유실 |

검색이 이 서버의 존재 이유이므로 fail-fast를 선택. alias 없는 상태로 기동되는 것 자체가 잘못된 운영이므로 앱 전체를 막는 결정이 맞다. 단, 이 결정은 "alias 없는 게 항상 오류"라는 전제 위에 있으므로, 향후 무중단 배포 시나리오에서 alias 생성과 앱 기동 순서를 배포 파이프라인에서 반드시 보장해야 한다.

---

## 운영 선행 조건

`multicast-server` 최초 기동 전 반드시 `mysqlToEsJob`을 한 번 실행해 alias를 생성해야 함.  
`EsAliasHealthChecker`가 코드로 강제 — 선행 조건 미충족 시 앱이 기동되지 않음.

---

## 변경 파일 목록

| 파일 | 유형 | 변경 내용 |
|------|------|-----------|
| `SyncScheduleHelper.java` | 수정 | index target → `"products"`, status code 기반 예외 분기, 유실 ID 로그 |
| `SyncAliasManager.java` | 수정 | `swapAlias()`에 `isWriteIndex(true)` 추가 |
| `EsAliasHealthChecker.java` | 신규 | 기동 시 alias 존재 확인 + fail-fast |
