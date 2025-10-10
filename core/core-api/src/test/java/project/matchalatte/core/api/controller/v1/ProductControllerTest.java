package project.matchalatte.core.api.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import project.matchalatte.core.api.controller.v1.request.ProductCreateRequest;
import project.matchalatte.core.api.controller.v1.request.ProductUpdateRequest;
import project.matchalatte.core.domain.product.Product;
import project.matchalatte.core.domain.product.ProductService;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("contextTest")
@WebMvcTest(ProductController.class)
@WithMockUser
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // WebMvcTest 는 해당 컨트롤러만 빈으로 가져옴
    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상품 생성 API 정상 테스트")
    void productCreate_API() throws Exception {
        // given
        ProductCreateRequest createRequest = new ProductCreateRequest("곰돌이 인형", "애기가 가지고 놀던 곰돌이에요", 35000L);

        Long userId = 1L;

        Product serviceResult = new Product(createRequest.name(), createRequest.description(), createRequest.price(),
                userId);

        given(productService.createProduct(any(), any(), any(), any())).willReturn(serviceResult);

        // when & then
        mockMvc
                .perform(post("/api/v1/product").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("곰돌이 인형"))
                .andExpect(jsonPath("$.data.description").value("애기가 가지고 놀던 곰돌이에요"))
                .andExpect(jsonPath("$.data.price").value(35000L))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.error").value(is(nullValue())));

    }

    @Test
    @DisplayName("상품 단건 읽기 API 정상 테스트")
    void productRead_API() throws Exception {
        // given
        Long productId = 1L;

        Long userId = 2L;

        Product savedProduct = new Product("아이돌 굿즈", "BTS 정국 굿즈에요", 50000L, userId);

        given(productService.readProductById(productId)).willReturn(savedProduct);

        // when & then
        mockMvc
                .perform(get("/api/v1/product/" + productId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedProduct))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("아이돌 굿즈"))
                .andExpect(jsonPath("$.data.description").value("BTS 정국 굿즈에요"))
                .andExpect(jsonPath("$.data.price").value(50000L))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("상품 업데이트 API 정상 테스트")
    void productUpdate_API() throws Exception {
        // given
        Long productId = 1L;

        Long userId = 2L;

        ProductUpdateRequest updateRequest = new ProductUpdateRequest("아이패드", "사용한지 1개월 정도 됐어요.", 60000L);
        Product updatedProduct = new Product(updateRequest.name(), updateRequest.description(), updateRequest.price(),
                userId);

        given(productService.updateProduct(any(), any(), any(), any(), any())).willReturn(updatedProduct);

        // when & then
        mockMvc
                .perform(put("/api/v1/product/" + productId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("아이패드"))
                .andExpect(jsonPath("$.data.description").value("사용한지 1개월 정도 됐어요."))
                .andExpect(jsonPath("$.data.price").value(60000L))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("상품 삭제 API 정상 반환")
    void productDelete_API() throws Exception {
        // given
        Long productId = 1L;
        Long userId = 2L;

        willDoNothing().given(productService).deleteProductById(productId, userId);

        // when & then
        mockMvc.perform(delete("/api/v1/product/" + productId).contentType(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

    }

    @Test
    @DisplayName("특정 유저가 올린 상품들 전체 조회")
    void productListByUserId_API() throws Exception {
        // given
        Long userId = 1L;

        List<Product> productList = List.of(
                new Product("상품 1", "입니다.", 5000L, 1L),
                new Product("상품 2", "입니다.", 6000L, 1L),
                new Product("상품 3", "입니다.", 7000L, 2L)
        );

        given(productService.readProductsByUserId(userId)).willReturn(productList);

        // when & then
        mockMvc
                .perform(get("/api/v1/product/user/" + userId).contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.[0].name").value("상품 1"))
                .andExpect(jsonPath("$.data.[0].description").value("입니다."))
                .andExpect(jsonPath("$.data.[0].price").value(5000L))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

}