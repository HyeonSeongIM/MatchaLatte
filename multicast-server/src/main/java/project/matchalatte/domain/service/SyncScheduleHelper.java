package project.matchalatte.domain.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.matchalatte.api.dto.ProductInfo;
import project.matchalatte.domain.entity.ProductDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Component
@Slf4j
public class SyncScheduleHelper {

    private final Queue<ProductInfo> productQueue;

    private final ElasticsearchClient elasticsearchClient;

    public SyncScheduleHelper(Queue<ProductInfo> productQueue, ElasticsearchClient elasticsearchClient) {
        this.productQueue = productQueue;
        this.elasticsearchClient = elasticsearchClient;
    }

    public void scheduleFlush() {
        if (!productQueue.isEmpty()) {
            flush();
        }
    }

    private void flush() {
        List<ProductInfo> batch = new ArrayList<>();

        while (!productQueue.isEmpty()) {
            batch.add(productQueue.poll());
        }

        try {
            log.info("스케줄링 시작");
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (ProductInfo info : batch) {
                ProductDocument doc = ProductDocument.from(info);

                log.info("등록될 Product ID : {}", doc.getId());

                br.operations(op -> op.index(idx -> idx.index("products").id(doc.getId().toString()).document(doc)));
            }

            BulkResponse result = elasticsearchClient.bulk(br.build());
            log.info("스케줄링 완료 : {}", result);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
