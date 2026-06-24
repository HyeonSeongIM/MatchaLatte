package project.matchalatte.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateReportHelperTest {

    @Mock JdbcTemplate jdbcTemplate;

    ValidateReportHelper sut;

    @BeforeEach
    void setUp() {
        sut = new ValidateReportHelper(jdbcTemplate);
    }

    @Test
    void insertMissing_정상_insert_호출() {
        sut.insertMissing(42L, "2026-06-24T10:00:00");
        verify(jdbcTemplate).update(
            contains("INSERT INTO validate_report"),
            eq("2026-06-24T10:00:00"), eq("MISSING"), eq(42L), isNull()
        );
    }

    @Test
    void insertGhost_정상_insert_호출() {
        sut.insertGhost(99L, "2026-06-24T10:00:00");
        verify(jdbcTemplate).update(
            contains("INSERT INTO validate_report"),
            eq("2026-06-24T10:00:00"), eq("GHOST"), eq(99L), isNull()
        );
    }

    @Test
    void insertSkipped_detail에_청크_범위_기록() {
        sut.insertSkipped(1001L, 2000L, "2026-06-24T10:00:00");
        verify(jdbcTemplate).update(
            contains("INSERT INTO validate_report"),
            eq("2026-06-24T10:00:00"), eq("SKIPPED"), isNull(),
            eq("chunk_start=1001, chunk_end=2000")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void findMissingProductIds_MISSING_id_목록_반환() {
        when(jdbcTemplate.queryForList(contains("issue_type = 'MISSING'"), eq(Long.class)))
            .thenReturn(List.of(1L, 2L, 3L));

        List<Long> result = sut.findMissingProductIds();

        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    void countByIssueType_건수_반환() {
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Integer.class), eq("MISSING")))
            .thenReturn(5);

        assertThat(sut.countByIssueType("MISSING")).isEqualTo(5);
    }
}
