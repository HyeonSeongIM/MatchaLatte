package project.matchalatte.core.domain.product;

public interface ProductRepository {

    Product save(Product product);

    Product findById(Long id);

    Product update(Long id, Product newProduct);

    void deleteById(Long id);

}
