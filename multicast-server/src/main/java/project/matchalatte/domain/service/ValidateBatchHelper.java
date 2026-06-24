package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
@Slf4j
public class ValidateBatchHelper {

    private final JobLauncher jobLauncher;
    private final Job validateSyncJob;
    private final SyncLockHelper syncLockHelper;

    public ValidateBatchHelper(JobLauncher jobLauncher,
                               @Qualifier("validateSyncJob") Job validateSyncJob,
                               SyncLockHelper syncLockHelper) {
        this.jobLauncher = jobLauncher;
        this.validateSyncJob = validateSyncJob;
        this.syncLockHelper = syncLockHelper;
    }

    public String validateProduct() {
        if (!syncLockHelper.startFullSync()) {
            return "Validate Job 실행 요청 실패 — 이미 동기화 작업 진행 중.";
        }
        try {
            String runAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            JobParameters params = new JobParametersBuilder()
                .addString("run_at", runAt)
                .addDate("run.date", new Date())
                .toJobParameters();

            var jobExecution = jobLauncher.run(validateSyncJob, params);
            return "Validate Job (ID: " + jobExecution.getId() + ") 실행 요청 완료. run_at=" + runAt;
        } catch (Exception e) {
            syncLockHelper.stopFullSync();
            log.error("Validate Job 실행 요청 중 오류", e);
            return "Validate Job 실행 오류: " + e.getMessage();
        }
    }
}
