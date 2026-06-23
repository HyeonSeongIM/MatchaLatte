package project.matchalatte.core.api.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class AsyncConfigTest {

    @Autowired
    @Qualifier("productEventExecutor")
    private ThreadPoolTaskExecutor executor;

    @Test
    void executor_corePoolSize_검증() {
        assertThat(executor.getCorePoolSize()).isEqualTo(5);
    }

    @Test
    void executor_maxPoolSize_검증() {
        assertThat(executor.getMaxPoolSize()).isEqualTo(20);
    }

    @Test
    void executor_queueCapacity_검증() {
        assertThat(executor.getQueueCapacity()).isEqualTo(500);
    }

    @Test
    void executor_keepAliveSeconds_검증() {
        assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
    }

    @Test
    void executor_threadNamePrefix_검증() {
        assertThat(executor.getThreadNamePrefix()).isEqualTo("product-event-");
    }

}
