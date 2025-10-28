package project.matchalatte.storage.db.core;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    @Query("SELECT p FROM ProductEntity p WHERE p.userId = :userId ORDER BY p.id DESC ")
    List<ProductEntity> findByUserId(@Param("userId") Long userId);

    Slice<ProductEntity> findAllBy(Pageable pageable);

    @Query("SELECT p FROM ProductEntity p WHERE p.name LIKE CONCAT('%', :keyword, '%')")
    Slice<ProductEntity> findProductsByKeyword(String keyword, Pageable pageable);

}
