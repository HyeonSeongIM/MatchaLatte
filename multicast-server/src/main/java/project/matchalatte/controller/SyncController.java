package project.matchalatte.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.matchalatte.dto.ProductInfo;
import project.matchalatte.service.ProductBulkService;

@RestController
@Slf4j
public class SyncController {

    private final ProductBulkService productBulkService;

    public SyncController(ProductBulkService productBulkService) {
        this.productBulkService = productBulkService;
    }

    @PostMapping("/api/internal/sync/products")
    public void syncProduct(@RequestBody ProductInfo productInfo) {
        productBulkService.add(productInfo);
    }

}
