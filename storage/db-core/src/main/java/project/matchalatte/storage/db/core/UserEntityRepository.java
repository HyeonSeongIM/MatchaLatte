package project.matchalatte.storage.db.core;

import org.springframework.stereotype.Repository;
import project.matchalatte.core.domain.user.User;
import project.matchalatte.core.domain.user.UserRepository;

import java.util.Optional;

@Repository
public class UserEntityRepository implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserEntityRepository(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User add(String username, String password, String nickname) {
        UserEntity userEntity = new UserEntity(username, password, nickname);

        UserEntity savedEntity = userJpaRepository.save(userEntity);

        return new User(savedEntity.getId(), username, password, nickname);
    }

    @Override
    public Optional<User> read(Long id) {
        Optional<UserEntity> userEntity = userJpaRepository.findById(id);

        return userEntity.map(entity -> new User(
                entity.getId(),
                entity.getUsername(),
                entity.getPassword(),
                entity.getNickname())
        );
    }
}
