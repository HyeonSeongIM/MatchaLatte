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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SyncScheduleHelper {

    private final Queue<ProductEvent> productQueue;

    private final ElasticsearchClient elasticsearchClient;

    public SyncScheduleHelper(@Qualifier("apiProductQueue") Queue<ProductEvent> productQueue,
            ElasticsearchClient elasticsearchClient) {
        this.productQueue = productQueue;
        this.elasticsearchClient = elasticsearchClient;
    }

    public void scheduleFlush() {
        if (!productQueue.isEmpty()) {
            flush();
        }
    }

    private void flush() {
        List<ProductEvent> batch = new ArrayList<>();

        while (!productQueue.isEmpty()) {
            batch.add(productQueue.poll());
        }

        try {
            log.info("스케줄링 시작");
            BulkRequest.Builder br = new BulkRequest.Builder();

            String indexName = "products";

            for (ProductEvent event : batch) {
                if (event.type() == EventType.DELETE) {
                    br.operations(op -> op.delete(del -> del.index(indexName).id(event.id().toString())));
                    log.info("삭제 오퍼레이션 등록: Product ID {}", event.id());
                } else {
                    ProductDocument doc = ProductDocument.from(event);
                    br.operations(
                        op -> op.index(idx -> idx.index(indexName).id(doc.getId().toString()).document(doc)));
                    log.info("색인 오퍼레이션 등록: Product ID {}", doc.getId());
                }
            }

            BulkResponse result = elasticsearchClient.bulk(br.build());

            if (result.errors()) {
                result.items().forEach(item -> {
                    if (item.error() != null) {
                        if (item.status() == 429) {
                            log.error("Bulk 아이템 실패 (429 Too Many Requests — ES write 풀 포화) ID: {}, reason: {}",
                                item.id(), item.error().reason());
                        } else {
                            log.error("Bulk 아이템 실패 — ID: {}, status: {}, reason: {}",
                                item.id(), item.status(), item.error().reason());
                        }
                    }
                });
            } else {
                log.info("스케줄링 완료: {}건, {}ms", batch.size(), result.took());
            }

        } catch (ElasticsearchException e) {
            if (e.status() == 404) {
                List<Long> lostIds = batch.stream()
                    .map(ProductEvent::id)
                    .collect(Collectors.toList());
                log.error("[CRITICAL] 'products' alias 없음 — {}건 유실. 유실 ID: {}. " +
                    "validateSyncJob 실행 후 /repair로 보정 필요.", batch.size(), lostIds);
            } else {
                log.error("스케줄링 Bulk 요청 실패 — status: {}", e.status(), e);
            }
        } catch (Exception e) {
            log.error("스케줄링 Bulk 요청 실패", e);
        }
    }

}
