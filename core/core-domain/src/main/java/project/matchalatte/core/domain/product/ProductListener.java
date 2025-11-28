package project.matchalatte.core.domain.product;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ProductListener {

    private static final String ES_TRANSFER_URL = "http://localhost:8082/api/internal/sync/products";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @EventListener
    @Async
    public void onProductListen(ProductEvent event) {
        if (event.eventType() == EventType.DELETE) {
            sendDeleteEvent(event.id());
            return;
        }
        sendInsertOrUpdateEvent(event);
    }

    private void sendInsertOrUpdateEvent(ProductEvent event) {
        Long productId = event.id();
        try {
            String jsonBody = String.format(
                    "{\"id\":%d, \"name\":\"%s\", \"description\":\"%s\", \"price\":%d, \"userId\":%d, \"eventType\":\"%s\"}",
                    event.id(), event.name(), event.description(), event.price(), event.userId(),
                    event.eventType().name());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ES_TRANSFER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(5))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() == 200) {
                    System.out.println("ES 전송 성공 (INSERT/UPDATE): " + productId);
                }
                else {
                    System.err.println("ES 전송 실패 (Status " + response.statusCode() + "): " + productId);
                }
            }).exceptionally(e -> {
                System.err.println("ES 전송 중 에러 발생: " + e.getMessage());
                return null;
            });

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendDeleteEvent(Long productId) {
        try {
            String jsonBody = String.format("{\"id\":%d, \"eventType\":\"DELETE\"}", productId);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ES_TRANSFER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(5))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() == 200) {
                    System.out.println("ES 전송 성공 (DELETE): " + productId);
                }
                else {
                    System.err.println("ES 전송 실패 (Status " + response.statusCode() + "): " + productId);
                }
            }).exceptionally(e -> {
                System.err.println("ES 전송 중 에러 발생: " + e.getMessage());
                return null;
            });

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}