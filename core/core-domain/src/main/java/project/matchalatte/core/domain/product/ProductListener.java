package project.matchalatte.core.domain.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import project.matchalatte.support.logging.LogData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ProductListener {

    private final Logger log = LoggerFactory.getLogger(ProductListener.class);

    private static final String ES_TRANSFER_URL = "http://localhost:8082/api/internal/sync/products";

    private final HttpClient httpClient;

    public ProductListener(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @EventListener
    @Async
    public void onProductListen(ProductEvent event) {
        try {
            HttpRequest request = createHttpRequest(event);
            log.info("{}", LogData.of("상품 데이터 가공 로직", "상품 데이터 가공 성공 : " + event.id()));

            sendData(request, event.id());
            log.info("{}", LogData.of("상품 데이터 전송 로직", "상품 데이터 전송 성공 : " + event.id()));
        }
        catch (Exception e) {
            log.error("{}", LogData.of("상품 이벤트 처리 로직", event.id() + " : " + e.getMessage()));
        }
    }

    private HttpRequest createHttpRequest(ProductEvent event) {
        String jsonBody = (event.eventType() == EventType.DELETE) ? toDeleteJson(event.id()) : toInsertJson(event);

        return HttpRequest.newBuilder()
            .uri(URI.create(ES_TRANSFER_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(5))
            .build();
    }

    private void sendData(HttpRequest httpRequest, Long productId) {
        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() == 200) {
                log.info("{}", LogData.of("상품 데이터 전송 로직", "상품 데이터 전송 성공 ID: " + productId));
            }
            else {
                // 특이한 상황
                log.error("{}", LogData.of("상품 데이터 전송 로직", "상품 데이터 전송 실패 ID: {}" + productId));
            }
        }).exceptionally(e -> {
            // 서버가 꺼져있거나 응답시간이 너무 길 때
            log.error("{}",
                    LogData.of("상품 데이터 전송 로직", "상품 데이터 전송 실패 ID: " + productId + ", Message: " + e.getMessage()));
            return null;
        });
    }

    private String toInsertJson(ProductEvent event) {
        return String.format("{\"id\":%d, \"name\":\"%s\", \"price\":%d, \"userId\":%d, \"eventType\":\"%s\"}",
                event.id(), event.name(), event.price(), event.userId(), event.eventType().name());
    }

    private String toDeleteJson(Long productId) {
        return String.format("{\"id\":%d, \"eventType\":\"DELETE\"}", productId);
    }

}