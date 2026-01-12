package project.matchalatte.support.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class QueueMetricMonitoring {

    private final MeterRegistry meterRegistry;

    // 메트릭
    private final Counter enqueueCounter;

    private final Counter errorCounter;

    private final AtomicInteger queueSize;

    public QueueMetricMonitoring(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.queueSize = new AtomicInteger(0);

        // 카운터 메트릭 등록
        this.enqueueCounter = Counter.builder("product_queue_enqueue_total")
            .description("Total number of products enqueued")
            .tag("queue", "product_sync")
            .register(meterRegistry);

        this.errorCounter = Counter.builder("product_queue_error_total")
            .description("Total number of queue errors")
            .tag("queue", "product_sync")
            .register(meterRegistry);

        Gauge.builder("product_queue_size", queueSize, AtomicInteger::get)
            .description("Current size of product queue")
            .tag("queue", "product_sync")
            .register(meterRegistry);

        log.info("Queue metrics initialized");
    }

    // Enqueue 성공 기록
    public void recordEnqueue() {
        queueSize.incrementAndGet();
        enqueueCounter.increment();
    }

    // 에러 기록
    public void recordError() {
        errorCounter.increment();
    }

    // 현재 큐 사이즈 조회
    public int getCurrentQueueSize() {
        return queueSize.get();
    }

}