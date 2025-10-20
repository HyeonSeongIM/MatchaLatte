package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Service;
import project.matchalatte.core.domain.product.support.Page;

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

    public Page readProductsPageable(int offset, int limit) {
        return productReader.readProductsPageable(offset, limit);
    }

    public List<Product> readProductsSlice(int page, int size, String sortType, String direction) {
        return productReader.readProductsSlice(page, size, sortType, direction);
    }

    public List<Product> findProductsByName(String name, int page, int size) {
        return productReader.findProductByCondition(name, page, size);
    }

}
