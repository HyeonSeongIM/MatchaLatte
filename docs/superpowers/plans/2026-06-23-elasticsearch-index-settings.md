# ES 인덱스 샤드/레플리카 + nori 매핑 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `SyncAliasManager.createNewIndex()`가 인덱스 생성 시 샤드 1개, 레플리카 0개, nori 분석기 기반 필드 매핑을 JSON 파일에서 로드해 적용한다.

**Architecture:** `product-index-settings.json`을 classpath에 두고 `ClassPathResource`로 읽어 ES Java Client의 `withJson(InputStream)` API로 인덱스 생성 요청에 주입한다. `description` 필드는 ES에 색인하지 않으므로 매핑에 포함하지 않으며 `dynamic: "strict"`로 실수를 차단한다.

**Tech Stack:** Elasticsearch Java Client 8.x (`co.elastic.clients`), Spring `ClassPathResource`, Jackson (JSON 검증 테스트), JUnit 5, Mockito

## Global Constraints

- 수정 대상 모듈: `multicast-server`
- `number_of_shards`: 1, `number_of_replicas`: 0
- `dynamic`: `"strict"` — 매핑 외 필드 색인 차단
- 분석기 이름: `nori_analyzer` (settings와 mappings에서 동일 이름 사용)
- tokenizer: `nori_tokenizer`, `decompound_mode`: `"mixed"`
- ES 색인 필드: `id`(long), `name`(text+keyword), `price`(long), `userId`(long) — `description` 제외
- Gradle 실행 기준 디렉토리: 프로젝트 루트 `/Users/user/IdeaProjects/MatchaLatte`
- 커밋 메시지 형식: `[feat] : <description>`

---

## 파일 맵

| 작업 | 파일 |
|---|---|
| 생성 | `multicast-server/src/main/resources/product-index-settings.json` |
| 수정 | `multicast-server/src/main/java/project/matchalatte/domain/service/SyncAliasManager.java` |
| 생성(테스트) | `multicast-server/src/test/java/project/matchalatte/domain/service/SyncAliasManagerTest.java` |

---

### Task 1: product-index-settings.json 생성 + 구조 검증 테스트

**Files:**
- Create: `multicast-server/src/main/resources/product-index-settings.json`
- Test: `multicast-server/src/test/java/project/matchalatte/domain/service/SyncAliasManagerTest.java`

**Interfaces:**
- Produces: classpath 경로 `product-index-settings.json` — Task 2의 `ClassPathResource("product-index-settings.json")`에서 로드

- [ ] **Step 1: 테스트 디렉토리 생성 + 실패하는 테스트 작성**

디렉토리 생성:
```bash
mkdir -p multicast-server/src/test/java/project/matchalatte/domain/service
```

`multicast-server/src/test/java/project/matchalatte/domain/service/SyncAliasManagerTest.java`:

```java
package project.matchalatte.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class SyncAliasManagerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void productIndexSettings_샤드_레플리카_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/settings/number_of_shards").asInt()).isEqualTo(1);
        assertThat(root.at("/settings/number_of_replicas").asInt()).isEqualTo(0);
    }

    @Test
    void productIndexSettings_dynamic_strict_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/mappings/dynamic").asText()).isEqualTo("strict");
    }

    @Test
    void productIndexSettings_name_필드_nori_분석기_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/mappings/properties/name/type").asText()).isEqualTo("text");
        assertThat(root.at("/mappings/properties/name/analyzer").asText()).isEqualTo("nori_analyzer");
        assertThat(root.at("/mappings/properties/name/fields/keyword/type").asText()).isEqualTo("keyword");
    }

    @Test
    void productIndexSettings_nori_decompound_mode_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/settings/analysis/tokenizer/nori_tokenizer/type").asText())
                .isEqualTo("nori_tokenizer");
        assertThat(root.at("/settings/analysis/tokenizer/nori_tokenizer/decompound_mode").asText())
                .isEqualTo("mixed");
    }

    @Test
    void productIndexSettings_description_필드_없음_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/mappings/properties/description").isMissingNode()).isTrue();
    }

}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :multicast-server:test --tests "project.matchalatte.domain.service.SyncAliasManagerTest"
```

Expected: FAIL — `product-index-settings.json` 파일 없음 (`FileNotFoundException`)

- [ ] **Step 3: product-index-settings.json 생성**

`multicast-server/src/main/resources/product-index-settings.json`:

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "nori_analyzer": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase"]
        }
      },
      "tokenizer": {
        "nori_tokenizer": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed"
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "id":     { "type": "long" },
      "name":   {
        "type": "text",
        "analyzer": "nori_analyzer",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "price":  { "type": "long" },
      "userId": { "type": "long" }
    }
  }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :multicast-server:test --tests "project.matchalatte.domain.service.SyncAliasManagerTest"
```

Expected: PASS — 5개 테스트 모두 통과

- [ ] **Step 5: 커밋**

```bash
git add multicast-server/src/main/resources/product-index-settings.json \
        multicast-server/src/test/java/project/matchalatte/domain/service/SyncAliasManagerTest.java
