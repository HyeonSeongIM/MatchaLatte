package project.matchalatte.core.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final String ALIAS_NAME = "products";

    private final ElasticsearchClient elasticsearchClient;

    public SearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public SearchResponse<ProductInfo> searchProducts(String query) throws Exception {

        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(ALIAS_NAME)
            .query(q -> q.match(m -> m.field("name").query(query)))
            .size(10));

        return elasticsearchClient.search(searchRequest, ProductInfo.class);
    }

}
