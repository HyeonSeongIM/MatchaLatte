package project.matchalatte.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import project.matchalatte.core.api.controller.IntegrationTestSupport;
import project.matchalatte.core.domain.product.EventType;
import project.matchalatte.core.domain.product.ProductEvent;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class ProductListenerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationEventPublisher publisher;

    @MockBean
    private HttpClient httpClient;

    @Test
    @DisplayName("ProductListener는 product-event- 스레드에서 실행된다")
    @SuppressWarnings("unchecked")
    void onProductListen_productEventThread에서_실행() throws Exception {
        // given
        CompletableFuture<String> capturedThreadName = new CompletableFuture<>();

        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(mockResponse.statusCode()).thenReturn(200);

        CompletableFuture<HttpResponse<String>> mockFuture = new CompletableFuture<>();
        mockFuture.complete(mockResponse);

        Mockito.when(httpClient.sendAsync(any(), any())).thenAnswer(invocation -> {
            capturedThreadName.complete(Thread.currentThread().getName());
            return mockFuture;
        });

        ProductEvent event = new ProductEvent(EventType.CREATE, 1L, "테스트 상품", "설명", 10000L, 1L, "test-trace-id");

        // when
        publisher.publishEvent(event);

        // then
        String threadName = capturedThreadName.get(5, TimeUnit.SECONDS);
        assertThat(threadName).startsWith("product-event-");
        Mockito.verify(httpClient, Mockito.timeout(5000)).sendAsync(any(), any());
    }

}
