package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.function.Function;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;

@ExtendWith(MockitoExtension.class)
class SyncAliasManagerTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private SyncAliasManager syncAliasManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void productIndexSettings_샤드_레플리카_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/settings/number_of_shards").asInt()).isEqualTo(1);
        assertThat(root.at("/settings/number_of_replicas").asInt()).isEqualTo(0);
    }

    @Test
    void productIndexSettings_dynamic_strict_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/mappings/dynamic").asText()).isEqualTo("strict");
    }

    @Test
    void productIndexSettings_name_필드_nori_분석기_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/mappings/properties/name/type").asText()).isEqualTo("text");
        assertThat(root.at("/mappings/properties/name/analyzer").asText()).isEqualTo("nori_analyzer");
        assertThat(root.at("/mappings/properties/name/fields/keyword/type").asText()).isEqualTo("keyword");
    }

    @Test
    void productIndexSettings_nori_decompound_mode_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/settings/analysis/tokenizer/nori_tokenizer/type").asText())
                .isEqualTo("nori_tokenizer");
        assertThat(root.at("/settings/analysis/tokenizer/nori_tokenizer/decompound_mode").asText())
                .isEqualTo("mixed");
    }

    @Test
    void productIndexSettings_description_필드_없음_검증() throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource("product-index-settings.json").getInputStream());

        assertThat(root.at("/mappings/properties/description").isMissingNode()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createNewIndex_acknowledged_true_정상_종료() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
        CreateIndexResponse mockResponse = mock(CreateIndexResponse.class);

        when(elasticsearchClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.create(any(Function.class))).thenReturn(mockResponse);
        when(mockResponse.acknowledged()).thenReturn(true);

        assertThatCode(() -> syncAliasManager.createNewIndex("products_2026_06_23"))
                .doesNotThrowAnyException();

        verify(mockIndicesClient).create(any(Function.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createNewIndex_acknowledged_false_RuntimeException_발생() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
        CreateIndexResponse mockResponse = mock(CreateIndexResponse.class);

        when(elasticsearchClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.create(any(Function.class))).thenReturn(mockResponse);
        when(mockResponse.acknowledged()).thenReturn(false);

        assertThatThrownBy(() -> syncAliasManager.createNewIndex("products_2026_06_23"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Index creation failed.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createNewIndex_인덱스_이미_존재시_예외_무시() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);

        when(elasticsearchClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.create(any(Function.class)))
                .thenThrow(new RuntimeException("resource_already_exists_exception"));

        assertThatCode(() -> syncAliasManager.createNewIndex("products_2026_06_23"))
                .doesNotThrowAnyException();
    }

}
