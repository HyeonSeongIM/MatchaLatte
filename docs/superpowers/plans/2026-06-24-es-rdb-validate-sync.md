# ES-RDB 정합성 검증 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** RDB와 ES 사이의 누락(MISSING)·고스트(GHOST)·미검증(SKIPPED) 데이터를 청크 단위로 탐지하고 validate_report 테이블에 기록한 뒤, 사람이 확인 후 누락분만 Bulk 재색인할 수 있는 구조를 만든다.

**Architecture:** `validateSyncJob`은 기존 `mysqlToEsJob`과 같은 Spring Batch 골격을 사용한다. Step 1(rdbToEsCheckStep)은 RDB → ES mget으로 MISSING을 탐지하는 청크 지향 스텝, Step 2(ghostCheckStep)는 PIT + search_after로 ES → RDB를 순회하는 Tasklet이다. 감지와 보정은 완전히 분리되며 보정은 `/api/internal/sync/repair` 수동 트리거로만 실행된다.

**Tech Stack:** Spring Batch, co.elastic.clients(ElasticsearchClient), JdbcTemplate(NamedParameterJdbcTemplate), JUnit 5 + Mockito + AssertJ

## Global Constraints

- 모듈: `multicast-server` (별도 Spring Boot 앱, 포트 8082)
- JPA 없음 — validate_report 영속화는 `JdbcTemplate` / `NamedParameterJdbcTemplate` 사용
- ES 클라이언트: `co.elastic.clients.elasticsearch.ElasticsearchClient`
- 테스트 패턴: `@ExtendWith(MockitoExtension.class)`, AssertJ, `@SuppressWarnings("unchecked")` for lambda mock
- 패키지 루트: `project.matchalatte`
- Lock: 기존 `SyncLockHelper.fullSyncInProgress` 재사용
- ES 인덱스 alias: `"products"`
- 청크 사이즈: 1000
- PIT keep_alive: `"5m"`, 매 응답에서 pit_id 갱신, `finally`에서 반드시 close

---

## 파일 구조

**신규 생성:**
- `multicast-server/.../domain/service/ValidateReportHelper.java` — JdbcTemplate CRUD for validate_report
- `multicast-server/.../domain/service/ValidateItemWriter.java` — Step 1 ItemWriter (MISSING/SKIPPED 탐지)
- `multicast-server/.../domain/service/GhostCheckTasklet.java` — Step 2 Tasklet (GHOST 탐지, PIT + search_after)
- `multicast-server/.../domain/service/ValidateJobListener.java` — afterJob 요약 로그 + Lock 해제
- `multicast-server/.../domain/service/ValidateBatchHelper.java` — Lock 획득 + validateSyncJob 실행
- `multicast-server/.../domain/service/RepairService.java` — MISSING id 조회 + Bulk 재색인
- `multicast-server/src/test/.../ValidateReportHelperTest.java`
- `multicast-server/src/test/.../ValidateItemWriterTest.java`
- `multicast-server/src/test/.../GhostCheckTaskletTest.java`
- `multicast-server/src/test/.../ValidateJobListenerTest.java`
- `multicast-server/src/test/.../RepairServiceTest.java`

**수정:**
- `multicast-server/.../api/config/BatchConfig.java` — validateSyncJob, Step 빈 추가
- `multicast-server/.../api/controller/SyncController.java` — `/validate`, `/repair` 엔드포인트 추가

---

## Task 1: validate_report 테이블 DDL + ValidateReportHelper

**Files:**
- Create: `multicast-server/src/main/resources/validate_report_ddl.sql`
- Create: `multicast-server/src/main/java/project/matchalatte/domain/service/ValidateReportHelper.java`
- Test: `multicast-server/src/test/java/project/matchalatte/domain/service/ValidateReportHelperTest.java`

**Interfaces:**
- Produces:
  - `ValidateReportHelper.insertMissing(long productId, String runAt)`
  - `ValidateReportHelper.insertGhost(long productId, String runAt)`
  - `ValidateReportHelper.insertSkipped(long chunkStart, long chunkEnd, String runAt)`
  - `ValidateReportHelper.findMissingProductIds(): List<Long>`
  - `ValidateReportHelper.countByIssueType(String issueType): int`

- [ ] **Step 1: DDL 파일 작성**

`multicast-server/src/main/resources/validate_report_ddl.sql`:
```sql
CREATE TABLE IF NOT EXISTS validate_report (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    run_at     VARCHAR(20)  NOT NULL,
    issue_type ENUM('MISSING', 'GHOST', 'SKIPPED') NOT NULL,
    product_id BIGINT       NULL,
    detail     VARCHAR(255) NULL,
    PRIMARY KEY (id),
    INDEX idx_issue_type (issue_type),
    INDEX idx_run_at (run_at)
);
```

