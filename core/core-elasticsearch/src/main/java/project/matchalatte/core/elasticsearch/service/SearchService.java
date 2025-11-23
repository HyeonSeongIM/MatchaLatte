package project.matchalatte.core.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;

    public SearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public SearchResponse<ProductInfo> searchProducts(String query) throws Exception {

        String indexName = "products_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));

        SearchRequest searchRequest = SearchRequest.of(s -> s
            // 💡 인덱스 이름 대신 Alias를 사용합니다.
            .index(indexName)
            .query(q -> q.match(m -> m.field("name").query(query)))
            .size(10));

        return elasticsearchClient.search(searchRequest, ProductInfo.class // 실제 Document
                                                                           // Class (예:
                                                                           // ProductDocument.class)로
                                                                           // 변경해야 합니다.
        );
    }

}
