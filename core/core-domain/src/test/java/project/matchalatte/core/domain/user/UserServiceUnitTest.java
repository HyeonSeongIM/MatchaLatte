package project.matchalatte.core.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자 저장 확인")
    void check_save_user() {

    }

    @Test
    @DisplayName("사용자 읽기 확인")
    void check_read_user() {

    }

    @Test
    @DisplayName("사용자 저장 실패 시")
    void check_save_not_user() {

    }

    @Test
    @DisplayName("사용자 읽기 시 존재하지 않을 경우")
    void check_read_not_user() {

    }

}
