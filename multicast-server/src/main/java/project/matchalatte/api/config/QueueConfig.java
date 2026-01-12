package project.matchalatte.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import project.matchalatte.api.dto.ProductEvent;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

@Configuration
public class QueueConfig {

    @Value("${queue.product.max-capacity}")
    private int maxCapacity;

    @Bean
    public Queue<ProductEvent> productQueue() {
        // 용량 제한이 있는 큐 사용
        return new ArrayBlockingQueue<>(maxCapacity);
    }

    @Bean
    public int queueMaxCapacity() {
        return maxCapacity;
    }

}
