package project.matchalatte.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class SyncAliasManagerTest {

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

}
