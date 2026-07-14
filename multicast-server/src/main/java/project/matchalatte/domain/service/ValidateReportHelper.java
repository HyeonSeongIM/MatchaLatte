package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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

    public Optional<String> findLatestRunAt() {
        List<String> results = jdbcTemplate.queryForList(
            "SELECT run_at FROM validate_report ORDER BY run_at DESC LIMIT 1",
            String.class
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Long> findMissingProductIds(String runAt) {
        return jdbcTemplate.queryForList(
            "SELECT product_id FROM validate_report WHERE issue_type = 'MISSING' AND run_at = ?",
            Long.class, runAt
        );
    }

    public int countByIssueType(String issueType, String runAt) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM validate_report WHERE issue_type = ? AND run_at = ?",
            Integer.class, issueType, runAt
        );
        return count != null ? count : 0;
    }

    public void insertGhostCheckInterrupted(String runAt) {
        jdbcTemplate.update(INSERT_SQL, runAt, "SKIPPED", null, "ghost_check_interrupted");
    }
}
