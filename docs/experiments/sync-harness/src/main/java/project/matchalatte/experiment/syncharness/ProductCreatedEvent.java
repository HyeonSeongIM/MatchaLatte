package project.matchalatte.experiment.syncharness;

public record ProductCreatedEvent(Long id, String name, String description, Long price, Long userId) {
}
