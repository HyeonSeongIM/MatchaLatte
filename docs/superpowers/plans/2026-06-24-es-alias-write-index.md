# ES alias write-index 정합성 수정 구현 플랜

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 실시간 색인 대상을 날짜 인덱스에서 `products` alias로 변경하고, alias 부재 시 fail-fast + 명시 로그로 조용한 유실을 방지한다.

**Architecture:** `SyncScheduleHelper`의 bulk 대상을 `"products"` alias로 고정해 ES가 라우팅을 처리하도록 한다. `SyncAliasManager.swapAlias()`에 `isWriteIndex(true)`를 추가해 다중 인덱스 상태에서도 쓰기 대상이 명확하다. `EsAliasHealthChecker`가 앱 기동 시 alias 존재를 확인하고, 없으면 즉시 예외를 던져 기동을 차단한다.

**Tech Stack:** Java 17, Spring Boot, co.elastic.clients (Elasticsearch Java Client), Mockito (테스트)

## Global Constraints

- 모듈: `multicast-server`
- 패키지 루트: `project.matchalatte`
- 테스트 프레임워크: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- alias 이름: 문자열 리터럴 `"products"` 사용 (상수 추출 금지 — YAGNI)
- `ElasticsearchException` import: `co.elastic.clients.elasticsearch._types.ElasticsearchException`
- `BooleanResponse` import: `co.elastic.clients.transport.endpoints.BooleanResponse`
- 테스트 실행 명령: `./gradlew :multicast-server:test --rerun`

---

### Task 1: SyncAliasManager — isWriteIndex(true) 추가

**Files:**
- Modify: `multicast-server/src/main/java/project/matchalatte/domain/service/SyncAliasManager.java`
- Test: `multicast-server/src/test/java/project/matchalatte/domain/service/SyncAliasManagerTest.java`

**Interfaces:**
- Consumes: 기존 `swapAlias(String aliasName, String newIndexName)` 시그니처 유지
- Produces: 변경 없음 — 내부 동작만 변경

- [ ] **Step 1: 실패 테스트 작성**

`SyncAliasManagerTest.java`에 아래 테스트를 추가한다.

```java
@Test
@SuppressWarnings("unchecked")
void swapAlias_isWriteIndex_true_설정() throws Exception {
    ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
    GetAliasResponse mockGetAliasResponse = mock(GetAliasResponse.class);
    UpdateAliasesResponse mockUpdateResponse = mock(UpdateAliasesResponse.class);

    when(elasticsearchClient.indices()).thenReturn(mockIndicesClient);
    when(mockIndicesClient.getAlias(any(Function.class))).thenReturn(mockGetAliasResponse);
    when(mockGetAliasResponse.result()).thenReturn(Map.of());
    when(mockIndicesClient.updateAliases(any(UpdateAliasesRequest.class))).thenReturn(mockUpdateResponse);
    when(mockUpdateResponse.acknowledged()).thenReturn(true);

    syncAliasManager.swapAlias("products", "products_2026_06_24");

    ArgumentCaptor<UpdateAliasesRequest> captor = ArgumentCaptor.forClass(UpdateAliasesRequest.class);
    verify(mockIndicesClient).updateAliases(captor.capture());
    UpdateAliasesRequest request = captor.getValue();

    AddAction addAction = request.actions().stream()
        .filter(a -> a.add() != null)
        .map(Action::add)
        .findFirst()
        .orElseThrow(() -> new AssertionError("add action 없음"));

    assertThat(addAction.isWriteIndex()).isTrue();
}
```

추가 import:
```java
import co.elastic.clients.elasticsearch.indices.Action;
import co.elastic.clients.elasticsearch.indices.AddAction;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import org.mockito.ArgumentCaptor;
import java.util.Map;
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
./gradlew :multicast-server:test --rerun 2>&1 | grep -E "FAIL|PASS|swapAlias"
```

Expected: `swapAlias_isWriteIndex_true_설정` FAIL (isWriteIndex가 null 또는 false)

- [ ] **Step 3: isWriteIndex(true) 추가**

`SyncAliasManager.java` 44번 줄 수정:

```java
// 기존
updateAliasesBuilder.actions(a -> a.add(ad -> ad.index(newIndexName).alias(aliasName)));

// 수정
updateAliasesBuilder.actions(a -> a.add(ad -> ad.index(newIndexName).alias(aliasName).isWriteIndex(true)));
```

