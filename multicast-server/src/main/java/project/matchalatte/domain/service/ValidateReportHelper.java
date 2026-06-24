package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ValidateReportHelper {

    private static final String INSERT_SQL =
        "INSERT INTO validate_report (run_at, issue_type, product_id, detail) VALUES (?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public ValidateReportHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertMissing(long productId, String runAt) {
        jdbcTemplate.update(INSERT_SQL, runAt, "MISSING", productId, null);
    }

    public void insertGhost(long productId, String runAt) {
        jdbcTemplate.update(INSERT_SQL, runAt, "GHOST", productId, null);
    }

    public void insertSkipped(long chunkStart, long chunkEnd, String runAt) {
        String detail = "chunk_start=" + chunkStart + ", chunk_end=" + chunkEnd;
        jdbcTemplate.update(INSERT_SQL, runAt, "SKIPPED", null, detail);
    }

    public List<Long> findMissingProductIds() {
        return jdbcTemplate.queryForList(
            "SELECT product_id FROM validate_report WHERE issue_type = 'MISSING'",
            Long.class
        );
    }

    public int countByIssueType(String issueType) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM validate_report WHERE issue_type = ?",
            Integer.class, issueType
        );
        return count != null ? count : 0;
    }
}
