package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class GhostCheckTasklet implements Tasklet {

    private final ElasticsearchClient elasticsearchClient;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final ValidateReportHelper reportHelper;
    private final String runAt;

    public GhostCheckTasklet(ElasticsearchClient elasticsearchClient,
                              NamedParameterJdbcTemplate namedJdbc,
                              ValidateReportHelper reportHelper,
                              String runAt) {
        this.elasticsearchClient = elasticsearchClient;
        this.namedJdbc = namedJdbc;
        this.reportHelper = reportHelper;
        this.runAt = runAt;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        OpenPointInTimeResponse pitOpen;
        try {
            pitOpen = elasticsearchClient.openPointInTime(
                p -> p.index("products").keepAlive(t -> t.time("5m"))
            );
        } catch (Exception e) {
            log.error("PIT open 실패 — ghostCheckStep 건너뜀", e);
            return RepeatStatus.FINISHED;
        }

        String pitId = pitOpen.id();
        try {
            List<FieldValue> searchAfter = null;
            while (true) {
                final String currentPitId = pitId;
                final List<FieldValue> currentSearchAfter = searchAfter;

                SearchResponse<Void> response = elasticsearchClient.search(s -> {
                    var builder = s
                        .pit(p -> p.id(currentPitId).keepAlive(t -> t.time("5m")))
                        .sort(so -> so.field(f -> f.field("_id").order(SortOrder.Asc)))
                        .size(1000)
                        .source(src -> src.fetch(false));
                    if (currentSearchAfter != null) {
                        builder.searchAfter(currentSearchAfter);
                    }
                    return builder;
                }, Void.class);

                pitId = response.pitId(); // 매 응답마다 갱신된 pit_id 이어받기

                List<Hit<Void>> hits = response.hits().hits();
                if (hits.isEmpty()) break;

                List<Long> esIds = hits.stream()
                    .map(h -> Long.parseLong(h.id()))
                    .toList();

                List<Long> existingInRdb = namedJdbc.queryForList(
                    "SELECT id FROM product WHERE id IN (:ids)",
                    new MapSqlParameterSource("ids", esIds),
                    Long.class
                );
                Set<Long> existingSet = new HashSet<>(existingInRdb);

                for (Long esId : esIds) {
                    if (!existingSet.contains(esId)) {
                        log.warn("GHOST 탐지: product_id={}", esId);
                        reportHelper.insertGhost(esId, runAt);
                    }
                }

                searchAfter = hits.get(hits.size() - 1).sort();
                if (hits.size() < 1000) break;
            }
        } catch (Exception e) {
            log.error("ghostCheckStep 루프 중 오류 — 이후 범위 미검증", e);
            reportHelper.insertGhostCheckInterrupted(runAt);
        } finally {
            final String finalPitId = pitId;
            try {
                elasticsearchClient.closePointInTime(p -> p.id(finalPitId));
                log.info("PIT close 완료");
            } catch (Exception e) {
                log.error("PIT close 실패: pit_id={}", finalPitId, e);
            }
        }
        return RepeatStatus.FINISHED;
    }
}
