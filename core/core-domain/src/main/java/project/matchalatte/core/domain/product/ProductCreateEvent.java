package project.matchalatte.core.domain.product;

public record ProductCreateEvent(Long id, String name, String description, Long price, Long userId) {
}