MySQL에 직접 실행한다: `mysql -u <user> -p <db> < validate_report_ddl.sql`

- [ ] **Step 2: ValidateReportHelper 실패 테스트 작성**

`src/test/.../ValidateReportHelperTest.java`:
```java
package project.matchalatte.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateReportHelperTest {

    @Mock JdbcTemplate jdbcTemplate;

    ValidateReportHelper sut;

    @BeforeEach
    void setUp() {
        sut = new ValidateReportHelper(jdbcTemplate);
    }

    @Test
    void insertMissing_정상_insert_호출() {
        sut.insertMissing(42L, "2026-06-24T10:00:00");
        verify(jdbcTemplate).update(
            contains("INSERT INTO validate_report"),
            eq("2026-06-24T10:00:00"), eq("MISSING"), eq(42L), isNull()
        );
    }

    @Test
    void insertGhost_정상_insert_호출() {
        sut.insertGhost(99L, "2026-06-24T10:00:00");
        verify(jdbcTemplate).update(
            contains("INSERT INTO validate_report"),
            eq("2026-06-24T10:00:00"), eq("GHOST"), eq(99L), isNull()
        );
    }

    @Test
    void insertSkipped_detail에_청크_범위_기록() {
        sut.insertSkipped(1001L, 2000L, "2026-06-24T10:00:00");
        verify(jdbcTemplate).update(
            contains("INSERT INTO validate_report"),
            eq("2026-06-24T10:00:00"), eq("SKIPPED"), isNull(),
            eq("chunk_start=1001, chunk_end=2000")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void findMissingProductIds_MISSING_id_목록_반환() {
        when(jdbcTemplate.queryForList(contains("issue_type = 'MISSING'"), eq(Long.class)))
            .thenReturn(List.of(1L, 2L, 3L));

        List<Long> result = sut.findMissingProductIds();

        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    void countByIssueType_건수_반환() {
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Integer.class), eq("MISSING")))
            .thenReturn(5);

        assertThat(sut.countByIssueType("MISSING")).isEqualTo(5);
    }
}
```

- [ ] **Step 3: 테스트 실행 — 실패 확인**

```bash
./gradlew :multicast-server:test --tests "*.ValidateReportHelperTest" 2>&1 | tail -20
```
Expected: FAIL (클래스 없음)

- [ ] **Step 4: ValidateReportHelper 구현**

`src/main/.../domain/service/ValidateReportHelper.java`:
```java
package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ValidateReportHelper {

    private static final String INSERT_SQL =
        "INSERT INTO validate_report (run_at, issue_type, product_id, detail) VALUES (?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public ValidateReportHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertMissing(long productId, String runAt) {
        jdbcTemplate.update(INSERT_SQL, runAt, "MISSING", productId, null);
    }

    public void insertGhost(long productId, String runAt) {
        jdbcTemplate.update(INSERT_SQL, runAt, "GHOST", productId, null);
    }

    public void insertSkipped(long chunkStart, long chunkEnd, String runAt) {
        String detail = "chunk_start=" + chunkStart + ", chunk_end=" + chunkEnd;
        jdbcTemplate.update(INSERT_SQL, runAt, "SKIPPED", null, detail);
    }

    public List<Long> findMissingProductIds() {
        return jdbcTemplate.queryForList(
            "SELECT product_id FROM validate_report WHERE issue_type = 'MISSING'",
            Long.class
        );
    }

    public int countByIssueType(String issueType) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM validate_report WHERE issue_type = ?",
            Integer.class, issueType
        );
        return count != null ? count : 0;
    }
}
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
./gradlew :multicast-server:test --tests "*.ValidateReportHelperTest" 2>&1 | tail -20
```
Expected: PASS (5 tests)

- [ ] **Step 6: 커밋**

```bash
git add multicast-server/src/main/resources/validate_report_ddl.sql \
        multicast-server/src/main/java/project/matchalatte/domain/service/ValidateReportHelper.java \
        multicast-server/src/test/java/project/matchalatte/domain/service/ValidateReportHelperTest.java
git commit -m "[feat] : validate_report DDL + ValidateReportHelper JdbcTemplate CRUD"
```

---

## Task 2: ValidateItemWriter — Step 1 MISSING/SKIPPED 탐지

**Files:**
- Create: `multicast-server/src/main/java/project/matchalatte/domain/service/ValidateItemWriter.java`
- Test: `multicast-server/src/test/java/project/matchalatte/domain/service/ValidateItemWriterTest.java`

