package project.matchalatte.core.api.controller.v1;

import org.springframework.web.bind.annotation.*;
import project.matchalatte.core.api.controller.v1.request.ProductCreateRequest;
import project.matchalatte.core.api.controller.v1.request.ProductUpdateRequest;
import project.matchalatte.core.api.controller.v1.response.ProductCreateResponse;
import project.matchalatte.core.api.controller.v1.response.ProductReadResponse;
import project.matchalatte.core.api.controller.v1.response.ProductUpdateResponse;
import project.matchalatte.core.domain.product.Product;
import project.matchalatte.core.domain.product.ProductService;
import project.matchalatte.core.support.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ApiResponse<ProductCreateResponse> createProduct(@RequestBody ProductCreateRequest request) {
        Product result = productService.createProduct(request.name(), request.description(), request.price());
        return ApiResponse.success(new ProductCreateResponse(result.name(), result.description(), result.price()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductReadResponse> readProductById(@PathVariable("id") Long id) {
        Product result = productService.readProductById(id);
        return ApiResponse.success(new ProductReadResponse(result.name(), result.description(), result.price()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductUpdateResponse> updateProduct(@PathVariable("id") Long id,
            @RequestBody ProductUpdateRequest request) {
        Product result = productService.updateProduct(id, request.name(), request.description(), request.price());
        return ApiResponse.success(new ProductUpdateResponse(result.name(), result.description(), result.price()));
    }

}
