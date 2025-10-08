package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductCreater productCreater;

    private final ProductReader productReader;

    private final ProductUpdater productUpdater;

    private final ProductDeleter productDeleter;

    private final ProductValidator productValidator;

    public ProductService(ProductCreater productCreater, ProductReader productReader, ProductUpdater productUpdater,
            ProductDeleter productDeleter, ProductValidator productValidator) {
        this.productCreater = productCreater;
        this.productReader = productReader;
        this.productUpdater = productUpdater;
        this.productDeleter = productDeleter;
        this.productValidator = productValidator;
    }

    public Product createProduct(String name, String description, Long price, Long userId) {
        return productCreater.createProduct(name, description, price, userId);
    }

    public Product readProductById(Long id) {
        return productReader.readProductById(id);
    }

    public Product updateProduct(Long productId, String name, String description, Long price, Long userId) {
        if (productValidator.matchUserById(productId, userId)) {
            return productUpdater.updateProduct(productId, name, description, price, userId);
        }
        else {
            throw new IllegalArgumentException("Product with id " + productId + " does not exist");
        }
    }

    public void deleteProductById(Long productId, Long userId) {
        if (productValidator.matchUserById(productId, userId)) {
            productDeleter.deleteById(productId);
        }
        else {
            throw new IllegalArgumentException("Product with id " + productId + " does not exist");
        }
    }

}
