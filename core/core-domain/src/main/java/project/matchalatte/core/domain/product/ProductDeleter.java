package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductDeleter {

    private final ProductRepository productRepository;

    private final ProductValidator productValidator;

    public ProductDeleter(ProductRepository productRepository, ProductValidator productValidator) {
        this.productRepository = productRepository;
        this.productValidator = productValidator;
    }

    @Transactional
    public void deleteById(Long productId, Long userId) {
        if (productValidator.matchUserById(userId, productId)) {
            productRepository.deleteById(productId);
        }
        else {
            throw new IllegalArgumentException("User does not have permission for this product.");
        }
    }

}
