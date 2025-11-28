package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class SyncJobListener implements JobExecutionListener {

    private final SyncLockHelper syncLockService;

    private final SyncAliasManager aliasManagementService;

    private static final String NEW_INDEX_NAME_KEY = "newIndexName";

    private static final String ALIAS_NAME = "products";

    public SyncJobListener(SyncLockHelper syncLockService, SyncAliasManager aliasManagementService) {
        this.syncLockService = syncLockService;
        this.aliasManagementService = aliasManagementService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // 1. 새로운 인덱스 이름 생성
        String newIndexName = ALIAS_NAME + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));

        log.info("Job 시작. 대상 인덱스: {}", newIndexName);

        try {
            // 2. 새로운 인덱스 생성
            aliasManagementService.createNewIndex(newIndexName);

            // 3. 인덱스 이름을 JobExecutionContext에 저장하여 ItemWriter가 사용할 수 있도록 함
            jobExecution.getExecutionContext().put(NEW_INDEX_NAME_KEY, newIndexName);

        }
        catch (IOException e) {
            log.error("Job 시작 전 인덱스 생성 실패", e);
            throw new RuntimeException("Index pre-setup failed", e);
        }
    }

    // Job이 완료(성공/실패)된 후 호출됨
    @Override
    public void afterJob(JobExecution jobExecution) {

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            String newIndexName = jobExecution.getExecutionContext().getString(NEW_INDEX_NAME_KEY);
            if (newIndexName == null) {
                log.error("Job Context에서 새 인덱스 이름을 찾을 수 없습니다.");
                return;
            }

            try {
                // 4. Job 성공 완료 후, Alias를 새 인덱스로 교체 (Atomic Swap)
                log.info("Job 완료. Alias [{}]를 인덱스 [{}]로 교체 시도.", ALIAS_NAME, newIndexName);
                aliasManagementService.swapAlias(ALIAS_NAME, newIndexName);
            }
            catch (IOException e) {
                log.error("Job 완료 후 Alias 교체 실패", e);
                // Alias 교체 실패 시 롤백 또는 알림 처리 필요
            }
        }
        else {
            // Job이 실패한 경우 (FAILED, ABANDONED 등)
            log.warn("Job이 완료되지 않음. Alias 교체를 건너뜀. 상태: {}", jobExecution.getStatus());
            // 실패한 인덱스 정리 로직을 추가할 수도 있습니다.
        }

        // Job이 끝났으므로 Lock을 해제합니다.
        syncLockService.stopFullSync();
    }

    // Job 시작 전에는 별도 Lock 로직 필요 없음. (Lock 획득은 SyncController에서 이미 수행)

}