package project.matchalatte.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateJobListenerTest {

    @Mock ValidateReportHelper reportHelper;
    @Mock SyncLockHelper syncLockHelper;

    @InjectMocks
    ValidateJobListener sut;

    private static final String RUN_AT = "2026-06-24T10:00:00";

    private JobExecution jobExecutionWithRunAt(BatchStatus status) {
        JobExecution jobExecution = mock(JobExecution.class);
        JobParameters params = new JobParametersBuilder()
            .addString("run_at", RUN_AT)
            .toJobParameters();
        when(jobExecution.getStatus()).thenReturn(status);
        when(jobExecution.getJobParameters()).thenReturn(params);
        return jobExecution;
    }

    @Test
    void afterJob_완료시_Lock_해제_호출() {
        JobExecution jobExecution = jobExecutionWithRunAt(BatchStatus.COMPLETED);
        when(reportHelper.countByIssueType("MISSING", RUN_AT)).thenReturn(3);
        when(reportHelper.countByIssueType("GHOST", RUN_AT)).thenReturn(1);
        when(reportHelper.countByIssueType("SKIPPED", RUN_AT)).thenReturn(0);

        sut.afterJob(jobExecution);

        verify(syncLockHelper).stopFullSync();
    }

    @Test
    void afterJob_실패시에도_Lock_해제_호출() {
        JobExecution jobExecution = jobExecutionWithRunAt(BatchStatus.FAILED);
        when(reportHelper.countByIssueType(anyString(), eq(RUN_AT))).thenReturn(0);

        sut.afterJob(jobExecution);

        verify(syncLockHelper).stopFullSync();
    }

    @Test
    void afterJob_countByIssueType_예외에도_Lock_해제_호출() {
        // getStatus()는 예외 발생 전에 호출되지 않으므로 stubbing 없이 생성
        JobExecution jobExecution = mock(JobExecution.class);
        JobParameters params = new JobParametersBuilder()
            .addString("run_at", RUN_AT)
            .toJobParameters();
        when(jobExecution.getJobParameters()).thenReturn(params);
        when(reportHelper.countByIssueType(anyString(), eq(RUN_AT))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> sut.afterJob(jobExecution));

        verify(syncLockHelper).stopFullSync();
    }
}
