package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;

@Component
public class ProductDeleter {

    private final ProductRepository productRepository;

    public ProductDeleter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

}
