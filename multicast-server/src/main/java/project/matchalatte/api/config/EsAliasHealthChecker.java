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
