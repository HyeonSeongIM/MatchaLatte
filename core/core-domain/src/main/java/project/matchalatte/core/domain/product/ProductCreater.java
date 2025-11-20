package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;

@Component
public class ProductCreater {

    private final ProductRepository productRepository;

    public ProductCreater(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(String name, String description, Long price, Long userId) {
        Product product = Product.from(name, description, price, userId);
        return productRepository.save(product);
    }

}
