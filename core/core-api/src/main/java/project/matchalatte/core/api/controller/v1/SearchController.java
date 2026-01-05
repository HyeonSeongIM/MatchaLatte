package project.matchalatte.core.api.controller.v1;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import project.matchalatte.core.elasticsearch.service.ProductInfo;
import project.matchalatte.core.elasticsearch.service.SearchService;
import project.matchalatte.core.support.response.ApiResponse;
import project.matchalatte.support.logging.LogData;

@RestController
public class SearchController {

    private final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search/es")
    public ApiResponse<SearchResponse<ProductInfo>> searchProductsFromES(@RequestParam("keyword") String keyword)
            throws Exception {
        log.info("{}", LogData.of("검색 상품 목록 조회", " " + " 상품 목록 조회 API 처리시작"));
        SearchResponse<ProductInfo> result = searchService.searchProducts(keyword);
        log.info("{}", LogData.of("검색 상품 목록 조회", " " + " 상품 목록 조회 API 처리완료"));
        return ApiResponse.success(result);
    }

}
