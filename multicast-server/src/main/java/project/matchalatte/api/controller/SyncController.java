package project.matchalatte.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.matchalatte.api.dto.ProductInfo;
import project.matchalatte.domain.service.ProductBulkService;

@RestController
@Slf4j
public class SyncController {

    private final ProductBulkService productBulkService;

    private final JobLauncher jobLauncher;

    private final Job mysqlToEsJob;

    public SyncController(ProductBulkService productBulkService, JobLauncher jobLauncher, Job mysqlToEsJob) {
        this.productBulkService = productBulkService;
        this.jobLauncher = jobLauncher;
        this.mysqlToEsJob = mysqlToEsJob;
    }

    @PostMapping("/api/internal/sync/products")
    public void syncProduct(@RequestBody ProductInfo productInfo) {
        productBulkService.add(productInfo);
    }

    @PostMapping("/api/internal/sync/mysql-to-es")
    public String syncBatchProducts() {
        try {
            JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
                .toJobParameters();

            // Job 실행
            JobExecution jobExecution = jobLauncher.run(mysqlToEsJob, jobParameters);

            log.info("Batch Job 실행 요청 완료. Status: {}", jobExecution.getStatus());

            return "Batch Job (ID: " + jobExecution.getId() + ") 실행 요청 완료. 상태: " + jobExecution.getStatus();

        }
        catch (Exception e) {
            log.error("Batch Job 실행 중 오류 발생", e);
            return "Batch Job 실행 중 오류 발생: " + e.getMessage();
        }
    }

}
