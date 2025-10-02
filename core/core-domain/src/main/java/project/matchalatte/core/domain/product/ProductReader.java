package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;

@Component
public class ProductReader {

    private final ProductRepository productRepository;

    public ProductReader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product readProductById(Long id) {
        return productRepository.findById(id);
    }

}