**Interfaces:**
- Consumes: `ValidateReportHelper` (Task 1)
- Produces: `ValidateItemWriter implements ItemWriter<Long>` — Spring Batch Step 1에서 사용

- [ ] **Step 1: 실패 테스트 작성**

`src/test/.../ValidateItemWriterTest.java`:
```java
package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.mget.MultiGetError;
import co.elastic.clients.elasticsearch.core.get.GetResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.util.List;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateItemWriterTest {

    @Mock ElasticsearchClient elasticsearchClient;
    @Mock ValidateReportHelper reportHelper;

    ValidateItemWriter sut;

    @BeforeEach
    void setUp() {
        sut = new ValidateItemWriter(elasticsearchClient, reportHelper, "2026-06-24T10:00:00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void mget_결과_found_false면_insertMissing_호출() throws Exception {
        MgetResponse<Void> mockResponse = mock(MgetResponse.class);
        MultiGetResponseItem<Void> missingItem = mock(MultiGetResponseItem.class);
        GetResult<Void> getResult = mock(GetResult.class);

        when(elasticsearchClient.mget(any(Function.class), eq(Void.class))).thenReturn(mockResponse);
        when(mockResponse.docs()).thenReturn(List.of(missingItem));
        when(missingItem.isFailure()).thenReturn(false);
        when(missingItem.result()).thenReturn(getResult);
        when(getResult.found()).thenReturn(false);
        when(getResult.id()).thenReturn("42");

        sut.write(new Chunk<>(List.of(42L)));

        verify(reportHelper).insertMissing(42L, "2026-06-24T10:00:00");
        verify(reportHelper, never()).insertSkipped(anyLong(), anyLong(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void mget_예외_발생시_insertSkipped_호출() throws Exception {
        when(elasticsearchClient.mget(any(Function.class), eq(Void.class)))
            .thenThrow(new RuntimeException("ES connection refused"));

        sut.write(new Chunk<>(List.of(1L, 2L, 3L)));

        verify(reportHelper).insertSkipped(1L, 3L, "2026-06-24T10:00:00");
        verify(reportHelper, never()).insertMissing(anyLong(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void mget_item_failure면_skip_처리() throws Exception {
        MgetResponse<Void> mockResponse = mock(MgetResponse.class);
        MultiGetResponseItem<Void> failureItem = mock(MultiGetResponseItem.class);

        when(elasticsearchClient.mget(any(Function.class), eq(Void.class))).thenReturn(mockResponse);
        when(mockResponse.docs()).thenReturn(List.of(failureItem));
        when(failureItem.isFailure()).thenReturn(true);

        sut.write(new Chunk<>(List.of(5L)));

        verify(reportHelper, never()).insertMissing(anyLong(), anyString());
        verify(reportHelper, never()).insertSkipped(anyLong(), anyLong(), anyString());
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew :multicast-server:test --tests "*.ValidateItemWriterTest" 2>&1 | tail -20
```
Expected: FAIL

- [ ] **Step 3: ValidateItemWriter 구현**

`src/main/.../domain/service/ValidateItemWriter.java`:
```java
package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@Slf4j
public class ValidateItemWriter implements ItemWriter<Long> {

    private final ElasticsearchClient elasticsearchClient;
    private final ValidateReportHelper reportHelper;
    private final String runAt;

    public ValidateItemWriter(ElasticsearchClient elasticsearchClient,
                              ValidateReportHelper reportHelper,
                              String runAt) {
        this.elasticsearchClient = elasticsearchClient;
        this.reportHelper = reportHelper;
        this.runAt = runAt;
    }

    @Override
    public void write(Chunk<? extends Long> chunk) {
        List<Long> ids = (List<Long>) chunk.getItems();
        List<String> idStrings = ids.stream().map(String::valueOf).toList();

        try {
            MgetResponse<Void> response = elasticsearchClient.mget(
                m -> m.index("products").ids(idStrings),
                Void.class
            );

            for (MultiGetResponseItem<Void> item : response.docs()) {
                if (item.isFailure()) {
                    log.warn("mget 개별 항목 오류: id={}", item.failure().id());
                    continue;
                }
                if (!item.result().found()) {
                    long missingId = Long.parseLong(item.result().id());
                    log.warn("MISSING 탐지: product_id={}", missingId);
                    reportHelper.insertMissing(missingId, runAt);
                }
            }
        } catch (Exception e) {
            long chunkStart = ids.get(0);
            long chunkEnd = ids.get(ids.size() - 1);
            log.error("mget 실패 — SKIPPED 기록: chunk_start={}, chunk_end={}", chunkStart, chunkEnd, e);
            reportHelper.insertSkipped(chunkStart, chunkEnd, runAt);
        }
    }
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
./gradlew :multicast-server:test --tests "*.ValidateItemWriterTest" 2>&1 | tail -20
```
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add multicast-server/src/main/java/project/matchalatte/domain/service/ValidateItemWriter.java \
        multicast-server/src/test/java/project/matchalatte/domain/service/ValidateItemWriterTest.java
