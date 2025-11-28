package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;

@Component
public class ProductUpdater {

    private final ProductRepository productRepository;

    private final ProductValidator productValidator;

    public ProductUpdater(ProductRepository productRepository, ProductValidator productValidator) {
        this.productRepository = productRepository;
        this.productValidator = productValidator;
    }

    public Product updateProduct(Long productId, String newName, String newDescription, Long price, Long userId) {
        Product newProduct = Product.from(newName, newDescription, price, userId);

        if (productValidator.matchUserById(productId, userId)) {
            return productRepository.update(productId, newProduct);
        }
        else {
            throw new IllegalArgumentException("User does not have permission for this product.");
        }
    }

}
