package project.matchalatte.presentation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.matchalatte.service.ProductBulkService;

@RestController
public class SyncController {

    private final ProductBulkService productBulkService;

    public SyncController(ProductBulkService productBulkService) {
        this.productBulkService = productBulkService;
    }

    @PostMapping("/api/sync/products")
    public void syncProduct(@RequestBody ProductInfo productInfo) {
        productBulkService.add(productInfo);
    }

}
