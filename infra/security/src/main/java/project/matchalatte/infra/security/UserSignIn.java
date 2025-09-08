package project.matchalatte.infra.security;

import org.springframework.stereotype.Component;
import project.matchalatte.storage.db.core.UserEntity;
import project.matchalatte.storage.db.core.UserSecurityRepository;

import java.util.Optional;

@Component
public class UserSignIn {

    private final UserSecurityRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public UserSignIn(UserSecurityRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String singIn(String username, String password) {
        Optional<UserEntity> userSecurity = userRepository.findByUsernameEntity(username);

        if (password.equals(userSecurity.get().getPassword())) {
            return jwtTokenProvider.createToken(username);
        } else {
            throw new IllegalArgumentException("Wrong username or password");
        }
    }
}
