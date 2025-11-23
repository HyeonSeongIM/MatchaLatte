package project.matchalatte.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.matchalatte.api.dto.ProductEvent;
import project.matchalatte.api.dto.ProductInfo;
import project.matchalatte.domain.service.SyncService;

@RestController
@Slf4j
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/api/internal/sync/products")
    public void syncProduct(@RequestBody ProductEvent productInfo) {
        syncService.registerInQueue(productInfo);
    }

    @PostMapping("/api/internal/sync/mysql-to-es")
    public String runSyncJob() {
        return syncService.triggerBatchJob();
    }

}
