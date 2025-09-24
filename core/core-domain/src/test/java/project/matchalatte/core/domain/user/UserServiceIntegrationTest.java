package project.matchalatte.core.domain.user;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserService.class)
@ExtendWith(MockitoExtension.class)
class UserServiceIntegrationTest {

    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자 저장 확인")
    void check_save_user() {

    }

    @Test
    @DisplayName("사용자 저장 실패 시")
    void check_save_not_user() {

    }

}
