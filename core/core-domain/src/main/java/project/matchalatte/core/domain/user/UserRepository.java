package project.matchalatte.core.domain.user;

import java.util.Optional;

public interface UserRepository {
    Long add (String name);

    Optional<User> read(Long id);
}
