package project.matchalatte.storage.db.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
}
