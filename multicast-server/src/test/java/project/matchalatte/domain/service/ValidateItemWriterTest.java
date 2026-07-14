package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.mget.MultiGetError;
import co.elastic.clients.elasticsearch.core.get.GetResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.util.List;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateItemWriterTest {

    @Mock ElasticsearchClient elasticsearchClient;
    @Mock ValidateReportHelper reportHelper;

    ValidateItemWriter sut;

    @BeforeEach
    void setUp() {
        sut = new ValidateItemWriter(elasticsearchClient, reportHelper, "2026-06-24T10:00:00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void mget_결과_found_false면_insertMissing_호출() throws Exception {
        MgetResponse<Void> mockResponse = mock(MgetResponse.class);
        MultiGetResponseItem<Void> missingItem = mock(MultiGetResponseItem.class);
        GetResult<Void> getResult = mock(GetResult.class);

        when(elasticsearchClient.mget(any(Function.class), eq(Void.class))).thenReturn(mockResponse);
        when(mockResponse.docs()).thenReturn(List.of(missingItem));
        when(missingItem.isFailure()).thenReturn(false);
        when(missingItem.result()).thenReturn(getResult);
        when(getResult.found()).thenReturn(false);
        when(getResult.id()).thenReturn("42");

        sut.write(new Chunk<>(List.of(42L)));

        verify(reportHelper).insertMissing(42L, "2026-06-24T10:00:00");
        verify(reportHelper, never()).insertSkipped(anyLong(), anyLong(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void mget_예외_발생시_insertSkipped_호출() throws Exception {
        when(elasticsearchClient.mget(any(Function.class), eq(Void.class)))
            .thenThrow(new RuntimeException("ES connection refused"));

        sut.write(new Chunk<>(List.of(1L, 2L, 3L)));

        verify(reportHelper).insertSkipped(1L, 3L, "2026-06-24T10:00:00");
        verify(reportHelper, never()).insertMissing(anyLong(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void mget_item_failure면_skip_처리() throws Exception {
        MgetResponse<Void> mockResponse = mock(MgetResponse.class);
        MultiGetResponseItem<Void> failureItem = mock(MultiGetResponseItem.class);

        when(elasticsearchClient.mget(any(Function.class), eq(Void.class))).thenReturn(mockResponse);
        when(mockResponse.docs()).thenReturn(List.of(failureItem));
        when(failureItem.isFailure()).thenReturn(true);

        sut.write(new Chunk<>(List.of(5L)));

        verify(reportHelper, never()).insertMissing(anyLong(), anyString());
        verify(reportHelper, never()).insertSkipped(anyLong(), anyLong(), anyString());
    }
}
