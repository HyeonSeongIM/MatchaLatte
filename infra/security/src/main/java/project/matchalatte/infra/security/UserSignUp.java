package project.matchalatte.infra.security;

import org.springframework.stereotype.Component;
import project.matchalatte.storage.db.core.UserSecurityRepository;

@Component
public class UserSignUp {

    private final UserSecurityRepository userRepository;

    public UserSignUp(UserSecurityRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserSecurity signUp(String username, String password, String nickname) {
        UserSecurity userSecurity = UserSecurity.of(username, password, nickname);

        userRepository.save(
                userSecurity.username(),
                userSecurity.password(),
                userSecurity.nickname()
        );

        return userSecurity;
    }
}
