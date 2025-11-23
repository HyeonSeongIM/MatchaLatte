package project.matchalatte.core.domain.product;

public record ProductEvent(EventType eventType, Long id, String name, String description, Long price, Long userId) {
}
