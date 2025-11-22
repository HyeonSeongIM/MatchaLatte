package project.matchalatte.domain.entity;

import lombok.Builder;
import lombok.Getter;
import project.matchalatte.api.dto.ProductInfo;

@Builder
@Getter
public class ProductDocument {

    private Long id;

    private String name;

    private String description;

    private Long price;

    private Long userId;

    public static ProductDocument from(ProductInfo product) {
        return ProductDocument.builder()
            .id(product.id())
            .name(product.name())
            .description(product.description())
            .price(product.price())
            .userId(product.userId())
            .build();
    }

}
