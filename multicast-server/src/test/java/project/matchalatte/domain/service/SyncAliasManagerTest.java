package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.elasticsearch.indices.update_aliases.AddAction;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Test
    @SuppressWarnings("unchecked")
    void swapAlias_isWriteIndex_true_설정() throws Exception {
        ElasticsearchIndicesClient mockIndicesClient = mock(ElasticsearchIndicesClient.class);
        GetAliasResponse mockGetAliasResponse = mock(GetAliasResponse.class);
        UpdateAliasesResponse mockUpdateResponse = mock(UpdateAliasesResponse.class);

        when(elasticsearchClient.indices()).thenReturn(mockIndicesClient);
        when(mockIndicesClient.getAlias(any(Function.class))).thenReturn(mockGetAliasResponse);
        when(mockGetAliasResponse.result()).thenReturn(Map.of());
        when(mockIndicesClient.updateAliases(any(UpdateAliasesRequest.class))).thenReturn(mockUpdateResponse);
        when(mockUpdateResponse.acknowledged()).thenReturn(true);

        syncAliasManager.swapAlias("products", "products_2026_06_24");

        ArgumentCaptor<UpdateAliasesRequest> captor = ArgumentCaptor.forClass(UpdateAliasesRequest.class);
        verify(mockIndicesClient).updateAliases(captor.capture());
        UpdateAliasesRequest request = captor.getValue();

        AddAction addAction = request.actions().stream()
            .filter(a -> a.add() != null)
            .map(Action::add)
            .findFirst()
            .orElseThrow(() -> new AssertionError("add action 없음"));

        assertThat(addAction.isWriteIndex()).isTrue();
    }

}
