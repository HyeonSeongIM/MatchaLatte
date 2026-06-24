package project.matchalatte.api.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsAliasHealthCheckerTest {

    @Mock
    private ElasticsearchClient esClient;

    @InjectMocks
    private EsAliasHealthChecker healthChecker;

    @Test
    @SuppressWarnings("unchecked")
    void alias_존재시_정상_기동() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
        BooleanResponse mockBoolResponse = mock(BooleanResponse.class);
        when(esClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.existsAlias(any(Function.class))).thenReturn(mockBoolResponse);
        when(mockBoolResponse.value()).thenReturn(true);

        assertThatCode(() -> healthChecker.run(mock(ApplicationArguments.class)))
            .doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void alias_없을시_IllegalStateException_발생() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
        BooleanResponse mockBoolResponse = mock(BooleanResponse.class);
        when(esClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.existsAlias(any(Function.class))).thenReturn(mockBoolResponse);
        when(mockBoolResponse.value()).thenReturn(false);

        assertThatThrownBy(() -> healthChecker.run(mock(ApplicationArguments.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("'products'")
            .hasMessageContaining("mysqlToEsJob");
    }
}
