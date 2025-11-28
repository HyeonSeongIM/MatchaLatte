package project.matchalatte.admin.domain;

import org.springframework.stereotype.Component;

import java.util.List;

public interface DashBoardRepository {

    // 최근 7일(또는 30일)간 날짜별 가입자 수 조회
    List<DailySignUp> getDailySignUpCount(DateTime dateTime);

    // 가격대별 상품 분
    List<PriceRange> getPriceRangeStats();

    // 헤비 유저 랭킹
    List<TopSeller> getTopSellers();

}
