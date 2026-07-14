# Elasticsearch 인덱스 샤드/레플리카 + 매핑 설계

**날짜:** 2026-06-23
**범위:** multicast-server 모듈
**목표:** `createNewIndex()` 호출 시 샤드/레플리카 설정과 필드 매핑을 명시적으로 적용

---

## 배경

현재 `SyncAliasManager.createNewIndex()`는 설정 없이 인덱스를 생성해 ES 기본값으로 동작합니다. 100만 건 규모의 프로덕션 환경에 맞는 설정값과 nori 기반 한국어 매핑을 명시적으로 관리합니다.

**인덱스 네이밍 전략:** `products_yyyy_MM_dd` — 날마다 새 인덱스를 생성해 일별 스냅샷과 백업을 유지합니다. 앨리어스 `products` 스왑으로 제로다운타임 전환합니다. 이 구조는 기존 설계이며 변경하지 않습니다.

---

## 샤드/레플리카 설정

| 설정 | 값 | 근거 |
|---|---|---|
| `number_of_shards` | `1` | 1M 상품 × ~500bytes ≈ 500MB. ES 권장 샤드 크기(10~50GB) 기준으로 1개 충분. 싱글 노드에서 샤드 여러 개는 병렬 검색 이점 없음 |
| `number_of_replicas` | `0` | 노드 1대 — 레플리카 배정 불가 |

**확장 시 대응:** 노드 추가 후 새 인덱스(`products_yyyy_MM_dd`)를 원하는 샤드 수로 생성 → 앨리어스 스왑. 기존 JSON 파일의 값만 변경하면 됩니다.

---

## 분석기 설정 (nori)

```json
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
```

`decompound_mode: "mixed"` — 복합어를 원형과 분해형 모두 색인합니다.
예: "말차라떼" → `말차라떼`, `말차`, `라떼` 모두 검색 가능.

---

## 필드 매핑

| 필드 | 타입 | 분석기 | 용도 |
|---|---|---|---|
| `id` | `long` | — | 식별자 |
| `name` | `text` (+ `keyword` sub-field) | nori_analyzer | 검색(`text`) + 정렬/집계(`keyword`) |
| `price` | `long` | — | 범위 필터 |
| `userId` | `long` | — | 필터 전용 |

**`description` 제외:** DB에만 존재하며 ES 색인 시 재직렬화로 제외합니다.
`"dynamic": "strict"` 설정으로 매핑에 없는 필드(description 포함)가 실수로 색인되면 에러로 명시적 차단합니다.

---

## 아키텍처

### 생성할 파일

```
multicast-server/src/main/resources/product-index-settings.json
```

전체 인덱스 설정(settings + mappings)을 담은 JSON 파일입니다.
Kibana Dev Tools에 그대로 붙여 테스트할 수 있는 ES 표준 포맷입니다.

### 수정할 파일

```
multicast-server/src/main/java/project/matchalatte/domain/service/SyncAliasManager.java
  → createNewIndex(): ClassPathResource로 JSON 파일 로드 후 settings + mappings 포함해 인덱스 생성
```

### product-index-settings.json 전체 구조

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
      "name":   { "type": "text", "analyzer": "nori_analyzer",
                  "fields": { "keyword": { "type": "keyword" } } },
      "price":  { "type": "long" },
      "userId": { "type": "long" }
    }
  }
}
```

---

## 테스트 전략

- `SyncAliasManager.createNewIndex()`에 `@MockBean ElasticsearchClient`를 주입해 JSON 파일이 정상 로드되고 create 요청이 settings/mappings를 포함해 호출되는지 검증
- nori 플러그인이 설치된 ES 환경에서만 실제 색인 가능하므로 단위 테스트는 JSON 로딩과 메서드 호출 단계까지만 검증
