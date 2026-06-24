package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import project.matchalatte.domain.entity.ProductDocument;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class RepairService {

    private final ValidateReportHelper reportHelper;
    private final ElasticsearchClient elasticsearchClient;
    private final JdbcTemplate jdbcTemplate;

    public RepairService(ValidateReportHelper reportHelper,
                         ElasticsearchClient elasticsearchClient,
                         JdbcTemplate jdbcTemplate) {
        this.reportHelper = reportHelper;
        this.elasticsearchClient = elasticsearchClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public String repair() {
        Optional<String> latestRunAt = reportHelper.findLatestRunAt();
        if (latestRunAt.isEmpty()) {
            log.info("validate_report 비어있음 — 검증 실행 필요");
            return "MISSING 항목 없음";
        }

        List<Long> missingIds = reportHelper.findMissingProductIds(latestRunAt.get());
        if (missingIds.isEmpty()) {
            log.info("MISSING 항목 없음 (run_at={}) — 재색인 불필요", latestRunAt.get());
            return "MISSING 항목 없음";
        }

        String placeholders = String.join(",", Collections.nCopies(missingIds.size(), "?"));
        String sql = "SELECT id, name, description, price, user_id FROM product WHERE id IN (" + placeholders + ")";
        List<SyncProductInfo> products = jdbcTemplate.query(
            sql,
            missingIds.toArray(),
            (rs, row) -> new SyncProductInfo(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getLong("price"),
                rs.getLong("user_id")
            )
        );

        if (products.isEmpty()) {
            log.warn("MISSING id가 RDB에도 없음 — 이미 삭제된 데이터일 수 있음");
            return "RDB에서 대상 상품 조회 결과 없음";
        }

        try {
            List<ProductDocument> docs = products.stream()
                .map(ProductDocument::from)
                .toList();

            BulkResponse result = elasticsearchClient.bulk(br -> {
                for (ProductDocument doc : docs) {
                    br.operations(op -> op.index(idx ->
                        idx.index("products").id(doc.getId().toString()).document(doc)
                    ));
                }
                return br;
            });

            if (result.errors()) {
                result.items().forEach(item -> {
                    if (item.error() != null) {
                        log.error("재색인 실패 ID={}: {}", item.id(), item.error().reason());
                    }
                });
                return "재색인 일부 실패 — 로그 확인 필요";
            }

            log.info("재색인 완료: {}건, {}ms", products.size(), result.took());
            return "재색인 완료: " + products.size() + "건";
        } catch (Exception e) {
            log.error("재색인 중 오류", e);
            return "재색인 오류: " + e.getMessage();
        }
    }
}
