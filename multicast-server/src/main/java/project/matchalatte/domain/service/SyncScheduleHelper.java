package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.matchalatte.api.dto.EventType;
import project.matchalatte.api.dto.ProductEvent;
import project.matchalatte.domain.entity.ProductDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Component
@Slf4j
public class SyncScheduleHelper {

    private final Queue<ProductEvent> productQueue;

    private final ElasticsearchClient elasticsearchClient;

    public SyncScheduleHelper(Queue<ProductEvent> productQueue, ElasticsearchClient elasticsearchClient) {
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

            for (ProductEvent event : batch) {
                if (event.type() == EventType.DELETE) {
                    br.operations(op -> op.delete(del -> del.index("products").id(event.id().toString())));
                    log.info("삭제 오퍼레이션 등록: Product ID {}", event.id());

                }
                else {
                    ProductDocument doc = ProductDocument.from(event);

                    br.operations(
                            op -> op.index(idx -> idx.index("products").id(doc.getId().toString()).document(doc)));
                    log.info("색인 오퍼레이션 등록: Product ID {}", doc.getId());
                }
            }

            BulkResponse result = elasticsearchClient.bulk(br.build());
            log.info("스케줄링 완료 : {}", result);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
