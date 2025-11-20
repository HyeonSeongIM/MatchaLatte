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
public class ProductCreateListener {

    private static final String ES_TRANSFER_URL = "http://localhost:8082/api/internal/sync/products";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @EventListener
    @Async
    public void onProductCreate(ProductCreateEvent event) {
        Long productId = event.productId();

        try {
            String jsonBody = String.format("{\"productId\":%d}", productId);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ES_TRANSFER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(5))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() == 200) {
                    System.out.println("ES 전송 성공: " + productId);
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