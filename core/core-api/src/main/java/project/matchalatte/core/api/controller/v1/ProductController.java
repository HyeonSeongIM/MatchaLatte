package project.matchalatte.core.api.controller.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.matchalatte.core.api.controller.v1.request.ProductCreateRequest;
import project.matchalatte.core.api.controller.v1.response.ProductCreateResponse;
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

}
