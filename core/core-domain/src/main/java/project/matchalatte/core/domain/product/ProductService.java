package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductCreater productCreater;

    private final ProductReader productReader;

    private final ProductUpdater productUpdater;

    private final ProductDeleter productDeleter;

    public ProductService(ProductCreater productCreater, ProductReader productReader, ProductUpdater productUpdater,
            ProductDeleter productDeleter) {
        this.productCreater = productCreater;
        this.productReader = productReader;
        this.productUpdater = productUpdater;
        this.productDeleter = productDeleter;
    }

    public Product createProduct(String name, String description, Long price, Long userId) {
        return productCreater.createProduct(name, description, price, userId);
    }

    public Product readProductById(Long id) {
        return productReader.readProductByProductId(id);
    }

    public Product updateProduct(Long productId, String name, String description, Long price, Long userId) {
        return productUpdater.updateProduct(productId, name, description, price, userId);
    }

    public void deleteProductById(Long productId, Long userId) {
        productDeleter.deleteById(productId, userId);
    }

    public List<Product> readProductsByUserId(Long userId) {
        return productReader.readProductsByUserId(userId);
    }

    public List<Product> readAllProducts() {
        return productReader.readAllProducts();
    }

    public List<Product> readProductsPageable(int page, int size) {
        return productReader.readProductsPageable(page, size);
    }

    public List<Product> readProductsSlice(int page, int size) {
        return productReader.readProductsSlice(page, size);
    }

}
