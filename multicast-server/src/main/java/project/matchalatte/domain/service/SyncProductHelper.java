package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.matchalatte.api.dto.ProductEvent;

import java.util.Queue;

@Component
@Slf4j
public class SyncProductHelper {

    private final Queue<ProductEvent> productQueue;

    public SyncProductHelper(Queue<ProductEvent> productQueue) {
        this.productQueue = productQueue;
    }

    public void enqueue(ProductEvent productEvent) {
        productQueue.add(productEvent);
        log.info("Queue 상품 저장 완료 : {}", productEvent.id());
    }

    public boolean isEmptyQueue() {
        return productQueue.isEmpty();
    }

}
