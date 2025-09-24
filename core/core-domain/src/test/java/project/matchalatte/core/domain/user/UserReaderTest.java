package project.matchalatte.core.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

class UserReaderTest {

    @Nested
    @DisplayName("UserReader 단위 테스트")
    @ExtendWith(MockitoExtension.class)
    class UnitTests {
        @InjectMocks
        private UserReader userReader;

        @Mock
        private UserRepository userRepository;

        @Test
        void some_unit_test() {

        }
    }

    @Nested
    @DisplayName("UserReader 통합 테스트")
    @SpringBootTest
    class IntegrationTests {
        @Autowired
        private UserReader userReader;

        @Autowired
        private UserRepository userRepository;

        @Test
        void some_integration_test() {

        }
    }
}