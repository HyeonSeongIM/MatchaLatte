package project.matchalatte.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateJobListenerTest {

    @Mock ValidateReportHelper reportHelper;
    @Mock SyncLockHelper syncLockHelper;

    @InjectMocks
    ValidateJobListener sut;

    @Test
    void afterJob_완료시_Lock_해제_호출() {
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(reportHelper.countByIssueType("MISSING")).thenReturn(3);
        when(reportHelper.countByIssueType("GHOST")).thenReturn(1);
        when(reportHelper.countByIssueType("SKIPPED")).thenReturn(0);

        sut.afterJob(jobExecution);

        verify(syncLockHelper).stopFullSync();
    }

    @Test
    void afterJob_실패시에도_Lock_해제_호출() {
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(reportHelper.countByIssueType(anyString())).thenReturn(0);

        sut.afterJob(jobExecution);

        verify(syncLockHelper).stopFullSync();
    }
}
