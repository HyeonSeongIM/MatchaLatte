package project.matchalatte.core.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.matchalatte.core.support.error.CoreException;
import project.matchalatte.core.support.error.ErrorType;
import project.matchalatte.core.support.error.UserException;

import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WithMockUser
class ApiControllerAdviceTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // 1. 테스트용 컨트롤러와 우리가 테스트할 Advice를 연결해서 MockMvc를 만듦
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new ApiControllerAdvice())
            .build();
    }

    @Test
    @DisplayName("CoreException 발생 시 Advice가 처리한다")
    void handleCoreException() throws Exception {
        mockMvc.perform(get("/test/core-exception"))
            .andExpect(status().is5xxServerError()) // DEFAULT_ERROR가 500이라면
            .andExpect(jsonPath("$.result").value("ERROR")) // ApiResponse 구조에 따라 수정 필요
            .andDo(print());
    }

    @Test
    @DisplayName("UserException (Legacy) 발생 시 Advice가 처리한다")
    void handleUserException() throws Exception {
        mockMvc.perform(get("/test/user-exception")).andExpect(status().is5xxServerError()).andDo(print());
    }

//    @Test
//    @DisplayName("NoSuchElementException 발생 시 404가 아닌 정의된 에러로 처리한다")
//    void handleNoSuchElement() throws Exception {
//        // 네 코드에서 NoSuchElement는 DEFAULT_ERROR를 리턴하므로 그 상태코드를 따라감
//        mockMvc.perform(get("/test/no-such-element"))
//                .andExpect(jsonPath("$.data").value("리소스를 찾을 수 없습니다."))
//            .andDo(print());
//    }
//
//    @Test
//    @DisplayName("BindException (유효성 검사 실패) 발생 시 400 Bad Request를 반환한다")
//    void handleBindException() throws Exception {
//        // 유효하지 않은 요청 바디 (name이 null)
//        TestDto invalidDto = new TestDto(null);
//
//        mockMvc
//            .perform(post("/test/bind-exception").contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(invalidDto)))
//            .andExpect(status().isBadRequest()) // HttpStatus.BAD_REQUEST
//                .andExpect(jsonPath("$.data").value("이름은 필수입니다."))
//            .andDo(print());
//    }

    @Test
    @DisplayName("알 수 없는 예외(Exception) 발생 시 500 에러로 처리한다")
    void handleException() throws Exception {
        mockMvc.perform(get("/test/exception"))
            .andExpect(status().is5xxServerError()) // DEFAULT_ERROR 상태 코드
            .andDo(print());
    }

    /**
     * 테스트를 위해 예외를 일부러 던지는 가짜 컨트롤러
     */
    @RestController
    static class TestController {

        @GetMapping("/test/core-exception")
        public void throwCoreException() {
            // 테스트를 위해 임의의 ErrorType 사용 (네 프로젝트의 실제 ErrorType으로 변경 가능)
            throw new CoreException(ErrorType.DEFAULT_ERROR, "코어 에러 데이터");
        }

        @GetMapping("/test/user-exception")
        public void throwUserException() {
            throw new UserException(ErrorType.DEFAULT_ERROR, "유저 에러 데이터");
        }

        @GetMapping("/test/no-such-element")
        public void throwNoSuchElement() {
            throw new NoSuchElementException("없어요");
        }

        @PostMapping("/test/bind-exception")
        public void throwBindException(@RequestBody @Valid TestDto dto) {
            // @Valid가 붙어있어서 DTO 검증 실패 시 BindException 자동 발생
        }

        @GetMapping("/test/exception")
        public void throwException() {
            throw new RuntimeException("알 수 없는 런타임 에러");
        }

    }

    /**
     * BindException 테스트를 위한 DTO
     */
    static class TestDto {

        @NotNull(message = "이름은 필수입니다.")
        private String name;

        public TestDto() {
        }

        public TestDto(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

}