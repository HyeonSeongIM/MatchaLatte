package project.matchalatte.storage.db.core;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductJpaQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ProductJpaQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Long countAllProducts() {
        QProductEntity productEntity = QProductEntity.productEntity;

        Long count = queryFactory.select(productEntity.count()).from(productEntity).fetchOne();

        return count != null ? count : 0L;

    }

    public List<ProductEntity> findProducts(Pageable pageable) {
        QProductEntity productEntity = QProductEntity.productEntity;

        return queryFactory.select(productEntity)
            .from(productEntity)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(productEntity.createdAt.desc(), productEntity.id.desc())
            .fetch();
    }

}
