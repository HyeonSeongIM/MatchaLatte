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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

        Product serviceResult = new Product(createRequest.name(), createRequest.description(), createRequest.price());

        given(productService.createProduct(any(), any(), any())).willReturn(serviceResult);

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
            .andExpect(jsonPath("$.error").value(is(nullValue())));

    }

    @Test
    @DisplayName("상품 단건 읽기 API 정상 테스트")
    void productRead_API() throws Exception {
        // given
        Long productId = 1L;

        Product savedProduct = new Product("아이돌 굿즈", "BTS 정국 굿즈에요", 50000L);

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

        ProductUpdateRequest updateRequest = new ProductUpdateRequest("아이패드", "사용한지 1개월 정도 됐어요.", 60000L);
        Product updatedProduct = new Product(updateRequest.name(), updateRequest.description(), updateRequest.price());

        given(productService.updateProduct(productId, updateRequest.name(), updateRequest.description(),
                updateRequest.price()))
            .willReturn(updatedProduct);

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
            .andExpect(jsonPath("$.error").value(nullValue()));
    }

}