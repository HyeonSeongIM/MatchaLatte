package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;
import project.matchalatte.core.domain.product.support.Page;

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

    public Page readProductsPageable(int offset, int limit) {
        return productRepository.findProductsPageable(offset, limit);
    }

    public List<Product> readProductsSlice(int page, int size, String sortType, String direction) {
        return productRepository.findProductsSlice(page, size, sortType, direction);
    }

    public List<Product> findProductByCondition(String keyword, int page, int size) {
        return productRepository.findProductsByKeyword(keyword, page, size);
    }

}
