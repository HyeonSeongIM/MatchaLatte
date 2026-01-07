package project.matchalatte.core.domain.product;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import project.matchalatte.support.logging.TraceIdContext;

@Component
public class ProductEventer {

    private final ApplicationEventPublisher applicationEventPublisher;

    public ProductEventer(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void createPublish(Product product) {
        applicationEventPublisher.publishEvent(new ProductEvent(EventType.CREATE, product.id(), product.name(),
                product.description(), product.price(), product.userId(), TraceIdContext.traceId()));
    }

    public void updatePublish(Product product) {
        applicationEventPublisher.publishEvent(new ProductEvent(EventType.UPDATE, product.id(), product.name(),
                product.description(), product.price(), product.userId(), TraceIdContext.traceId()));
    }

    public void deletePublish(Long productId) {
        applicationEventPublisher.publishEvent(
                new ProductEvent(EventType.DELETE, productId, null, null, null, null, TraceIdContext.traceId()));
    }

}
