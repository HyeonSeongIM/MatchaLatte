

---

# ☕ MatchaLatte

---

## 🎯 모듈 분리 원칙

* 각 모듈의 **역할(Role)** 과 **책임(Responsibility)** 을 명확하게 정의
* 유지보수성·확장성을 고려한 구조적 분리

---

## 🧪 Test Rules

1. JPA/ORM 기반 도메인 로직은 **필수로 통합 테스트 진행**
2. 테스트 플로우: **Unit → Integration → Human Test**

---

# 🛒 Commerce Platform Backend

### 2025.08 ~ 진행중 (단독 개발)

상품 검색 성능, 운영 효율성 개선을 목표로 하는
**커머스 플랫폼 REST API 서버 백엔드 개발 담당**

---

## ⚙️ Tech Stack

**Java 17 · Spring Framework 3 · JPA · QueryDSL · MySQL · ElasticSearch · ELK · JUnit5 · AWS(EC2/RDS) · Spring Batch · Gradle**

---

# 🌟 Features

### 🔎 1. 상품 검색 서비스

* 키워드 기반 고성능 검색 제공
* ElasticSearch + **nori 분석기** 적용
* 한글 형태소 기반의 정확도 높은 검색 결과

### 📦 2. 상품 CRUD 서비스

* 사용자 단위 상품 등록/수정/삭제
* QueryDSL 기반 조건 검색/정렬/필터링 지원

---

# 🚀 Technical Challenges & Solutions

## ⚡ 1. 페이징 성능 최적화 (Count Query 개선)

* Pageable 사용 시 count 쿼리로 발생하던 병목 해결
* count 결과를 **캐싱 처리**하여 DB I/O 감소
* **응답속도 3.5s → 500ms로 단축 (데이터 10만 건 기준)**
* 앱·웹 UX에 따른 페이징 전략 비교 및 블로그 정리

---

## 🔍 2. LIKE 검색 한계 극복 (ES 도입)

* RDB LIKE 검색의 성능/정확도 문제 해결
* ElasticSearch 적용으로 검색 속도·품질 향상
* **nori 분석기** 활용한 한글 토큰화로 검색 품질 강화
* ES ↔ RDB **데이터 동기화 및 정합성 보장 로직 구성 중**

---

## 🛠️ 3. 장애 분석 및 모니터링 환경 개선

* 모든 API 요청에 **Trace ID 자동 부여**
* ELK 스택(Elasticsearch, Logstash, Kibana) 구축
* 장애 발생 시 **유저 단위 영향 분석** 가능
* 로그 기반 장애 추적 및 실시간 모니터링 강화

---

## 🚧 4. 테스트 품질 & 빌드 속도 개선

* **Gradle 멀티 모듈 구조 도입**으로 관심사 분리
* Gradle Task 커스텀:

  * 유닛 테스트 / 통합 테스트 실행 환경 **분리**
* 불필요한 Spring Context 로딩 제거
* **전체 빌드 + 테스트 시간 12s → 7.5s 단축**

---

## 🔐 5. 보안 아키텍처 — VPC 네트워크 설계

* Public / Private Subnet 구조로 외부 침입 경로 차단
* DB는 **AWS RDS**에 배치하여 신뢰성 확보
* 불필요한 포트/리소스 외부 노출 제거
* 전체 시스템 **안정성·보안성 강화**

