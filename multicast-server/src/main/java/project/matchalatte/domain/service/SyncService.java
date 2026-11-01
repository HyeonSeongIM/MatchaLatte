package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import project.matchalatte.api.dto.ProductEvent;

@Service
@Slf4j
public class SyncService {

    private final SyncProductHelper syncProductService;

    private final SyncScheduleHelper syncScheduleService;

    private final SyncLockHelper syncLockService;

    private final SyncBatchHelper syncBatchService;

    public SyncService(SyncProductHelper syncProductService, SyncScheduleHelper syncScheduleService,
            SyncLockHelper syncLockService, SyncBatchHelper syncBatchService) {
        this.syncProductService = syncProductService;
        this.syncScheduleService = syncScheduleService;
        this.syncLockService = syncLockService;
        this.syncBatchService = syncBatchService;
    }

    public void registerInQueue(ProductEvent productEvent) {
        syncProductService.enqueue(productEvent);
    }

    // 데이터 정합성 복구를 위한 색인 로직
    public String triggerBatchJob() {
        return syncBatchService.batchProduct();
    }

    // 10초마다 주기적으로 발생하는 색인 로직
    @Scheduled(fixedRate = 10000)
    public void triggerScheduleJob() {
        if (syncLockService.isAvailableForSchedule() && !syncProductService.isEmptyQueue()) {
            syncScheduleService.scheduleFlush();
        }
    }

}
