package project.matchalatte.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import project.matchalatte.presentation.ProductInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class ProductBulkService {

    private final Queue<ProductInfo> buffer = new ConcurrentLinkedQueue<>();

    private static final int MAX_BATCH_SIZE = 500;

    public void add(ProductInfo productInfo) {
        buffer.add(productInfo);

        if (buffer.size() >= MAX_BATCH_SIZE) {
            flush();
        }
    }

    @Scheduled(fixedRate = 10000)
    public void scheduleFlush() {
        if (!buffer.isEmpty()) {
            flush();
        }
    }

    private void flush() {
        List<ProductInfo> batch = new ArrayList<>();

        while (!buffer.isEmpty() && batch.size() < MAX_BATCH_SIZE) {
            batch.add(buffer.poll());
        }

        if (batch.isEmpty())
            return;

        try {

            // TODO: 여기서 ES Bulk API 호출

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
