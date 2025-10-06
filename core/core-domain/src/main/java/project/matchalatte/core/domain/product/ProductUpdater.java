package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;

@Component
public class ProductUpdater {

    private final ProductRepository productRepository;

    public ProductUpdater(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product updateProduct(Long id, String newName, String newDescription, Long price) {
        Product newProduct = new Product(newName, newDescription, price);

        return productRepository.update(id, newProduct);
    }

}
