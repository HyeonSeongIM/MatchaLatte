package project.matchalatte.api.config;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;
import project.matchalatte.domain.service.SyncLockService;

@Component
public class FullSyncJobListener implements JobExecutionListener {

    private final SyncLockService syncLockService;

    public FullSyncJobListener(SyncLockService syncLockService) {
        this.syncLockService = syncLockService;
    }

    // Job이 완료(성공/실패)된 후 호출됨
    @Override
    public void afterJob(JobExecution jobExecution) {
        // Job이 끝났으므로 Lock을 해제합니다.
        syncLockService.stopFullSync();
    }

    // Job 시작 전에는 별도 Lock 로직 필요 없음. (Lock 획득은 SyncController에서 이미 수행)

}