package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Component
@Slf4j
public class SyncAliasManager {

    private final ElasticsearchClient elasticsearchClient;

    public SyncAliasManager(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 별칭을 새 인덱스로 원자적으로 전환하고 이전 인덱스를 백업으로 남깁니다.
     * @param aliasName 애플리케이션이 사용하는 별칭 이름 (예: products)
     * @param newIndexName 새로 데이터가 색인된 인덱스 이름
     */
    public void swapAlias(String aliasName, String newIndexName) throws IOException {

        // 1. 현재 별칭이 가리키는 이전 인덱스 찾기
        String oldIndexName = getCurrentIndexForAlias(aliasName).orElse(null);

        UpdateAliasesRequest.Builder updateAliasesBuilder = new UpdateAliasesRequest.Builder();

        // 2. 이전 인덱스가 있다면, 해당 인덱스에서 별칭을 제거합니다. (인덱스는 삭제하지 않고 백업으로 남김)
        if (oldIndexName != null) {
            updateAliasesBuilder.actions(a -> a.remove(r -> r.index(oldIndexName).alias(aliasName)));
            log.info("이전 인덱스 [{}]에서 별칭 [{}] 제거.", oldIndexName, aliasName);
        }

        // 3. 새 인덱스에 별칭을 추가합니다.
        updateAliasesBuilder.actions(a -> a.add(ad -> ad.index(newIndexName).alias(aliasName).isWriteIndex(true)));

        // 4. 원자적(Atomic) 요청 전송
        UpdateAliasesResponse response = elasticsearchClient.indices().updateAliases(updateAliasesBuilder.build());

        if (!response.acknowledged()) {
            log.error("Alias swap was not acknowledged for alias: {}", aliasName);
            throw new RuntimeException("Elasticsearch Alias Swap Failed.");
        }
        log.info("Alias Swap 완료: [{}] -> [{}]", oldIndexName, newIndexName);
    }

    // 현재 별칭이 가리키는 인덱스 이름을 찾는 헬퍼 메서드
    private Optional<String> getCurrentIndexForAlias(String aliasName) throws IOException {
        try {
            return elasticsearchClient.indices()
                .getAlias(a -> a.name(aliasName))
                .result() // Map<String(Index Name), IndexState>
                .keySet() // Set<String> (Index Names)
                .stream()
                .findFirst();
        }
        catch (Exception e) {
            // 별칭이 존재하지 않는 첫 실행 상황에서는 예외 발생 가능
            log.warn("Alias [{}]를 찾지 못함. 첫 실행으로 간주.", aliasName);
            return Optional.empty();
        }
    }

    // product-index-settings.json 의 number_of_replicas 와 같은 값 (재색인 후 복구 목표)
    private static final String SERVING_REPLICAS = "2";

    // replica 0: primary 에만 색인해 replica 별 재색인 비용 제거. refresh -1: 색인 중 세그먼트 생성 억제
    public void applyBatchSettings(String indexName) throws IOException {
        elasticsearchClient.indices().putSettings(s ->
            s.index(indexName)
             .settings(is -> is.numberOfReplicas("0").refreshInterval(t -> t.time("-1")))
        );
        log.info("인덱스 [{}] batch 설정 적용: number_of_replicas=0, refresh_interval=-1", indexName);
    }

    // forcemerge(1 segment) → replica 복구(세그먼트 파일 복사) → green 대기 → refresh 복구
    // socket-timeout(3s) 보다 오래 걸리는 작업이라 비동기 task + 폴링으로 기다린다
    public void restoreBatchSettings(String indexName) throws IOException {
        long t0 = System.currentTimeMillis();
        // 고수준 client(8.13)는 wait_for_completion=false 응답({"task":..})을 ForcemergeResponse 로 디코딩하다 실패해 low-level 로 호출
        RestClient restClient = ((RestClientTransport) elasticsearchClient._transport()).restClient();
        Response res = restClient.performRequest(
                new Request("POST", "/" + indexName + "/_forcemerge?max_num_segments=1&wait_for_completion=false"));
        String taskId = new ObjectMapper().readTree(res.getEntity().getContent()).get("task").asText();
        while (!elasticsearchClient.tasks().get(t -> t.taskId(taskId)).completed()) {
            sleep();
        }
        long t1 = System.currentTimeMillis();

        elasticsearchClient.indices().putSettings(s ->
            s.index(indexName).settings(is -> is.numberOfReplicas(SERVING_REPLICAS)));
        // wait_for_status 는 타임아웃 시 408 → 예외라서, 상태를 폴링한다
        while (elasticsearchClient.cluster().health(h -> h.index(indexName)).status() != HealthStatus.Green) {
            sleep();
        }
        long t2 = System.currentTimeMillis();

        elasticsearchClient.indices().putSettings(s ->
            s.index(indexName).settings(is -> is.refreshInterval(t -> t.time("1s"))));
        elasticsearchClient.indices().refresh(r -> r.index(indexName));
        log.info("인덱스 [{}] 설정 복구 완료: forcemerge={}ms, replica={}ms, refresh={}ms", indexName,
                t1 - t0, t2 - t1, System.currentTimeMillis() - t2);
    }

    private static void sleep() throws IOException {
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("인덱스 복구 대기 중 인터럽트", e);
        }
    }

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

}