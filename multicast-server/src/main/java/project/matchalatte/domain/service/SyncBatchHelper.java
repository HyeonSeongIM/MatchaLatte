package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class SyncBatchHelper {

    private final JobLauncher jobLauncher;

    private final Job mysqlToEsJob;

    private final SyncLockHelper syncLockService;

    public SyncBatchHelper(JobLauncher jobLauncher, Job mysqlToEsJob, SyncLockHelper syncLockService) {
        this.jobLauncher = jobLauncher;
        this.mysqlToEsJob = mysqlToEsJob;
        this.syncLockService = syncLockService;
    }

    public String batchProduct() {
        if (!syncLockService.startFullSync()) {
            return "Batch Job 실행 요청 실패.";
        }

        try {
            // Job 실행 파라미터 생성 (수동 실행용 고정 파라미터)
            JobParameters jobParameters = new JobParametersBuilder().addString("full_sync_mode", "MANUAL")
                .addDate("run.date", new Date())
                .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(mysqlToEsJob, jobParameters);

            return "Batch Job (ID: " + jobExecution.getId() + ") 실행 요청 완료.";

        }
        catch (Exception e) {
            // Job 실행 요청 자체가 실패했을 경우, 리스너가 호출되지 않으므로 수동으로 Lock 해제
            syncLockService.stopFullSync();
            log.error("Batch Job 실행 요청 중 오류 발생", e);
            return "Batch Job 실행 요청 중 오류 발생: " + e.getMessage();
        }
    }

}
