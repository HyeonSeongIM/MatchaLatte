package project.matchalatte.core.domain.user;

import org.springframework.stereotype.Component;

@Component
public class UserWriter {
    private final UserRepository userRepository;

    public UserWriter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long add(String username){
        return userRepository.add(username);
    }
}
