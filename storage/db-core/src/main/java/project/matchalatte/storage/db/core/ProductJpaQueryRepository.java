package project.matchalatte.storage.db.core;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductJpaQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ProductJpaQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    

}
