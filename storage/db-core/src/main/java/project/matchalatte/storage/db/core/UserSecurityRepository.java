package project.matchalatte.storage.db.core;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserSecurityRepository {

    private final UserJpaRepository userJpaRepository;

    public UserSecurityRepository(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    public Optional<UserEntity> findByUsernameEntity(String username) {
        return userJpaRepository.findByUsername(username);
    }

    public UserEntity save(String username, String password, String nickname) {
        UserEntity userEntity = new UserEntity(username, password, nickname);

        userJpaRepository.save(userEntity);

        return new UserEntity(username, password, nickname);
    }
}