git commit -m "[feat] : ValidateItemWriter — ES mget 기반 MISSING/SKIPPED 탐지"
```

---

## Task 3: GhostCheckTasklet — Step 2 GHOST 탐지 (PIT + search_after)

**Files:**
- Create: `multicast-server/src/main/java/project/matchalatte/domain/service/GhostCheckTasklet.java`
- Test: `multicast-server/src/test/java/project/matchalatte/domain/service/GhostCheckTaskletTest.java`

**Interfaces:**
- Consumes: `ValidateReportHelper` (Task 1)
- Produces: `GhostCheckTasklet implements Tasklet` — Spring Batch Step 2에서 사용

- [ ] **Step 1: 실패 테스트 작성**

`src/test/.../GhostCheckTaskletTest.java`:
```java
package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeResponse;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GhostCheckTaskletTest {

    @Mock ElasticsearchClient elasticsearchClient;
    @Mock NamedParameterJdbcTemplate namedJdbc;
    @Mock ValidateReportHelper reportHelper;
    @Mock StepContribution stepContribution;
    @Mock ChunkContext chunkContext;

    GhostCheckTasklet sut;

    @BeforeEach
    void setUp() {
        sut = new GhostCheckTasklet(elasticsearchClient, namedJdbc, reportHelper, "2026-06-24T10:00:00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void RDB에_없는_id는_insertGhost_호출() throws Exception {
        OpenPointInTimeResponse pitOpen = mock(OpenPointInTimeResponse.class);
        SearchResponse<Void> searchResponse = mock(SearchResponse.class);
        HitsMetadata<Void> hitsMetadata = mock(HitsMetadata.class);
        Hit<Void> hit = mock(Hit.class);
        ClosePointInTimeResponse closeResponse = mock(ClosePointInTimeResponse.class);

        when(elasticsearchClient.openPointInTime(any(Function.class))).thenReturn(pitOpen);
        when(pitOpen.id()).thenReturn("pit-1");
        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenReturn(searchResponse);
        when(searchResponse.pitId()).thenReturn("pit-2");
        when(searchResponse.hits()).thenReturn(hitsMetadata);
        when(hitsMetadata.hits()).thenReturn(List.of(hit));
        when(hit.id()).thenReturn("999");
        when(hit.sort()).thenReturn(List.of());
        when(namedJdbc.queryForList(anyString(), any(), eq(Long.class))).thenReturn(List.of());
        when(elasticsearchClient.closePointInTime(any(Function.class))).thenReturn(closeResponse);

        RepeatStatus status = sut.execute(stepContribution, chunkContext);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(reportHelper).insertGhost(999L, "2026-06-24T10:00:00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 예외_발생시_finally에서_PIT_close_호출() throws Exception {
        OpenPointInTimeResponse pitOpen = mock(OpenPointInTimeResponse.class);
        ClosePointInTimeResponse closeResponse = mock(ClosePointInTimeResponse.class);

        when(elasticsearchClient.openPointInTime(any(Function.class))).thenReturn(pitOpen);
        when(pitOpen.id()).thenReturn("pit-1");
        when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
            .thenThrow(new RuntimeException("ES error"));
        when(elasticsearchClient.closePointInTime(any(Function.class))).thenReturn(closeResponse);

        sut.execute(stepContribution, chunkContext);

        verify(elasticsearchClient).closePointInTime(any(Function.class));
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew :multicast-server:test --tests "*.GhostCheckTaskletTest" 2>&1 | tail -20
```
Expected: FAIL

- [ ] **Step 3: GhostCheckTasklet 구현**

`src/main/.../domain/service/GhostCheckTasklet.java`:
```java
package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Slf4j
public class GhostCheckTasklet implements Tasklet {

    private final ElasticsearchClient elasticsearchClient;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final ValidateReportHelper reportHelper;
    private final String runAt;

    public GhostCheckTasklet(ElasticsearchClient elasticsearchClient,
                              NamedParameterJdbcTemplate namedJdbc,
                              ValidateReportHelper reportHelper,
                              String runAt) {
        this.elasticsearchClient = elasticsearchClient;
        this.namedJdbc = namedJdbc;
        this.reportHelper = reportHelper;
        this.runAt = runAt;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        OpenPointInTimeResponse pitOpen;
        try {
            pitOpen = elasticsearchClient.openPointInTime(
                p -> p.index("products").keepAlive(t -> t.time("5m"))
            );
        } catch (Exception e) {
            log.error("PIT open 실패 — ghostCheckStep 건너뜀", e);
            return RepeatStatus.FINISHED;
        }

        String pitId = pitOpen.id();
        try {
            List<FieldValue> searchAfter = null;
            while (true) {
                final String currentPitId = pitId;
                final List<FieldValue> currentSearchAfter = searchAfter;

                SearchResponse<Void> response = elasticsearchClient.search(s -> {
                    var builder = s
                        .pit(p -> p.id(currentPitId).keepAlive(t -> t.time("5m")))
                        .sort(so -> so.field(f -> f.field("_id").order(SortOrder.Asc)))
                        .size(1000)
                        .source(src -> src.fetch(false));
                    if (currentSearchAfter != null) {
                        builder.searchAfter(currentSearchAfter);
                    }
                    return builder;
                }, Void.class);

                pitId = response.pitId(); // 매 응답마다 갱신된 pit_id 이어받기

                List<Hit<Void>> hits = response.hits().hits();
                if (hits.isEmpty()) break;

                List<Long> esIds = hits.stream()
                    .map(h -> Long.parseLong(h.id()))
                    .toList();

                List<Long> existingInRdb = namedJdbc.queryForList(
                    "SELECT id FROM product WHERE id IN (:ids)",
                    new MapSqlParameterSource("ids", esIds),
                    Long.class
                );
                Set<Long> existingSet = new HashSet<>(existingInRdb);

                for (Long esId : esIds) {
                    if (!existingSet.contains(esId)) {
                        log.warn("GHOST 탐지: product_id={}", esId);
                        reportHelper.insertGhost(esId, runAt);
                    }
                }

                searchAfter = hits.get(hits.size() - 1).sort();
                if (hits.size() < 1000) break;
            }
        } catch (Exception e) {
            log.error("ghostCheckStep 오류", e);
        } finally {
            final String finalPitId = pitId;
            try {
                elasticsearchClient.closePointInTime(p -> p.id(finalPitId));
                log.info("PIT close 완료");
            } catch (Exception e) {
                log.error("PIT close 실패: pit_id={}", finalPitId, e);
            }
        }
        return RepeatStatus.FINISHED;
    }
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
./gradlew :multicast-server:test --tests "*.GhostCheckTaskletTest" 2>&1 | tail -20
```
Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add multicast-server/src/main/java/project/matchalatte/domain/service/GhostCheckTasklet.java \
        multicast-server/src/test/java/project/matchalatte/domain/service/GhostCheckTaskletTest.java
git commit -m "[feat] : GhostCheckTasklet — PIT + search_after 기반 GHOST 탐지"
```

---

## Task 4: ValidateJobListener + ValidateBatchHelper

**Files:**
- Create: `multicast-server/src/main/java/project/matchalatte/domain/service/ValidateJobListener.java`
- Create: `multicast-server/src/main/java/project/matchalatte/domain/service/ValidateBatchHelper.java`
- Test: `multicast-server/src/test/java/project/matchalatte/domain/service/ValidateJobListenerTest.java`

**Interfaces:**
- Consumes: `ValidateReportHelper` (Task 1), `SyncLockHelper` (기존)
- Produces:
  - `ValidateJobListener implements JobExecutionListener`
  - `ValidateBatchHelper.validateProduct(): String` — SyncController에서 사용

- [ ] **Step 1: 실패 테스트 작성**

`src/test/.../ValidateJobListenerTest.java`:
```java
package project.matchalatte.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateJobListenerTest {

    @Mock ValidateReportHelper reportHelper;
    @Mock SyncLockHelper syncLockHelper;

    @InjectMocks
    ValidateJobListener sut;

    @Test
    void afterJob_완료시_Lock_해제_호출() {
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(reportHelper.countByIssueType("MISSING")).thenReturn(3);
        when(reportHelper.countByIssueType("GHOST")).thenReturn(1);
        when(reportHelper.countByIssueType("SKIPPED")).thenReturn(0);

        sut.afterJob(jobExecution);

        verify(syncLockHelper).stopFullSync();
    }

    @Test
    void afterJob_실패시에도_Lock_해제_호출() {
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(reportHelper.countByIssueType(anyString())).thenReturn(0);

        sut.afterJob(jobExecution);

        verify(syncLockHelper).stopFullSync();
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew :multicast-server:test --tests "*.ValidateJobListenerTest" 2>&1 | tail -20
```
Expected: FAIL

- [ ] **Step 3: ValidateJobListener 구현**

`src/main/.../domain/service/ValidateJobListener.java`:
```java
package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ValidateJobListener implements JobExecutionListener {

    private final ValidateReportHelper reportHelper;
    private final SyncLockHelper syncLockHelper;

    public ValidateJobListener(ValidateReportHelper reportHelper, SyncLockHelper syncLockHelper) {
        this.reportHelper = reportHelper;
        this.syncLockHelper = syncLockHelper;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        int missing = reportHelper.countByIssueType("MISSING");
        int ghost = reportHelper.countByIssueType("GHOST");
        int skipped = reportHelper.countByIssueType("SKIPPED");

        log.info("[검증 완료] 상태={} | MISSING={} | GHOST={} | SKIPPED={}",
            jobExecution.getStatus(), missing, ghost, skipped);

        if (skipped > 0) {
            log.warn("[주의] SKIPPED 청크 {}개 존재 — 해당 범위는 미검증 상태입니다. validate_report 확인 필요.", skipped);
        }

        syncLockHelper.stopFullSync();
    }
}
```

- [ ] **Step 4: ValidateBatchHelper 구현**

`src/main/.../domain/service/ValidateBatchHelper.java`:
```java
package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
@Slf4j
public class ValidateBatchHelper {

    private final JobLauncher jobLauncher;
    private final Job validateSyncJob;
    private final SyncLockHelper syncLockHelper;

    public ValidateBatchHelper(JobLauncher jobLauncher,
                               @Qualifier("validateSyncJob") Job validateSyncJob,
                               SyncLockHelper syncLockHelper) {
        this.jobLauncher = jobLauncher;
        this.validateSyncJob = validateSyncJob;
        this.syncLockHelper = syncLockHelper;
    }

    public String validateProduct() {
        if (!syncLockHelper.startFullSync()) {
            return "Validate Job 실행 요청 실패 — 이미 동기화 작업 진행 중.";
        }
        try {
            String runAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            JobParameters params = new JobParametersBuilder()
                .addString("run_at", runAt)
                .addDate("run.date", new Date())
                .toJobParameters();

            var jobExecution = jobLauncher.run(validateSyncJob, params);
            return "Validate Job (ID: " + jobExecution.getId() + ") 실행 요청 완료. run_at=" + runAt;
        } catch (Exception e) {
            syncLockHelper.stopFullSync();
            log.error("Validate Job 실행 요청 중 오류", e);
            return "Validate Job 실행 오류: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
./gradlew :multicast-server:test --tests "*.ValidateJobListenerTest" 2>&1 | tail -20
```
Expected: PASS (2 tests)

- [ ] **Step 6: 커밋**

```bash
git add multicast-server/src/main/java/project/matchalatte/domain/service/ValidateJobListener.java \
        multicast-server/src/main/java/project/matchalatte/domain/service/ValidateBatchHelper.java \
        multicast-server/src/test/java/project/matchalatte/domain/service/ValidateJobListenerTest.java
git commit -m "[feat] : ValidateJobListener afterJob 요약 + ValidateBatchHelper Lock 획득"
```

---

## Task 5: validateSyncJob 빈 등록 (BatchConfig 수정)

**Files:**
- Modify: `multicast-server/src/main/java/project/matchalatte/api/config/BatchConfig.java`

**Interfaces:**
- Consumes: `ValidateItemWriter` (Task 2), `GhostCheckTasklet` (Task 3), `ValidateJobListener` (Task 4)
- Produces: `@Bean("validateSyncJob") Job` — ValidateBatchHelper에서 사용

`run_at`은 `ValidateBatchHelper`가 JobParameter로 넘기므로, `ValidateItemWriter`와 `GhostCheckTasklet` 빈을 `@StepScope`로 등록하고 `@Value("#{jobParameters['run_at']}")`로 주입받는다.

- [ ] **Step 1: BatchConfig에 빈 추가**

`BatchConfig.java`의 기존 코드 아래에 다음을 추가한다:

```java
// ─── validateSyncJob ────────────────────────────────────────────────

@Bean
@StepScope
public ValidateItemWriter validateItemWriter(
        ValidateReportHelper reportHelper,
        @Value("#{jobParameters['run_at']}") String runAt) {
    return new ValidateItemWriter(elasticsearchClient, reportHelper, runAt);
}

@Bean
@StepScope
public GhostCheckTasklet ghostCheckTasklet(
        NamedParameterJdbcTemplate namedJdbc,
        ValidateReportHelper reportHelper,
        @Value("#{jobParameters['run_at']}") String runAt) {
    return new GhostCheckTasklet(elasticsearchClient, namedJdbc, reportHelper, runAt);
}

@Bean
public ItemReader<Long> validateProductIdReader() throws Exception {
    JdbcPagingItemReader<Long> reader = new JdbcPagingItemReader<>();
    reader.setDataSource(dataSource);
    reader.setPageSize(1000);
    reader.setRowMapper((rs, rowNum) -> rs.getLong("id"));

    SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
    factory.setDataSource(dataSource);
    factory.setSelectClause("SELECT id");
    factory.setFromClause("FROM product");
    factory.setSortKey("id");
    reader.setQueryProvider(Objects.requireNonNull(factory.getObject()));
    reader.afterPropertiesSet();
    return reader;
}

@Bean
public Step rdbToEsCheckStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              ValidateItemWriter validateItemWriter) throws Exception {
    return new StepBuilder("rdbToEsCheckStep", jobRepository)
        .<Long, Long>chunk(1000, transactionManager)
        .reader(validateProductIdReader())
        .writer(validateItemWriter)
        .build();
}

@Bean
public Step ghostCheckStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            GhostCheckTasklet ghostCheckTasklet) {
    return new StepBuilder("ghostCheckStep", jobRepository)
        .tasklet(ghostCheckTasklet, transactionManager)
        .build();
}

@Bean("validateSyncJob")
public Job validateSyncJob(JobRepository jobRepository,
                            Step rdbToEsCheckStep,
                            Step ghostCheckStep,
                            ValidateJobListener validateJobListener) {
    return new JobBuilder("validateSyncJob", jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(validateJobListener)
        .start(rdbToEsCheckStep)
        .next(ghostCheckStep)
        .build();
}
```

`BatchConfig`의 필드에 `NamedParameterJdbcTemplate` 주입을 추가한다:
```java
private final NamedParameterJdbcTemplate namedJdbc;

public BatchConfig(DataSource dataSource,
                   ElasticsearchClient elasticsearchClient,
                   NamedParameterJdbcTemplate namedJdbc) {
    this.dataSource = dataSource;
    this.elasticsearchClient = elasticsearchClient;
    this.namedJdbc = namedJdbc;
}
```

필요한 import 추가:
```java
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import project.matchalatte.domain.service.ValidateItemWriter;
import project.matchalatte.domain.service.GhostCheckTasklet;
import project.matchalatte.domain.service.ValidateJobListener;
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew :multicast-server:compileJava 2>&1 | tail -20
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add multicast-server/src/main/java/project/matchalatte/api/config/BatchConfig.java
git commit -m "[feat] : BatchConfig — validateSyncJob (rdbToEsCheckStep + ghostCheckStep) 빈 등록"
```

---

## Task 6: RepairService + SyncController 엔드포인트 2개

**Files:**
- Create: `multicast-server/src/main/java/project/matchalatte/domain/service/RepairService.java`
- Modify: `multicast-server/src/main/java/project/matchalatte/api/controller/SyncController.java`
- Test: `multicast-server/src/test/java/project/matchalatte/domain/service/RepairServiceTest.java`

**Interfaces:**
- Consumes: `ValidateReportHelper.findMissingProductIds()` (Task 1), `ElasticsearchClient`, `JdbcTemplate`
- Produces:
  - `RepairService.repair(): String`
  - `POST /api/internal/sync/validate` → `ValidateBatchHelper.validateProduct()`
  - `POST /api/internal/sync/repair` → `RepairService.repair()`

- [ ] **Step 1: 실패 테스트 작성**

`src/test/.../RepairServiceTest.java`:
```java
package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairServiceTest {

    @Mock ValidateReportHelper reportHelper;
    @Mock ElasticsearchClient elasticsearchClient;
    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks
    RepairService sut;

    @Test
    void MISSING_없으면_색인_안_함() {
        when(reportHelper.findMissingProductIds()).thenReturn(List.of());

        String result = sut.repair();

        assertThat(result).contains("MISSING 항목 없음");
        verifyNoInteractions(elasticsearchClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void MISSING_id로_RDB_조회_후_Bulk_색인() throws Exception {
        when(reportHelper.findMissingProductIds()).thenReturn(List.of(1L, 2L));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
            .thenReturn(List.of(
                new SyncProductInfo(1L, "상품A", "설명A", 10000L, 100L),
                new SyncProductInfo(2L, "상품B", "설명B", 20000L, 200L)
            ));
        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(false);
        when(elasticsearchClient.bulk(any(Function.class))).thenReturn(bulkResponse);

        String result = sut.repair();

        assertThat(result).contains("2");
        verify(elasticsearchClient).bulk(any(Function.class));
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew :multicast-server:test --tests "*.RepairServiceTest" 2>&1 | tail -20
```
Expected: FAIL

- [ ] **Step 3: RepairService 구현**

`src/main/.../domain/service/RepairService.java`:
```java
package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import project.matchalatte.domain.entity.ProductDocument;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RepairService {

    private final ValidateReportHelper reportHelper;
    private final ElasticsearchClient elasticsearchClient;
    private final JdbcTemplate jdbcTemplate;

    public RepairService(ValidateReportHelper reportHelper,
                         ElasticsearchClient elasticsearchClient,
                         JdbcTemplate jdbcTemplate) {
        this.reportHelper = reportHelper;
        this.elasticsearchClient = elasticsearchClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public String repair() {
        List<Long> missingIds = reportHelper.findMissingProductIds();
        if (missingIds.isEmpty()) {
            log.info("MISSING 항목 없음 — 재색인 불필요");
            return "MISSING 항목 없음";
        }

        String placeholders = String.join(",", Collections.nCopies(missingIds.size(), "?"));
        String sql = "SELECT id, name, description, price, user_id FROM product WHERE id IN (" + placeholders + ")";
        List<SyncProductInfo> products = jdbcTemplate.query(
            sql,
            (rs, row) -> new SyncProductInfo(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getLong("price"),
                rs.getLong("user_id")
            ),
            missingIds.toArray()
        );

        if (products.isEmpty()) {
            log.warn("MISSING id가 RDB에도 없음 — 이미 삭제된 데이터일 수 있음");
            return "RDB에서 대상 상품 조회 결과 없음";
        }

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (SyncProductInfo p : products) {
                ProductDocument doc = ProductDocument.from(p);
                br.operations(op -> op.index(idx ->
                    idx.index("products").id(doc.getId().toString()).document(doc)
                ));
            }
            BulkResponse result = elasticsearchClient.bulk(br.build());

            if (result.errors()) {
                result.items().forEach(item -> {
                    if (item.error() != null) {
                        log.error("재색인 실패 ID={}: {}", item.id(), item.error().reason());
                    }
                });
                return "재색인 일부 실패 — 로그 확인 필요";
            }

            log.info("재색인 완료: {}건, {}ms", products.size(), result.took());
            return "재색인 완료: " + products.size() + "건";
        } catch (Exception e) {
            log.error("재색인 중 오류", e);
            return "재색인 오류: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 4: SyncController에 엔드포인트 2개 추가**

`SyncController.java`의 기존 엔드포인트 아래에 추가:

```java
private final ValidateBatchHelper validateBatchHelper;
private final RepairService repairService;

// 생성자에 두 파라미터 추가
public SyncController(SyncService syncService,
                       ValidateBatchHelper validateBatchHelper,
                       RepairService repairService) {
    this.syncService = syncService;
    this.validateBatchHelper = validateBatchHelper;
    this.repairService = repairService;
}

@PostMapping("/api/internal/sync/validate")
public String validateSync() {
    return validateBatchHelper.validateProduct();
}

@PostMapping("/api/internal/sync/repair")
public String repairSync() {
    return repairService.repair();
}
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
./gradlew :multicast-server:test --tests "*.RepairServiceTest" 2>&1 | tail -20
```
Expected: PASS (2 tests)

- [ ] **Step 6: 전체 테스트 통과 확인**

```bash
./gradlew :multicast-server:test 2>&1 | tail -30
```
Expected: BUILD SUCCESSFUL, 모든 테스트 PASS

- [ ] **Step 7: 수동 검증**

multicast-server 실행 후:

```bash
# 1. 검증 Job 실행
curl -X POST http://localhost:8082/api/internal/sync/validate
# Expected: "Validate Job (ID: N) 실행 요청 완료. run_at=..."

# 2. validate_report 확인
# MySQL: SELECT issue_type, COUNT(*) FROM validate_report GROUP BY issue_type;

# 3. 누락 확인 후 재색인
curl -X POST http://localhost:8082/api/internal/sync/repair
# Expected: "재색인 완료: N건" 또는 "MISSING 항목 없음"
```

- [ ] **Step 8: 커밋**

```bash
git add multicast-server/src/main/java/project/matchalatte/domain/service/RepairService.java \
        multicast-server/src/main/java/project/matchalatte/api/controller/SyncController.java \
        multicast-server/src/test/java/project/matchalatte/domain/service/RepairServiceTest.java
git commit -m "[feat] : RepairService Bulk 재색인 + /validate, /repair 엔드포인트"
```
