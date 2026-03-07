package project.matchalatte.integration;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import project.matchalatte.core.elasticsearch.service.ProductInfo;
import project.matchalatte.core.elasticsearch.service.SearchService;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@WithMockUser
@Tag("integration")
class SearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchService searchService;

    @Test
    @DisplayName("검색 통합 테스트")
    void searchHappyPath_integration() throws Exception {
        // given
        String keyword = "말차";
        ProductInfo mockProduct = new ProductInfo(1L, "말차라떼 팔아요", "제가 좋아하는 말차라떼에요.", 2100L, 1L);

        // when
        // (1) 가짜 SearchResponse 생성
        SearchResponse<ProductInfo> mockResponse = SearchResponse.of(r -> r.took(10) // 검색
                                                                                     // 소요
                                                                                     // 시간
            .timedOut(false)
            .shards(s -> s.total(1).successful(1).skipped(0).failed(0))
            .hits(h -> h.total(t -> t.value(1).relation(TotalHitsRelation.Eq)) // 총 검색 결과
                                                                               // 수
                .hits(List.of(Hit.of(hit -> hit.index("products_2026_03_05").id("1").source(mockProduct) // 우리가
                                                                                                         // 찾은
                                                                                                         // 상품
                                                                                                         // 데이터!
                )))));

        // (2) 서비스 생성 후 가짜 데이터 반환
        Mockito.when(searchService.searchProducts(keyword)).thenReturn(mockResponse);

        // then
        mockMvc.perform(get("/search/es").param("keyword", keyword).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));

    }

}
