package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeResponse;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GhostCheckTaskletTest {

    @Mock ElasticsearchClient elasticsearchClient;
    @Mock NamedParameterJdbcTemplate namedJdbc;
    @Mock ValidateReportHelper reportHelper;
    @Mock StepContribution stepContribution;
    @Mock ChunkContext chunkContext;

    GhostCheckTasklet sut;

    @BeforeEach
    void setUp() {
        sut = new GhostCheckTasklet(elasticsearchClient, namedJdbc, reportHelper, "2026-06-24T10:00:00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void RDB에_없는_id는_insertGhost_호출() throws Exception {
        OpenPointInTimeResponse pitOpen = mock(OpenPointInTimeResponse.class);
        SearchResponse<Void> searchResponse = mock(SearchResponse.class);
        HitsMetadata<Void> hitsMetadata = mock(HitsMetadata.class);
        Hit<Void> hit = mock(Hit.class);
        ClosePointInTimeResponse closeResponse = mock(ClosePointInTimeResponse.class);

        when(elasticsearchClient.openPointInTime(any(Function.class))).thenReturn(pitOpen);
        when(pitOpen.id()).thenReturn("pit-1");
        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenReturn(searchResponse);
        when(searchResponse.pitId()).thenReturn("pit-2");
        when(searchResponse.hits()).thenReturn(hitsMetadata);
        when(hitsMetadata.hits()).thenReturn(List.of(hit));
        when(hit.id()).thenReturn("999");
        when(hit.sort()).thenReturn(List.of());
        when(namedJdbc.queryForList(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(List.of());
        when(elasticsearchClient.closePointInTime(any(Function.class))).thenReturn(closeResponse);

        RepeatStatus status = sut.execute(stepContribution, chunkContext);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(reportHelper).insertGhost(999L, "2026-06-24T10:00:00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 예외_발생시_finally에서_PIT_close_호출() throws Exception {
        OpenPointInTimeResponse pitOpen = mock(OpenPointInTimeResponse.class);
        ClosePointInTimeResponse closeResponse = mock(ClosePointInTimeResponse.class);

        when(elasticsearchClient.openPointInTime(any(Function.class))).thenReturn(pitOpen);
        when(pitOpen.id()).thenReturn("pit-1");
        when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
            .thenThrow(new RuntimeException("ES error"));
        when(elasticsearchClient.closePointInTime(any(Function.class))).thenReturn(closeResponse);

        sut.execute(stepContribution, chunkContext);

        verify(elasticsearchClient).closePointInTime(any(Function.class));
    }
}
