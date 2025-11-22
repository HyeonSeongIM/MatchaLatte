package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
@Slf4j
public class AliasManagementService {

    private static final String DEFAULT_ALIAS = "products";

    private final ElasticsearchClient elasticsearchClient;

    public AliasManagementService(ElasticsearchClient elasticsearchClient) {
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
        updateAliasesBuilder.actions(a -> a.add(ad -> ad.index(newIndexName).alias(aliasName)));

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
                .result()
                .values()
                .stream()
                .findFirst()
                .map(map -> map.aliases().keySet().stream().findFirst().orElse(null));
        }
        catch (Exception e) {
            // 별칭이 존재하지 않는 첫 실행 상황에서는 예외 발생 가능
            log.warn("Alias [{}]를 찾지 못함. 첫 실행으로 간주.", aliasName);
            return Optional.empty();
        }
    }

}