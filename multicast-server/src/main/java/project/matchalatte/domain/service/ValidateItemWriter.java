package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@Slf4j
public class ValidateItemWriter implements ItemWriter<Long> {

    private final ElasticsearchClient elasticsearchClient;
    private final ValidateReportHelper reportHelper;
    private final String runAt;

    public ValidateItemWriter(ElasticsearchClient elasticsearchClient,
                              ValidateReportHelper reportHelper,
                              String runAt) {
        this.elasticsearchClient = elasticsearchClient;
        this.reportHelper = reportHelper;
        this.runAt = runAt;
    }

    @Override
    public void write(Chunk<? extends Long> chunk) {
        List<Long> ids = (List<Long>) chunk.getItems();
        List<String> idStrings = ids.stream().map(String::valueOf).toList();

        try {
            MgetResponse<Void> response = elasticsearchClient.mget(
                m -> m.index("products").ids(idStrings),
                Void.class
            );

            for (MultiGetResponseItem<Void> item : response.docs()) {
                if (item.isFailure()) {
                    log.warn("mget 개별 항목 오류 발생 — 해당 항목 스킵");
                    continue;
                }
                if (!item.result().found()) {
                    long missingId = Long.parseLong(item.result().id());
                    log.warn("MISSING 탐지: product_id={}", missingId);
                    reportHelper.insertMissing(missingId, runAt);
                }
            }
        } catch (Exception e) {
            long chunkStart = ids.get(0);
            long chunkEnd = ids.get(ids.size() - 1);
            log.error("mget 실패 — SKIPPED 기록: chunk_start={}, chunk_end={}", chunkStart, chunkEnd, e);
            reportHelper.insertSkipped(chunkStart, chunkEnd, runAt);
        }
    }
}
