package project.matchalatte.core.domain.product;

public record Product(Long id, String name, String description, Long price, Long userId) {
    public static Product from(String name, String description, Long price, Long userId) {
        return new Product(null, name, description, price, userId);
    }
}
