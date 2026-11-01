package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import project.matchalatte.api.dto.EventType;
import project.matchalatte.api.dto.ProductEvent;
import project.matchalatte.domain.entity.ProductDocument;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SyncScheduleHelper {

    private static final int MAX_RETRY = 2;

    private static final long[] BACKOFF_MS = { 300, 800 };

    private static final DateTimeFormatter RUN_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Queue<ProductEvent> productQueue;

    private final ElasticsearchClient elasticsearchClient;

    private final ValidateReportHelper reportHelper;

    public SyncScheduleHelper(@Qualifier("apiProductQueue") Queue<ProductEvent> productQueue,
            ElasticsearchClient elasticsearchClient, ValidateReportHelper reportHelper) {
        this.productQueue = productQueue;
        this.elasticsearchClient = elasticsearchClient;
        this.reportHelper = reportHelper;
    }

    public void scheduleFlush() {
        if (!productQueue.isEmpty()) {
            flush();
        }
    }

    private void flush() {
        List<ProductEvent> batch = drainQueue();
        if (batch.isEmpty()) {
            return;
        }

        log.info("스케줄링 시작: {}건", batch.size());

        List<ProductEvent> remaining = batch;
        for (int attempt = 0; attempt <= MAX_RETRY && !remaining.isEmpty(); attempt++) {
            if (attempt > 0) {
                sleep(BACKOFF_MS[attempt - 1]);
                log.warn("flush 재시도 {}/{} — 대상 {}건", attempt, MAX_RETRY, remaining.size());
            }
            remaining = tryBulk(remaining);
        }

        if (!remaining.isEmpty()) {
            recordUnrecovered(remaining);
        }
    }

    private List<ProductEvent> drainQueue() {
        List<ProductEvent> batch = new ArrayList<>();
        while (!productQueue.isEmpty()) {
            batch.add(productQueue.poll());
        }
        return batch;
    }

    // 벌크 색인/삭제를 시도하고, 실패한(부분 실패 또는 전체 예외) 이벤트만 반환한다.
    private List<ProductEvent> tryBulk(List<ProductEvent> events) {
        String indexName = "products";
        Map<String, ProductEvent> byId = events.stream()
            .collect(Collectors.toMap(event -> event.id().toString(), event -> event, (a, b) -> b));

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (ProductEvent event : events) {
                if (event.type() == EventType.DELETE) {
                    br.operations(op -> op.delete(del -> del.index(indexName).id(event.id().toString())));
                    log.info("삭제 오퍼레이션 등록: Product ID {}", event.id());
                }
                else {
                    ProductDocument doc = ProductDocument.from(event);
                    br.operations(op -> op.index(idx -> idx.index(indexName).id(doc.getId().toString()).document(doc)));
                    log.info("색인 오퍼레이션 등록: Product ID {}", doc.getId());
                }
            }

            BulkResponse result = elasticsearchClient.bulk(br.build());

            if (!result.errors()) {
                log.info("스케줄링 완료: {}건, {}ms", events.size(), result.took());
                return List.of();
            }

            List<ProductEvent> failed = new ArrayList<>();
            result.items().forEach(item -> {
                if (item.error() != null) {
                    if (item.status() == 429) {
                        log.error("Bulk 아이템 실패 (429 Too Many Requests — ES write 풀 포화) ID: {}, reason: {}", item.id(),
                                item.error().reason());
                    }
                    else {
                        log.error("Bulk 아이템 실패 — ID: {}, status: {}, reason: {}", item.id(), item.status(),
                                item.error().reason());
                    }
                    ProductEvent failedEvent = byId.get(item.id());
                    if (failedEvent != null) {
                        failed.add(failedEvent);
                    }
                }
            });
            return failed;

        }
        catch (ElasticsearchException e) {
            List<Long> ids = events.stream().map(ProductEvent::id).collect(Collectors.toList());
            if (e.status() == 404) {
                log.error("[CRITICAL] 'products' alias 없음 — {}건 재시도 대상. ID: {}", events.size(), ids);
            }
            else {
                log.error("스케줄링 Bulk 요청 실패 — status: {}, {}건 재시도 대상. ID: {}", e.status(), events.size(), ids, e);
            }
            return events;
        }
        catch (Exception e) {
            log.error("스케줄링 Bulk 요청 실패 — {}건 재시도 대상", events.size(), e);
            return events;
        }
    }

    // 재시도를 모두 소진한 이벤트를 validate_report에 즉시 기록해 /repair로 바로 복구할 수 있게 한다.
    private void recordUnrecovered(List<ProductEvent> events) {
        String runAt = LocalDateTime.now().format(RUN_AT_FORMAT);

        for (ProductEvent event : events) {
            if (event.type() == EventType.DELETE) {
                reportHelper.insertGhost(event.id(), runAt);
            }
            else {
                reportHelper.insertMissing(event.id(), runAt);
            }
        }

        List<Long> ids = events.stream().map(ProductEvent::id).collect(Collectors.toList());
        log.error("[CRITICAL] flush 재시도 소진 — {}건 validate_report 기록(run_at={}), ID: {}. "
                + "CREATE/UPDATE는 /repair로 자동 복구 가능, DELETE(GHOST)는 수동 확인 필요.", events.size(), runAt, ids);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
