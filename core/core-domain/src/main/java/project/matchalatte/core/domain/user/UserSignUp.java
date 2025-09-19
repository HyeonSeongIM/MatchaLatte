package project.matchalatte.core.domain.user;

import org.springframework.stereotype.Component;

@Component
public class UserSignUp {

    private final UserRepository userRepository;

    public UserSignUp(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User signUp(String username, String password, String nickname) {
        User user = new User(null, username, password, nickname);

        return userRepository.save(user.username(), user.password(), user.nickname());
    }

}
