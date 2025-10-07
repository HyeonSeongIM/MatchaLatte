package project.matchalatte.core.api.controller.v1;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import project.matchalatte.core.api.controller.v1.request.ProductCreateRequest;
import project.matchalatte.core.api.controller.v1.request.ProductUpdateRequest;
import project.matchalatte.core.api.controller.v1.response.ProductCreateResponse;
import project.matchalatte.core.api.controller.v1.response.ProductReadResponse;
import project.matchalatte.core.api.controller.v1.response.ProductUpdateResponse;
import project.matchalatte.core.domain.product.Product;
import project.matchalatte.core.domain.product.ProductService;
import project.matchalatte.core.support.response.ApiResponse;
import project.matchalatte.support.logging.LogData;
import project.matchalatte.support.logging.TraceIdContext;
import project.matchalatte.support.logging.UserIdContext;

import org.slf4j.Logger;

@RestController
@RequestMapping("/api/v1/product")
public class ProductController {

    private final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ApiResponse<ProductCreateResponse> createProduct(@RequestBody ProductCreateRequest request) {
        String traceId = TraceIdContext.traceId();
        Long userId = UserIdContext.getCurrentUserId();
        log.info("{}", LogData.of(traceId, userId, "상품 생성", "상품 생성 API 처리시작"));
        Product result = productService.createProduct(request.name(), request.description(), request.price(), userId);
        log.info("{}", LogData.of(traceId, userId, "상품 생성", "상품 생성 API 처리완료"));
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
            @RequestBody ProductUpdateRequest request) {
        String traceId = TraceIdContext.traceId();
        Long userId = UserIdContext.getCurrentUserId();
        log.info("{}", LogData.of(traceId, userId, "상품 수정", "상품 수정 API 처리시작"));
        Product result = productService.updateProduct(id, request.name(), request.description(), request.price(),
                userId);
        log.info("{}", LogData.of(traceId, userId, "상품 수정", "상품 수정 API 처리시작"));
        return ApiResponse
            .success(new ProductUpdateResponse(result.name(), result.description(), result.price(), result.userId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProductById(id);
        return ApiResponse.success(null);
    }

}
