package project.matchalatte.core.domain.product;

import org.springframework.stereotype.Component;
import project.matchalatte.core.domain.product.support.Page;
import project.matchalatte.core.domain.product.support.Slice;

import java.util.List;

@Component
public class ProductReader {

    private final ProductRepository productRepository;

    private final ProductCounter productCounter;

    public ProductReader(ProductRepository productRepository, ProductCounter productCounter) {
        this.productRepository = productRepository;
        this.productCounter = productCounter;
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

    public Page readProductsPage(int offset, int limit) {
        List<Product> products = productRepository.findProducts(offset, limit);

        long totalCount = productCounter.getTotalCount();

        long totalPages = totalCount / limit + (totalCount % limit == 0 ? 0 : 1);

        return new Page(products, totalCount, totalPages);
    }

    public Slice readProductsSlice(int offset, int limit) {
        List<Product> products = productRepository.findProducts(offset, limit + 1);

        boolean hasNext = false;

        if (products.size() > limit) {
            hasNext = true;

            products.remove(limit);
        }

        return new Slice(products, hasNext);
    }

    public Slice readProductsSliceNoOffset(int limit, Long lastId) {

        List<Product> products;

        if (lastId == null) {
            products = productRepository.findProductsNoOffsetNull(limit + 1);
        }
        else {
            products = productRepository.findProductsNoOffsetNotNull(limit + 1, lastId);
        }

        boolean hasNext = false;

        if (products.size() > limit) {
            hasNext = true;

            products.remove(limit);
        }

        return new Slice(products, hasNext);
    }

    public List<Product> findProductByCondition(String keyword, int page, int size) {
        return productRepository.findProductsByKeyword(keyword, page, size);
    }

}
