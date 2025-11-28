package project.matchalatte.admin.domain;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DashBoardQueryRepository implements DashBoardRepository {

    private final EntityManager em;

    public DashBoardQueryRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<DailySignUp> getDailySignUpCount(DateTime dateTime) {
        // 1. SQL 작성 (MySQL 문법 기준)
        // created_at을 'YYYY-MM-DD' 형식으로 바꿔서 그룹핑합니다.
        String sql = """
                    SELECT
                        DATE_FORMAT(created_at, '%Y-%m-%d') AS date,
                        COUNT(*) AS count
                    FROM users
                    WHERE created_at BETWEEN :startTime AND :endTime
                    GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
                    ORDER BY date ASC
                """;

        // 2. 쿼리 실행 및 파라미터 바인딩
        // dateTime 객체 안에 있는 startTime()과 endTime()을 꺼내서 넣어줍니다.
        List<Object[]> results = em.createNativeQuery(sql)
            .setParameter("startTime", dateTime.startTime())
            .setParameter("endTime", dateTime.endTime())
            .getResultList();

        // 3. 결과 매핑 (Object[] -> DTO)
        return results.stream()
            .map(row -> new DailySignUp((String) row[0], // 날짜 문자열 (예: "2025-11-14")
                    ((Number) row[1]).longValue() // 카운트 (Long 변환)
            ))
            .collect(Collectors.toList());
    }

    // 1. 가격대별 통계 이사 완료
    @Override
    public List<PriceRange> getPriceRangeStats() {
        // 아까 @Query 안에 있던 SQL을 여기에 문자열로 넣습니다.
        String sql = """
                    SELECT
                        CASE
                            WHEN price < 10000 THEN '1만원 미만'
                            WHEN price BETWEEN 10000 AND 50000 THEN '1만원~5만원'
                            ELSE '5만원 이상'
                        END AS priceRange,
                        COUNT(*) AS totalCount
                    FROM products
                    GROUP BY
                        CASE
                            WHEN price < 10000 THEN '1만원 미만'
                            WHEN price BETWEEN 10000 AND 50000 THEN '1만원~5만원'
                            ELSE '5만원 이상'
                        END
                """;

        // 쿼리 실행 및 매핑
        List<Object[]> results = em.createNativeQuery(sql).getResultList();

        return results.stream()
            .map(row -> new PriceRange((String) row[0], ((Number) row[1]).longValue()))
            .collect(Collectors.toList());
    }

    // 2. 탑 셀러 랭킹 이사 완료
    @Override
    public List<TopSeller> getTopSellers() {
        String sql = """
                    SELECT
                        u.nickname AS nickname,
                        COUNT(p.id) AS productCount,
                        SUM(p.price) AS totalPrice
                    FROM products p
                    INNER JOIN users u ON p.user_id = u.id
                    GROUP BY u.id, u.nickname
                    ORDER BY productCount DESC
                    LIMIT 5
                """;

        List<Object[]> results = em.createNativeQuery(sql).getResultList();

        return results.stream()
            .map(row -> new TopSeller((String) row[0], ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
            .collect(Collectors.toList());
    }

    // ... 나머지 메서드도 동일하게

}