git commit -m "[feat] : ES 인덱스 샤드/레플리카 + nori 매핑 JSON 설정 파일 추가"
```

---

### Task 2: SyncAliasManager.createNewIndex() JSON 로드로 수정

**Files:**
- Modify: `multicast-server/src/main/java/project/matchalatte/domain/service/SyncAliasManager.java`
- Test: `multicast-server/src/test/java/project/matchalatte/domain/service/SyncAliasManagerTest.java` (테스트 추가)

**Interfaces:**
- Consumes: `product-index-settings.json` (Task 1), `ElasticsearchClient` (기존)
- `createNewIndex(String newIndexName) throws IOException` — 시그니처 변경 없음

- [ ] **Step 1: 실패하는 동작 테스트 추가**

`SyncAliasManagerTest.java`에 아래 3개 테스트를 추가한다. 현재 구현(`create(c -> c.index(name))`)은 JSON 로딩 없이 동작하므로 mock 체인을 이용한 검증 시 `elasticsearchClient.indices()`가 호출되나 아직 테스트를 통해 확인되지 않은 상태다:

```java
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
```

클래스 선언 위에 `@ExtendWith(MockitoExtension.class)` 추가 후 필드 추가:

```java
@ExtendWith(MockitoExtension.class)
class SyncAliasManagerTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private SyncAliasManager syncAliasManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // ... 기존 JSON 테스트들 유지 ...

    @Test
    @SuppressWarnings("unchecked")
    void createNewIndex_acknowledged_true_정상_종료() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
        CreateIndexResponse mockResponse = mock(CreateIndexResponse.class);

        when(elasticsearchClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.create(any())).thenReturn(mockResponse);
        when(mockResponse.acknowledged()).thenReturn(true);

        assertThatCode(() -> syncAliasManager.createNewIndex("products_2026_06_23"))
                .doesNotThrowAnyException();

        verify(mockIndicesClient).create(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createNewIndex_acknowledged_false_RuntimeException_발생() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
        CreateIndexResponse mockResponse = mock(CreateIndexResponse.class);

        when(elasticsearchClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.create(any())).thenReturn(mockResponse);
        when(mockResponse.acknowledged()).thenReturn(false);

        assertThatThrownBy(() -> syncAliasManager.createNewIndex("products_2026_06_23"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Index creation failed.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createNewIndex_인덱스_이미_존재시_예외_무시() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);

        when(elasticsearchClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.create(any()))
                .thenThrow(new RuntimeException("resource_already_exists_exception"));

        assertThatCode(() -> syncAliasManager.createNewIndex("products_2026_06_23"))
                .doesNotThrowAnyException();
    }

}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :multicast-server:test --tests "project.matchalatte.domain.service.SyncAliasManagerTest"
```

Expected: `createNewIndex_*` 테스트 3개 FAIL — 현재 `createNewIndex()`가 `ClassPathResource`를 로드하지 않으므로 mock 체인과 연결되지 않음

- [ ] **Step 3: createNewIndex() 수정**

`SyncAliasManager.java`에 import 추가:

```java
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
```

`createNewIndex()` 메서드 전체를 아래로 교체:

```java
public void createNewIndex(String newIndexName) throws IOException {
    log.info("새로운 인덱스 [{}] 생성을 시도합니다.", newIndexName);

    try (InputStream is = new ClassPathResource("product-index-settings.json").getInputStream()) {
        CreateIndexResponse response = elasticsearchClient.indices()
            .create(c -> c.withJson(is).index(newIndexName));

        if (response.acknowledged()) {
            log.info("인덱스 [{}] 생성 성공.", newIndexName);
        }
        else {
            log.error("인덱스 [{}] 생성 실패: acknowledged=false.", newIndexName);
            throw new RuntimeException("Index creation failed.");
        }
    }
    catch (Exception e) {
        log.error("인덱스 [{}] 생성 중 예외 발생.", newIndexName, e);
        if (e.getMessage() != null && e.getMessage().contains("resource_already_exists_exception")) {
            log.warn("인덱스 [{}]는 이미 존재합니다. 계속 진행합니다.", newIndexName);
            return;
        }
        throw e;
    }
}
```

> **핵심:** `.withJson(is)` → settings/mappings를 JSON에서 주입, `.index(newIndexName)` → URL 경로 파라미터로 인덱스명 설정. `withJson()` 이후에 `.index()`를 호출해 덮어쓰기를 방지한다.

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :multicast-server:test --tests "project.matchalatte.domain.service.SyncAliasManagerTest"
```

Expected: PASS — 8개 테스트 모두 통과 (JSON 구조 5개 + 동작 3개)

- [ ] **Step 5: 커밋**

```bash
git add multicast-server/src/main/java/project/matchalatte/domain/service/SyncAliasManager.java \
        multicast-server/src/test/java/project/matchalatte/domain/service/SyncAliasManagerTest.java
git commit -m "[feat] : createNewIndex() JSON 설정 파일 로드로 샤드/레플리카/nori 매핑 적용"
```
