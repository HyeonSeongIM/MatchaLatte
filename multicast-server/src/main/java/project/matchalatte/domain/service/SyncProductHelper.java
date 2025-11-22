package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.matchalatte.api.dto.ProductInfo;

import java.util.Queue;

@Component
@Slf4j
public class SyncProductHelper {

    private final Queue<ProductInfo> productQueue;

    public SyncProductHelper(Queue<ProductInfo> productQueue) {
        this.productQueue = productQueue;
    }

    public void enqueue(ProductInfo productInfo) {
        productQueue.add(productInfo);
        log.info("Queue 상품 저장 완료 : {}", productInfo.id());
    }

    public boolean isEmptyQueue() {
        return productQueue.isEmpty();
    }

}
