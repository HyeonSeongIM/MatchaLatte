package project.matchalatte.api.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import project.matchalatte.domain.entity.ProductDocument;

@Slf4j
public class ElasticsearchItemWriter implements ItemWriter<ProductDocument> {

    private final ElasticsearchClient elasticsearchClient;

    private final String indexName;

    public ElasticsearchItemWriter(ElasticsearchClient elasticsearchClient, String indexName) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexName = indexName;
    }

    @Override
    public void write(Chunk<? extends ProductDocument> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        BulkRequest.Builder br = new BulkRequest.Builder();

        // Chunk의 모든 아이템을 BulkRequest에 추가
        for (ProductDocument doc : chunk.getItems()) {
            br.operations(op -> op.index(idx -> idx.index(indexName).id(doc.getId().toString()).document(doc)));
        }

        // Bulk 요청 전송
        BulkResponse result = elasticsearchClient.bulk(br.build());

        if (result.errors()) {
            log.error("Elasticsearch Bulk Write에 오류 발생. 처리된 아이템 수: {}", chunk.size());
            result.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("도큐먼트 ID: {}, 오류: {}", item.id(), item.error().reason());
                }
            });
            // 필요에 따라 예외를 던져 Step을 실패 처리할 수 있습니다.
            // throw new RuntimeException("Elasticsearch Bulk Write Failed");
        }
        else {
            log.info("Elasticsearch Bulk Write 성공. 처리된 아이템 수: {}, 소요 시간: {}ms", chunk.size(), result.took());
        }
    }

}