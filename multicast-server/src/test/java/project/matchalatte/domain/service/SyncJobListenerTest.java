package project.matchalatte.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncJobListenerTest {

    @Mock SyncLockHelper syncLockHelper;
    @Mock SyncAliasManager aliasManager;

    @InjectMocks
    SyncJobListener sut;

    private JobExecution completedJob() {
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getJobParameters())
            .thenReturn(new JobParametersBuilder().addString("newIndexName", "products_new").toJobParameters());
        lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        return jobExecution;
    }

    @Test
    void afterJob_설정복구_실패시_alias_교체_안함() throws Exception {
        doThrow(new IOException("forcemerge 실패")).when(aliasManager).restoreBatchSettings("products_new");

        sut.afterJob(completedJob());

        verify(aliasManager, never()).swapAlias(anyString(), anyString());
        verify(syncLockHelper).stopFullSync();
    }

    @Test
    void afterJob_설정복구_성공시_alias_교체() throws Exception {
        sut.afterJob(completedJob());

        verify(aliasManager).swapAlias("products", "products_new");
        verify(syncLockHelper).stopFullSync();
    }

}
