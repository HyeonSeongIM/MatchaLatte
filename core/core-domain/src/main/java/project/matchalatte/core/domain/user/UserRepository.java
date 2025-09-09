package project.matchalatte.core.domain.user;

import java.util.Optional;

public interface UserRepository {
    User add(String username, String password, String nickname);

    Optional<User> read(Long id);
}
