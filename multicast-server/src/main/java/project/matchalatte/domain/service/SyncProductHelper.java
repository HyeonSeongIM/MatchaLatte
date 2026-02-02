package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import project.matchalatte.api.dto.ProductEvent;
import project.matchalatte.support.monitoring.QueueMetricMonitoring;

import java.util.Queue;

@Component
@Slf4j
public class SyncProductHelper {

    private final Queue<ProductEvent> productQueue;

    private final QueueMetricMonitoring monitoring;

    public SyncProductHelper(@Qualifier("apiProductQueue") Queue<ProductEvent> productQueue,
            QueueMetricMonitoring monitoring) {
        this.productQueue = productQueue;
        this.monitoring = monitoring;
    }

    public void enqueue(ProductEvent productEvent) {
        try {
            productQueue.add(productEvent);
            monitoring.recordEnqueue();
            log.info("Queue 상품 저장 완료 : {}", productEvent.id());
        }
        catch (Exception e) {
            monitoring.recordError();
            log.error("Queue 상품 저장 실패 : {}", productEvent.id(), e);
            throw e;
        }
    }

    public boolean isEmptyQueue() {
        return productQueue.isEmpty();
    }

}