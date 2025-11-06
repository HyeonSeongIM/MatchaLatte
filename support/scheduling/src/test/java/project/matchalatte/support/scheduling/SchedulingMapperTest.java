package project.matchalatte.support.scheduling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SchedulingMapperTest {

    @InjectMocks
    private SchedulingMapper schedulingMapper;

    @Test
    @DisplayName("CSV 문자열 변환 성공 케이스")
    void dataToCsv_success() {
        // given
        String name = "투썸 아메리카노 쿠폰";
        String description = "투썸 아메리카노 쿠폰입니다. 디카페인 변경 가능해요.";
        Long price = 3000L;
        String expectedLine = "\"투썸 아메리카노 쿠폰\",\"투썸 아메리카노 쿠폰입니다. 디카페인 변경 가능해요.\",3000";

        // when
        String actualLine = schedulingMapper.dataToCsv(name, description, price);

        // then
        assertThat(actualLine).isEqualTo(expectedLine);
    }

}