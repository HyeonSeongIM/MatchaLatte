package project.matchalatte.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import project.matchalatte.core.api.controller.v1.request.ProductCreateRequest;
import project.matchalatte.core.api.controller.v1.request.ProductUpdateRequest;
import project.matchalatte.core.domain.product.Product;
import project.matchalatte.core.domain.product.ProductService;
import project.matchalatte.storage.db.core.ProductEntity;
import project.matchalatte.storage.db.core.ProductJpaRepository;
import project.matchalatte.support.logging.UserIdContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@WithMockUser
@Tag("integration")
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // UserId 뽑아내는 용도
    private MockedStatic<UserIdContext> mockedUserIdContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductJpaRepository productRepository;

    @BeforeEach
    void setUp() {
        // (1) 저장된 객체들 전부 다 삭제
        productRepository.deleteAll();

        // (2) 사용자 정보 세팅
        mockedUserIdContext = Mockito.mockStatic(UserIdContext.class);
        mockedUserIdContext.when(UserIdContext::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        // (1) 사용자 정보 초기화
        mockedUserIdContext.close();
    }

    @Test
    @DisplayName("상품 생성 통합 테스트")
    void productCreate_integration() throws Exception {
        // given
        ProductCreateRequest req = new ProductCreateRequest("곰돌이 인형", "설명입니다.설명이에요.", 35000L);

        // when & then
        mockMvc
            .perform(post("/api/v1/product").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));

        // (1) 실제 데이터 베이스 검증
        Product saved = productRepository.findByName("곰돌이 인형");
        assertThat(saved.price()).isEqualTo(35000L);
    }

    @Test
    @DisplayName("상품 수정 통합 테스트")
    void productUpdate_integration() throws Exception {
        // given
        // (1) 기존 데이터 만들어 놓기
        ProductEntity productEntity = new ProductEntity("아메리카노", "시원한 커피, 맛이 있어요", 4000L, 1L);
        ProductEntity savedEntity = productRepository.save(productEntity);

        // (2) DTO
        ProductUpdateRequest req = new ProductUpdateRequest("수정하였습니다.", "수정입니다. 수정이에요.", 20500L);

        // when & then
        mockMvc
            .perform(put("/api/v1/product/" + savedEntity.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));

        // 실제 데이터 검증
        Product updated = productRepository.findByName("수정하였습니다.");
        assertThat(updated.price()).isEqualTo(20500L);
    }

    @Test
    @DisplayName("상품 단건 조회 통합 테스트")
    void productFindSomeone_integration() throws Exception {
        // given
        ProductEntity productEntity = new ProductEntity("아메리카노", "시원한 커피, 맛이 있어요", 4000L, 1L);
        ProductEntity savedEntity = productRepository.save(productEntity);

        // when & then
        mockMvc.perform(get("/api/v1/product/" + savedEntity.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("아메리카노"))
            .andExpect(jsonPath("$.data.price").value(4000L));

        // 실제 데이터 검증
        Product updated = productRepository.findByName("아메리카노");
        assertThat(updated.price()).isEqualTo(4000L);
    }

    @Test
    @DisplayName("상품 삭제 통합 테스트")
    void productDelete_integration() throws Exception {
        // given
        ProductEntity productEntity = new ProductEntity("아메리카노", "시원한 커피, 맛이 있어요", 4000L, 1L);
        ProductEntity savedEntity = productRepository.save(productEntity);

        // when & then
        mockMvc
            .perform(delete("/api/v1/product/" + savedEntity.getId()).contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));

        // 실제 데이터 검증
        assertThat(productRepository.findAll()).hasSize(0);
    }

}