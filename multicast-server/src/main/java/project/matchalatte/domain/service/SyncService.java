package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import project.matchalatte.api.dto.ProductInfo;

@Service
@Slf4j
public class SyncService {

    private final SyncProductService syncProductService;

    private final SyncScheduleService syncScheduleService;

    private final SyncLockService syncLockService;

    private final SyncBatchService syncBatchService;

    public SyncService(SyncProductService syncProductService, SyncScheduleService syncScheduleService,
            SyncLockService syncLockService, SyncBatchService syncBatchService) {
        this.syncProductService = syncProductService;
        this.syncScheduleService = syncScheduleService;
        this.syncLockService = syncLockService;
        this.syncBatchService = syncBatchService;
    }

    public void registerInQueue(ProductInfo productInfo) {
        syncProductService.enqueue(productInfo);
    }

    public String triggerBatchJob() {
        return syncBatchService.batchProduct();
    }

    @Scheduled(fixedRate = 10000)
    public void triggerScheduleJob() {
        if (syncLockService.isAvailableForSchedule() && !syncProductService.isEmptyQueue()) {
            syncScheduleService.scheduleFlush();
        }
    }

}
