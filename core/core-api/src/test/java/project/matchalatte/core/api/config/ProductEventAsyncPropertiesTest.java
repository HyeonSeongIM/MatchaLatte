package project.matchalatte.core.api.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class ProductEventAsyncPropertiesTest {

    @Autowired
    private ProductEventAsyncProperties properties;

    @Test
    void yml_바인딩_coreSize() {
        assertThat(properties.getCoreSize()).isEqualTo(5);
    }

    @Test
    void yml_바인딩_maxSize() {
        assertThat(properties.getMaxSize()).isEqualTo(20);
    }

    @Test
    void yml_바인딩_queueCapacity() {
        assertThat(properties.getQueueCapacity()).isEqualTo(500);
    }

    @Test
    void yml_바인딩_keepAliveSeconds() {
        assertThat(properties.getKeepAliveSeconds()).isEqualTo(60);
    }

    @Test
    void yml_바인딩_threadNamePrefix() {
        assertThat(properties.getThreadNamePrefix()).isEqualTo("product-event-");
    }

}
