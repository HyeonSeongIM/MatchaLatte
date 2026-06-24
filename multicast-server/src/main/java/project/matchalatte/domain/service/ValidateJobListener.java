package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ValidateJobListener implements JobExecutionListener {

    private final ValidateReportHelper reportHelper;
    private final SyncLockHelper syncLockHelper;

    public ValidateJobListener(ValidateReportHelper reportHelper, SyncLockHelper syncLockHelper) {
        this.reportHelper = reportHelper;
        this.syncLockHelper = syncLockHelper;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        try {
            String runAt = jobExecution.getJobParameters().getString("run_at");
            int missing = reportHelper.countByIssueType("MISSING", runAt);
            int ghost   = reportHelper.countByIssueType("GHOST", runAt);
            int skipped = reportHelper.countByIssueType("SKIPPED", runAt);

            log.info("[검증 완료] 상태={} | run_at={} | MISSING={} | GHOST={} | SKIPPED={}",
                jobExecution.getStatus(), runAt, missing, ghost, skipped);

            if (skipped > 0) {
                log.warn("[주의] SKIPPED 청크 {}개 존재 — 해당 범위는 미검증 상태입니다. validate_report 확인 필요.", skipped);
            }
        } finally {
            syncLockHelper.stopFullSync();
        }
    }
}