- [ ] **Step 4: 테스트 실행 — PASS 확인**

```bash
./gradlew :multicast-server:test --rerun 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add multicast-server/src/main/java/project/matchalatte/domain/service/SyncAliasManager.java \
        multicast-server/src/test/java/project/matchalatte/domain/service/SyncAliasManagerTest.java
git commit -m "[feat] : swapAlias isWriteIndex(true) 추가 — 쓰기 대상 인덱스 명시"
```

---

### Task 2: SyncScheduleHelper — alias 직접 write + catch 개선

**Files:**
- Modify: `multicast-server/src/main/java/project/matchalatte/domain/service/SyncScheduleHelper.java`
- Create: `multicast-server/src/test/java/project/matchalatte/domain/service/SyncScheduleHelperTest.java`

**Interfaces:**
- Consumes: `ProductEvent` record — `event.type()` (`EventType`), `event.id()` (`Long`), `event.name()` (`String`), `event.price()` (`Long`), `event.userId()` (`Long`)
- Produces: 변경 없음 — 공개 API 동일

- [ ] **Step 1: 실패 테스트 작성**

`SyncScheduleHelperTest.java`를 신규 생성한다:

```java
package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.matchalatte.api.dto.EventType;
import project.matchalatte.api.dto.ProductEvent;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncScheduleHelperTest {

    @Mock
    private Queue<ProductEvent> productQueue;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private SyncScheduleHelper syncScheduleHelper;

    private ProductEvent sampleEvent() {
        return new ProductEvent(EventType.INSERT, 42L, "테스트상품", "설명", 10000L, 1L);
    }

    @Test
    void flush_bulk_대상이_products_alias() throws Exception {
        when(productQueue.isEmpty()).thenReturn(false, true);
        when(productQueue.poll()).thenReturn(sampleEvent());

        BulkResponse mockResponse = mock(BulkResponse.class);
        when(mockResponse.errors()).thenReturn(false);
        when(mockResponse.took()).thenReturn(3L);
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(mockResponse);

        syncScheduleHelper.scheduleFlush();

        ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
        verify(elasticsearchClient).bulk(captor.capture());
        String actualIndex = captor.getValue().operations().get(0).index().index();
        assertThat(actualIndex).isEqualTo("products");
    }

    @Test
    void flush_alias_없음_404_예외_전파_안됨() throws Exception {
        when(productQueue.isEmpty()).thenReturn(false, true);
        when(productQueue.poll()).thenReturn(sampleEvent());

        ElasticsearchException ex = mock(ElasticsearchException.class);
        when(ex.status()).thenReturn(404);
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenThrow(ex);

        assertThatCode(() -> syncScheduleHelper.scheduleFlush()).doesNotThrowAnyException();
    }

    @Test
    void flush_일반_예외_전파_안됨() throws Exception {
        when(productQueue.isEmpty()).thenReturn(false, true);
        when(productQueue.poll()).thenReturn(sampleEvent());

        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenThrow(new RuntimeException("network error"));

        assertThatCode(() -> syncScheduleHelper.scheduleFlush()).doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
./gradlew :multicast-server:test --rerun 2>&1 | grep -E "FAIL|PASS|flush"
```

Expected: `flush_bulk_대상이_products_alias` FAIL (현재 `products_2026_06_24`를 대상으로 함)

- [ ] **Step 3: SyncScheduleHelper 수정**

`SyncScheduleHelper.java` 전체 `flush()` 메서드를 아래로 교체한다:

