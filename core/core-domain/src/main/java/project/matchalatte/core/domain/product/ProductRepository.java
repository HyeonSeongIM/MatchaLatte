package project.matchalatte.core.domain.product;

import java.util.List;

public interface ProductRepository {

    Product save(Product product);

    Product findById(Long id);

    Product update(Long id, Product newProduct);

    void deleteById(Long id);

    List<Product> findByUserId(Long userId);

    List<Product> findAll();

    List<Product> findProducts(int offset, int limit);

    long countTotal();

    List<Product> findProductsNoOffsetNotNull(int limit, Long lastId);

    List<Product> findProductsNoOffsetNull(int limit);

    List<Product> findProductsByKeyword(String keyword, int page, int size);

}
