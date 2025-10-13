package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductReader {

    private final ProductRepository productRepository;

    public ProductReader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product readProductByProductId(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> readProductsByUserId(Long userId) {
        return productRepository.findByUserId(userId);
    }

    public List<Product> readAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> readProductsPageable(int page, int size) {
        return productRepository.findProductsPageable(page, size);
    }

}
