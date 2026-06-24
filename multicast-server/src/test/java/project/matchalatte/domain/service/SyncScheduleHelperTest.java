package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.matchalatte.api.dto.EventType;
import project.matchalatte.api.dto.ProductEvent;

import java.util.LinkedList;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncScheduleHelperTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    private Queue<ProductEvent> productQueue;
    private SyncScheduleHelper syncScheduleHelper;

    private ProductEvent sampleEvent() {
        return new ProductEvent(EventType.CREATE, 42L, "테스트상품", "설명", 10000L, 1L);
    }

    private void setupHelper() {
        syncScheduleHelper = new SyncScheduleHelper(productQueue, elasticsearchClient);
    }

    @Test
    void flush_bulk_대상이_products_alias() throws Exception {
        productQueue = new LinkedList<>();
        productQueue.offer(sampleEvent());
        setupHelper();

        BulkResponse mockResponse = mock(BulkResponse.class);
        when(mockResponse.errors()).thenReturn(false);
        when(mockResponse.took()).thenReturn(3L);
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(mockResponse);

        syncScheduleHelper.scheduleFlush();

        ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
        verify(elasticsearchClient).bulk(captor.capture());
        String actualIndex = captor.getValue().operations().get(0).index().index();
        assertThat(actualIndex).isEqualTo("products");
    }

    @Test
    void flush_alias_없음_404_예외_전파_안됨() throws Exception {
        productQueue = new LinkedList<>();
        productQueue.offer(sampleEvent());
        setupHelper();

        ElasticsearchException ex = mock(ElasticsearchException.class);
        when(ex.status()).thenReturn(404);
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenThrow(ex);

        assertThatCode(() -> syncScheduleHelper.scheduleFlush()).doesNotThrowAnyException();
    }

    @Test
    void flush_일반_예외_전파_안됨() throws Exception {
        productQueue = new LinkedList<>();
        productQueue.offer(sampleEvent());
        setupHelper();

        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenThrow(new RuntimeException("network error"));

        assertThatCode(() -> syncScheduleHelper.scheduleFlush()).doesNotThrowAnyException();
    }
}
