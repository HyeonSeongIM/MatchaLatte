package project.matchalatte.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.matchalatte.api.dto.ProductEvent;
import project.matchalatte.domain.service.SyncService;
import project.matchalatte.support.logging.LogData;

@RestController
public class SyncController {

    private final Logger log = LoggerFactory.getLogger(SyncController.class);

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/api/internal/sync/products")
    public void syncProduct(@RequestBody ProductEvent productInfo, HttpServletRequest request) {
        log.info("{}", LogData.of(request.getHeader("X-Trace-Id"), productInfo.id(), "상품 데이터 수신 API", "상품 데이터 수신 완료"));
        syncService.registerInQueue(productInfo);
        log.info("{}",
                LogData.of(request.getHeader("X-Trace-Id"), productInfo.id(), "상품 데이터 수신 API", "상품 데이터 큐 저장 완료"));
    }

    @PostMapping("/api/internal/sync/mysql-to-es")
    public String runSyncJob() {
        return syncService.triggerBatchJob();
    }

}
