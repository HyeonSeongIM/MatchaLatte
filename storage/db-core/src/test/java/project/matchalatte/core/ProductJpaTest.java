package project.matchalatte.core;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import project.matchalatte.storage.db.core.ProductEntity;
import project.matchalatte.storage.db.core.ProductJpaRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductJpaTest extends CoreDbContextTest {

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    ProductJpaTest(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Test
    @DisplayName("특정 유저 ID로 등록된 상품 목록을 조회한다")
    void readByUserId() {
        // given
        Long A_userId = 1L;
        Long B_userId = 2L;

        productJpaRepository.save(new ProductEntity("상품1", "상품1입니다.", 5000L, A_userId));
        productJpaRepository.save(new ProductEntity("상품2", "상품2입니다.", 5000L, B_userId));
        productJpaRepository.save(new ProductEntity("상품3", "상품3입니다.", 5000L, B_userId));

        entityManager.flush();
        entityManager.clear();

        // when
        List<ProductEntity> result = productJpaRepository.findByUserId(B_userId);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getUserId()).isEqualTo(B_userId);
        assertThat(result.get(1).getName()).isEqualTo("상품2");
    }
}
