package project.matchalatte.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import project.matchalatte.core.api.controller.v1.request.SignInRequest;
import project.matchalatte.core.api.controller.v1.request.SignUpRequest;
import project.matchalatte.core.domain.user.User;
import project.matchalatte.core.domain.user.UserRepository;
import project.matchalatte.core.domain.user.UserService;
import project.matchalatte.storage.db.core.UserEntity;
import project.matchalatte.storage.db.core.UserJpaRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        userJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자 회원가입 통합 테스트")
    void userSignUp_integration() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("username1", "password", "첫번째");

        // when & then
        mockMvc
            .perform(post("/api/v1/user/signUp").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));

        Optional<User> user = userRepository.findByUsernameEntity("username1");
        assertThat(user.get().nickname()).isEqualTo("첫번째");
    }

    @Test
    @DisplayName("사용자 회원가입 통합 테스트")
    void userSignIn_integration() throws Exception {
        // given
        // (1) 미리 저장된 사용자 정보 생성
        UserEntity userEntity = new UserEntity("username1", "password", "첫번째");
        User savedEntity = userRepository.save(userEntity.getUsername(), userEntity.getPassword(),
                userEntity.getNickname());

        // (2) DTO
        SignInRequest request = new SignInRequest("username1", "password");

        // when & then
        mockMvc
            .perform(post("/api/v1/user/signIn").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

}
