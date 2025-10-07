package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;

@Component
public class ProductUpdater {

    private final ProductRepository productRepository;

    public ProductUpdater(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product updateProduct(Long productId, String newName, String newDescription, Long price, Long userId) {
        Product newProduct = new Product(newName, newDescription, price, userId);

        return productRepository.update(productId, newProduct);
    }

}
