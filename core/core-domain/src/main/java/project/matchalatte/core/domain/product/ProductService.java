package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Service;

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

    public Product createProduct(String name, String description, Long price) {
        return productCreater.createProduct(name, description, price);
    }

    public Product readProductById(Long id) {
        return productReader.readProductById(id);
    }

    public Product updateProduct(Long id, String name, String description, Long price) {
        return productUpdater.updateProduct(id, name, description, price);
    }

}
