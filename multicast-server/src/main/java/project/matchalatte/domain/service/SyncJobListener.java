package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class SyncJobListener implements JobExecutionListener {

    private final SyncLockHelper syncLockService;

    private final SyncAliasManager aliasManagementService;

    private static final String ALIAS_NAME = "products";

    public SyncJobListener(SyncLockHelper syncLockService, SyncAliasManager aliasManagementService) {
        this.syncLockService = syncLockService;
        this.aliasManagementService = aliasManagementService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // 새 인덱스 이름은 SyncBatchHelper가 Job 실행 시점에 JobParameter로 넘겨줌
        String newIndexName = jobExecution.getJobParameters().getString("newIndexName");

        log.info("Job 시작. 대상 인덱스: {}", newIndexName);

        try {
            // 1. 새로운 인덱스 생성
            aliasManagementService.createNewIndex(newIndexName);

            // 2. Bulk 색인 속도 향상을 위해 자동 refresh 비활성화
            aliasManagementService.applyBatchSettings(newIndexName);
        }
        catch (IOException e) {
            log.error("Job 시작 전 인덱스 생성 실패", e);
            throw new RuntimeException("Index pre-setup failed", e);
        }
    }

    // Job이 완료(성공/실패)된 후 호출됨
    @Override
    public void afterJob(JobExecution jobExecution) {
        String newIndexName = jobExecution.getJobParameters().getString("newIndexName");

        boolean restored = false;
        if (newIndexName != null) {
            try {
                aliasManagementService.restoreBatchSettings(newIndexName);
                restored = true;
            } catch (Exception e) {
                log.error("refresh_interval 복구 실패 — 인덱스 [{}] 수동 확인 필요", newIndexName, e);
            }
        }

        // 복구 실패한 인덱스(refresh 미실행 → 검색 0건, replica 0)로 교체하면 검색 장애가 되므로 교체하지 않는다
        if (!restored) {
            log.error("인덱스 설정 복구 실패. Alias 교체를 건너뜀 — 기존 인덱스로 계속 서빙");
        } else if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            if (newIndexName == null) {
                log.error("Job Context에서 새 인덱스 이름을 찾을 수 없습니다.");
            } else {
                try {
                    log.info("Job 완료. Alias [{}]를 인덱스 [{}]로 교체 시도.", ALIAS_NAME, newIndexName);
                    aliasManagementService.swapAlias(ALIAS_NAME, newIndexName);
                } catch (IOException e) {
                    log.error("Alias 교체 실패", e);
                }
            }
        } else {
            log.warn("Job이 완료되지 않음. Alias 교체를 건너뜀. 상태: {}", jobExecution.getStatus());
        }

        // Job이 끝났으므로 Lock을 해제합니다.
        syncLockService.stopFullSync();
    }

    // Job 시작 전에는 별도 Lock 로직 필요 없음. (Lock 획득은 SyncController에서 이미 수행)

}