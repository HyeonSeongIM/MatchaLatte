package project.matchalatte.core.api.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import project.matchalatte.core.api.controller.v1.request.SignUpRequest;
import project.matchalatte.core.domain.user.User;
import project.matchalatte.core.domain.user.UserSecurityService;
import project.matchalatte.core.domain.user.UserService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Tag("restdocs")
@WithMockUser
@WebMvcTest(UserController.class)
@ExtendWith({ RestDocumentationExtension.class, SpringExtension.class })
class UserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserSecurityService userSecurityService;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(documentationConfiguration(restDocumentation)) // ⬅️ 핵심! MockMvc에 REST
                                                                  // Docs 설정을 적용합니다.
            .addFilters(new CharacterEncodingFilter("UTF-8", true)) // ⬅️ 한글 깨짐 방지를 위한 필터
                                                                    // 추가
            .alwaysDo(print()) // ⬅️ 모든 요청/응답을 항상 콘솔에 출력 (디버깅에 유용)
            .build();
    }

    @Test
    @DisplayName("회원가입 API 문서화 테스트")
    void signUpApiTest() throws Exception {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("Hyeonseong", "1234", "Dev0");
        String requestJson = objectMapper.writeValueAsString(signUpRequest);

        User mockedUser = new User(1L, "Hyeonseong", "1234", "Dev0");

        given(userSecurityService.signUp(anyString(), anyString(), anyString())).willReturn(mockedUser);

        // when & then
        mockMvc.perform(
                post("/api/v1/user/signUp").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(requestJson))
            .andExpect(jsonPath("$.data.nickname").value("Dev0"))
            .andDo(print())
            .andDo(document("user-signUp", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(fieldWithPath("username").description("사용자 아이디"),
                            fieldWithPath("password").description("사용자 비밀번호"),
                            fieldWithPath("nickname").description("사용자 닉네임")),
                    responseFields(fieldWithPath("result").description("API 성공 여부"),
                            fieldWithPath("data.nickname").description("가입된 사용자의 닉네임"),
                            fieldWithPath("error").description("에러 여부"))));

    }

}