```java
private void flush() {
    List<ProductEvent> batch = new ArrayList<>();

    while (!productQueue.isEmpty()) {
        batch.add(productQueue.poll());
    }

    try {
        log.info("스케줄링 시작");
        BulkRequest.Builder br = new BulkRequest.Builder();

        String indexName = "products";

        for (ProductEvent event : batch) {
            if (event.type() == EventType.DELETE) {
                br.operations(op -> op.delete(del -> del.index(indexName).id(event.id().toString())));
                log.info("삭제 오퍼레이션 등록: Product ID {}", event.id());
            } else {
                ProductDocument doc = ProductDocument.from(event);
                br.operations(
                    op -> op.index(idx -> idx.index(indexName).id(doc.getId().toString()).document(doc)));
                log.info("색인 오퍼레이션 등록: Product ID {}", doc.getId());
            }
        }

        BulkResponse result = elasticsearchClient.bulk(br.build());

        if (result.errors()) {
            result.items().forEach(item -> {
                if (item.error() != null) {
                    if (item.status() == 429) {
                        log.error("Bulk 아이템 실패 (429 Too Many Requests — ES write 풀 포화) ID: {}, reason: {}",
                            item.id(), item.error().reason());
                    } else {
                        log.error("Bulk 아이템 실패 — ID: {}, status: {}, reason: {}",
                            item.id(), item.status(), item.error().reason());
                    }
                }
            });
        } else {
            log.info("스케줄링 완료: {}건, {}ms", batch.size(), result.took());
        }

    } catch (ElasticsearchException e) {
        if (e.status() == 404) {
            List<Long> lostIds = batch.stream()
                .map(ProductEvent::id)
                .collect(Collectors.toList());
            log.error("[CRITICAL] 'products' alias 없음 — {}건 유실. 유실 ID: {}. " +
                "validateSyncJob 실행 후 /repair로 보정 필요.", batch.size(), lostIds);
        } else {
            log.error("스케줄링 Bulk 요청 실패 — status: {}", e.status(), e);
        }
    } catch (Exception e) {
        log.error("스케줄링 Bulk 요청 실패", e);
    }
}
```

불필요한 import 제거, 신규 import 추가:

```java
// 제거
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 추가
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import java.util.stream.Collectors;
```

- [ ] **Step 4: 테스트 실행 — PASS 확인**

```bash
./gradlew :multicast-server:test --rerun 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add multicast-server/src/main/java/project/matchalatte/domain/service/SyncScheduleHelper.java \
        multicast-server/src/test/java/project/matchalatte/domain/service/SyncScheduleHelperTest.java
git commit -m "[feat] : SyncScheduleHelper — products alias 직접 write, 404 유실 ID 로그"
```

---

### Task 3: EsAliasHealthChecker — 기동 시 fail-fast

**Files:**
- Create: `multicast-server/src/main/java/project/matchalatte/api/config/EsAliasHealthChecker.java`
- Create: `multicast-server/src/test/java/project/matchalatte/api/config/EsAliasHealthCheckerTest.java`

**Interfaces:**
- Consumes: `ElasticsearchClient.indices().existsAlias(fn).value()` — `boolean`
- Produces: `ApplicationRunner` 구현체 — Spring Boot 기동 시 자동 실행

- [ ] **Step 1: 실패 테스트 작성**

`EsAliasHealthCheckerTest.java` 신규 생성:

```java
package project.matchalatte.api.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsAliasHealthCheckerTest {

    @Mock
    private ElasticsearchClient esClient;

    @InjectMocks
    private EsAliasHealthChecker healthChecker;

    @Test
    @SuppressWarnings("unchecked")
    void alias_존재시_정상_기동() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
        BooleanResponse mockBoolResponse = mock(BooleanResponse.class);
        when(esClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.existsAlias(any(Function.class))).thenReturn(mockBoolResponse);
        when(mockBoolResponse.value()).thenReturn(true);

        assertThatCode(() -> healthChecker.run(mock(ApplicationArguments.class)))
            .doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void alias_없을시_IllegalStateException_발생() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
        BooleanResponse mockBoolResponse = mock(BooleanResponse.class);
        when(esClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.existsAlias(any(Function.class))).thenReturn(mockBoolResponse);
        when(mockBoolResponse.value()).thenReturn(false);

        assertThatThrownBy(() -> healthChecker.run(mock(ApplicationArguments.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("'products'")
            .hasMessageContaining("mysqlToEsJob");
    }
}
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인 (컴파일 오류)**

```bash
./gradlew :multicast-server:compileTestJava 2>&1 | grep "error:"
```

Expected: `EsAliasHealthChecker` 클래스를 찾을 수 없음 오류

- [ ] **Step 3: EsAliasHealthChecker 구현**

`EsAliasHealthChecker.java` 신규 생성:

```java
package project.matchalatte.api.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

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

- [ ] **Step 4: 테스트 실행 — PASS 확인**

```bash
./gradlew :multicast-server:test --rerun 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add multicast-server/src/main/java/project/matchalatte/api/config/EsAliasHealthChecker.java \
        multicast-server/src/test/java/project/matchalatte/api/config/EsAliasHealthCheckerTest.java
git commit -m "[feat] : EsAliasHealthChecker — 기동 시 products alias 부재 fail-fast"
```
