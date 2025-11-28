package project.matchalatte.admin.domain;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashBoardService {

    private final DashBoardRepository dashBoardRepository;

    public DashBoardService(DashBoardRepository dashBoardRepository) {
        this.dashBoardRepository = dashBoardRepository;
    }

    // 가입자 수 조회
    public List<DailySignUp> getDailySignUps() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(7);
        DateTime dateTime = new DateTime(startDate, endDate);
        return dashBoardRepository.getDailySignUpCount(dateTime);
    }

    // 가격대별 상품
    public List<PriceRange> getPriceRanges() {
        return dashBoardRepository.getPriceRangeStats();
    }

    // 헤비 유저
    public List<TopSeller> getTopSellers() {
        return dashBoardRepository.getTopSellers();
    }

}
