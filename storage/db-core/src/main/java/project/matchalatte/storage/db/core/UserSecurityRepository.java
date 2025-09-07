package project.matchalatte.storage.db.core;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSecurityRepository extends JpaRepository<UserSecurityEntity, Long> {
    Optional<UserSecurityEntity> findByUsername(String username);
}
