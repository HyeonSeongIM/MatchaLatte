package project.matchalatte.core.domain.product;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import project.matchalatte.core.domain.product.support.Page;
import project.matchalatte.core.domain.product.support.Slice;

import java.util.List;

@Service
public class ProductService {

    private final ApplicationEventPublisher applicationEventPublisher;

    private final ProductCreater productCreater;

    private final ProductReader productReader;

    private final ProductUpdater productUpdater;

    private final ProductDeleter productDeleter;

    public ProductService(ApplicationEventPublisher applicationEventPublisher, ProductCreater productCreater,
            ProductReader productReader, ProductUpdater productUpdater, ProductDeleter productDeleter) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.productCreater = productCreater;
        this.productReader = productReader;
        this.productUpdater = productUpdater;
        this.productDeleter = productDeleter;
    }

    public Product createProduct(String name, String description, Long price, Long userId) {
        Product product = productCreater.createProduct(name, description, price, userId);
        applicationEventPublisher.publishEvent(new ProductCreateEvent(product.id()));
        return product;
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

    public Page readProductsPage(int offset, int limit) {
        return productReader.readProductsPage(offset, limit);
    }

    public Slice readProductsSlice(int offset, int limit) {
        return productReader.readProductsSlice(offset, limit);
    }

    public Slice readProductsSliceNoOffset(int limit, Long lastId) {
        return productReader.readProductsSliceNoOffset(limit, lastId);
    }

    public List<Product> findProductsByName(String name, int page, int size) {
        return productReader.findProductByCondition(name, page, size);
    }

}
