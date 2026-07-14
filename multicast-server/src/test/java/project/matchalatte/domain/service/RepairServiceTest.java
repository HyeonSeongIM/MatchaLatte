package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairServiceTest {

    @Mock ValidateReportHelper reportHelper;
    @Mock ElasticsearchClient elasticsearchClient;
    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks
    RepairService sut;

    private static final String RUN_AT = "2026-06-24T10:00:00";

    @Test
    void validate_report_비어있으면_색인_안_함() {
        when(reportHelper.findLatestRunAt()).thenReturn(Optional.empty());

        String result = sut.repair();

        assertThat(result).contains("MISSING 항목 없음");
        verifyNoInteractions(elasticsearchClient);
    }

    @Test
    void MISSING_없으면_색인_안_함() {
        when(reportHelper.findLatestRunAt()).thenReturn(Optional.of(RUN_AT));
        when(reportHelper.findMissingProductIds(RUN_AT)).thenReturn(List.of());

        String result = sut.repair();

        assertThat(result).contains("MISSING 항목 없음");
        verifyNoInteractions(elasticsearchClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void MISSING_id로_RDB_조회_후_Bulk_색인() throws Exception {
        when(reportHelper.findLatestRunAt()).thenReturn(Optional.of(RUN_AT));
        when(reportHelper.findMissingProductIds(RUN_AT)).thenReturn(List.of(1L, 2L));
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
            .thenReturn(List.of(
                new SyncProductInfo(1L, "상품A", "설명A", 10000L, 100L),
                new SyncProductInfo(2L, "상품B", "설명B", 20000L, 200L)
            ));
        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(false);
        when(elasticsearchClient.bulk(any(Function.class))).thenReturn(bulkResponse);

        String result = sut.repair();

        assertThat(result).contains("2");
        verify(elasticsearchClient).bulk(any(Function.class));
    }
}
