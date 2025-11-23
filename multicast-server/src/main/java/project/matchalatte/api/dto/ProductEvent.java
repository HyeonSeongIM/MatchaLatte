package project.matchalatte.api.dto;

public record ProductEvent(EventType type, Long id, String name, String description, Long price, Long userId) {
}
