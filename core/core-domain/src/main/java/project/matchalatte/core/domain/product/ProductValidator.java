package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;

@Component
public class ProductValidator {

    private final ProductRepository productRepository;

    public ProductValidator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public boolean matchUserById(Long productId, Long userId) {
        Product productById = productRepository.findById(productId);

        Long productMadeByUserId = productById.userId();

        return productMadeByUserId.equals(userId);
    }

}
