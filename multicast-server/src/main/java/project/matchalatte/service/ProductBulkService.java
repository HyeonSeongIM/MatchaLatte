package project.matchalatte.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import project.matchalatte.dto.ProductInfo;
import project.matchalatte.entity.ProductDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
public class ProductBulkService {

    private final Queue<ProductInfo> buffer = new ConcurrentLinkedQueue<>();

    private final ElasticsearchClient elasticsearchClient;

    public ProductBulkService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public void add(ProductInfo productInfo) {
        buffer.add(productInfo);
        log.info("Queue 상품 저장 완료 : {}", productInfo.id());
    }

    @Scheduled(fixedRate = 10000)
    public void scheduleFlush() {
        if (!buffer.isEmpty()) {
            flush();
        }
    }

    private void flush() {
        List<ProductInfo> batch = new ArrayList<>();

        while (!buffer.isEmpty()) {
            batch.add(buffer.poll());
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
