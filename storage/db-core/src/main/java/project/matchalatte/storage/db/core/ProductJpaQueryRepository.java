package project.matchalatte.storage.db.core;

import com.querydsl.core.types.dsl.BooleanExpression;
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

    public List<ProductEntity> findProductsNoOffsetNotNull(int limit, Long lastId) {
        QProductEntity productEntity = QProductEntity.productEntity;

        return queryFactory.select(productEntity)
            .from(productEntity)
            .where(productEntity.id.lt(lastId))
            .limit(limit)
            .orderBy(productEntity.id.desc())
            .fetch();
    }

    public List<ProductEntity> findProductsNoOffsetNull(int limit) {
        QProductEntity productEntity = QProductEntity.productEntity;

        return queryFactory.select(productEntity)
            .from(productEntity)
            .limit(limit)
            .orderBy(productEntity.id.desc())
            .fetch();
    }

}
