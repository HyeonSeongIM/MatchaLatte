package project.matchalatte.core.domain.user;

import org.springframework.stereotype.Component;
import project.matchalatte.infra.security.JwtTokenProvider;

import java.util.Optional;

@Component
public class UserSignIn {

    private final UserRepository userRepository;

    private final JwtTokenProvider jwtTokenProvider;

    public UserSignIn(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String singIn(String username, String password) {
        Optional<User> user = userRepository.findByUsernameEntity(username);

        if (password.equals(user.get().password())) {
            return jwtTokenProvider.createToken(username);
        }
        else {
            throw new IllegalArgumentException("Wrong username or password");
        }
    }

}
