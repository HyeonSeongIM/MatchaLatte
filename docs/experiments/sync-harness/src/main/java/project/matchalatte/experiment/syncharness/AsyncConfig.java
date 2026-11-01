package project.matchalatte.experiment.syncharness;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// 운영 AsyncConfig(core-api)와 동일하게 이벤트 처리 전용 스레드풀을 분리해, 실제와 유사한 조건으로 재현
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("productEventExecutor")
    public Executor productEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("product-event-");
        executor.initialize();
        return executor;
    }

}
