package project.matchalatte.core.api.controller.v1;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import project.matchalatte.core.api.controller.v1.request.ProductCreateRequest;
import project.matchalatte.core.api.controller.v1.request.ProductUpdateRequest;
import project.matchalatte.core.api.controller.v1.response.ProductCreateResponse;
import project.matchalatte.core.api.controller.v1.response.ProductReadResponse;
import project.matchalatte.core.api.controller.v1.response.ProductUpdateResponse;
import project.matchalatte.core.domain.product.Product;
import project.matchalatte.core.domain.product.ProductService;
import project.matchalatte.core.domain.product.support.Page;
import project.matchalatte.core.domain.product.support.Slice;
import project.matchalatte.core.elasticsearch.service.ProductInfo;
import project.matchalatte.core.elasticsearch.service.SearchService;
import project.matchalatte.core.support.response.ApiResponse;
import project.matchalatte.support.logging.LogData;
import project.matchalatte.support.logging.UserIdContext;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product")
public class ProductController {

    private final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    private final SearchService searchService;

    public ProductController(ProductService productService, SearchService searchService) {
        this.productService = productService;
        this.searchService = searchService;
    }

    @PostMapping
    public ApiResponse<ProductCreateResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        log.info("{}", LogData.of("상품 생성", "상품 생성 API 처리시작"));
        Product result = productService.createProduct(request.name(), request.description(), request.price(),
                UserIdContext.getCurrentUserId());
        log.info("{}", LogData.of("상품 생성", "상품 생성 API 처리완료"));
        return ApiResponse
            .success(new ProductCreateResponse(result.name(), result.description(), result.price(), result.userId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductReadResponse> readProductById(@PathVariable("id") Long id) {
        Product result = productService.readProductById(id);
        return ApiResponse.success(new ProductReadResponse(result.name(), result.description(), result.price()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductUpdateResponse> updateProduct(@PathVariable("id") Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        log.info("{}", LogData.of("상품 수정", "상품 수정 API 처리시작"));
        Product result = productService.updateProduct(id, request.name(), request.description(), request.price(),
                UserIdContext.getCurrentUserId());
        log.info("{}", LogData.of("상품 수정", "상품 수정 API 처리완료"));
        return ApiResponse
            .success(new ProductUpdateResponse(result.name(), result.description(), result.price(), result.userId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteProduct(@PathVariable("id") Long productId) {
        Long userId = UserIdContext.getCurrentUserId();
        log.info("{}", LogData.of("상품 삭제", "상품 삭제 API 처리시작"));
        productService.deleteProductById(productId, userId);
        log.info("{}", LogData.of("상품 삭제", "상품 삭제 API 처리완료"));
        return ApiResponse.success(null);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<ProductReadResponse>> readProductsByUserId(@PathVariable("userId") Long userId) {
        log.info("{}", LogData.of("특정 유저에 따른 상품 목록 조회", "유저에 따른 상품 목록 조회 API 처리시작"));
        List<Product> result = productService.readProductsByUserId(userId);
        List<ProductReadResponse> responseData = result.stream()
            .map(product -> new ProductReadResponse(product.name(), product.description(), product.price()))
            .toList();
        log.info("{}", LogData.of("특정 유저에 따른 상품 목록 조회", "유저에 따른 상품 목록 조회 API 처리완료"));
        return ApiResponse.success(responseData);
    }

    @GetMapping("/list")
    public ApiResponse<List<ProductReadResponse>> readAllProducts() {
        log.info("{}", LogData.of("전체 상품 목록 조회", "전체 상품 목록 조회 API 처리시작"));
        List<Product> result = productService.readAllProducts();
        List<ProductReadResponse> responseData = result.stream()
            .map(product -> new ProductReadResponse(product.name(), product.description(), product.price()))
            .toList();
        log.info("{}", LogData.of("전체 상품 목록 조회", "전체 상품 목록 조회 API 처리완료"));
        return ApiResponse.success(responseData);
    }

    @GetMapping("/lists")
    public ApiResponse<Page> readAllProductsByPageable(@RequestParam int offset, @RequestParam int limit) {
        log.info("{}", LogData.of("전체 상품 목록 조회", "전체 상품 목록 조회 API 처리시작"));
        Page result = productService.readProductsPage(offset, limit);
        log.info("{}", LogData.of("전체 상품 목록 조회", "전체 상품 목록 조회 API 처리완료"));
        return ApiResponse.success(result);
    }

    @GetMapping("/listss")
    public ApiResponse<Slice> readAllProductsSlice(@RequestParam int offset, @RequestParam int limit) {
        log.info("{}", LogData.of("전체 상품 목록 조회", "전체 상품 목록 조회 API 처리시작"));
        Slice result = productService.readProductsSlice(offset, limit);
        log.info("{}", LogData.of("전체 상품 목록 조회", "전체 상품 목록 조회 API 처리완료"));
        return ApiResponse.success(result);
    }

    @GetMapping("/list/noOffset")
    public ApiResponse<Slice> readAllProductsSliceNoOffset(@RequestParam int limit,
            @RequestParam(required = false) Long lastId) {
        log.info("{}", LogData.of("전체 상품 목록 조회", "전체 상품 목록 조회 API 처리시작"));
        Slice result = productService.readProductsSliceNoOffset(limit, lastId);
        log.info("{}", LogData.of("전체 상품 목록 조회", "전체 상품 목록 조회 API 처리완료"));
        return ApiResponse.success(result);
    }

    @GetMapping("/search")
    public ApiResponse<List<ProductReadResponse>> readAllProductsBySearch(@RequestParam String keyword,
            @RequestParam int page, @RequestParam int size) {
        log.info("{}", LogData.of("검색 상품 목록 조회", keyword + " 상품 목록 조회 API 처리시작"));
        List<Product> result = productService.findProductsByName(keyword, page, size);
        List<ProductReadResponse> responseData = result.stream()
            .map(product -> new ProductReadResponse(product.name(), product.description(), product.price()))
            .toList();
        log.info("{}", LogData.of("검색 상품 목록 조회", keyword + " 상품 목록 조회 API 처리완료"));
        return ApiResponse.success(responseData);
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
