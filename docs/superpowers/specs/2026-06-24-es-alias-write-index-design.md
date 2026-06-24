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

alias 부재 상태에서 bulk 요청이 들어오면 catch 블록에서 alias 부재를 명시 구분해 로그.

```java
catch (ElasticsearchException e) {
    if (e.getMessage().contains("no such index")) {
        log.error("[CRITICAL] 'products' alias가 존재하지 않음. 이벤트 {}건 유실.", batch.size());
    } else {
        log.error("스케줄링 Bulk 요청 실패", e);
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

`ApplicationRunner` 구현체. 앱 시작 시 `products` alias 존재 여부를 확인하고, 없으면 즉시 예외를 던져 기동을 중단. "조용히 기동됐다가 이벤트 유실"을 방지.

```java
@Component
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

---

## 운영 선행 조건

`EsAliasHealthChecker`가 코드로 강제하므로 별도 문서 규칙 불필요.  
`multicast-server` 최초 기동 전 반드시 `mysqlToEsJob`을 한 번 실행해 alias를 생성해야 함.

---

## 변경 파일 목록

| 파일 | 유형 | 변경 내용 |
|------|------|-----------|
| `SyncScheduleHelper.java` | 수정 | index target → `"products"`, alias 부재 예외 구분 로그 |
| `SyncAliasManager.java` | 수정 | `swapAlias()`에 `isWriteIndex(true)` 추가 |
| `EsAliasHealthChecker.java` | 신규 | 기동 시 alias 존재 확인 + fail-fast |
