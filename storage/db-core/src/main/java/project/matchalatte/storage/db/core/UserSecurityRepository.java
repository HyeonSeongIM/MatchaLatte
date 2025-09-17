package project.matchalatte.storage.db.core;

import org.springframework.stereotype.Repository;
import project.matchalatte.infra.security.CustomUserDetails;
import project.matchalatte.infra.security.UserDetailsRepository;

@Repository
public class UserSecurityRepository implements UserDetailsRepository {

    private final UserJpaRepository userJpaRepository;

    public UserSecurityRepository(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public CustomUserDetails findByUsernameEntity(String username) {
        UserEntity user = userJpaRepository.findByUsername(username).get();

        return new CustomUserDetails(user.getId(), user.getUsername(), user.getPassword());
    }

}
