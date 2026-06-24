CREATE TABLE IF NOT EXISTS validate_report (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    run_at     VARCHAR(20)  NOT NULL,
    issue_type ENUM('MISSING', 'GHOST', 'SKIPPED') NOT NULL,
    product_id BIGINT       NULL,
    detail     VARCHAR(255) NULL,
    PRIMARY KEY (id),
    INDEX idx_issue_type (issue_type),
    INDEX idx_run_at (run_at)
);
