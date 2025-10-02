package project.matchalatte.storage.db.core;

import org.springframework.stereotype.Repository;
import project.matchalatte.core.domain.product.Product;
import project.matchalatte.core.domain.product.ProductRepository;

@Repository
public class ProductEntityRepository implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    public ProductEntityRepository(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        ProductEntity productEntity = jpaRepository
            .save(new ProductEntity(product.name(), product.description(), product.price()));

        return new Product(productEntity.getName(), productEntity.getDescription(), productEntity.getPrice());
    }

    @Override
    public Product findById(Long id) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        return new Product(productEntity.getName(), productEntity.getDescription(), productEntity.getPrice());
    }

    @Override
    public Product update(Long id, Product product) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        productEntity.setName(product.name());
        productEntity.setDescription(product.description());
        productEntity.setPrice(product.price());

        return new Product(productEntity.getName(), productEntity.getDescription(), productEntity.getPrice());
    }

    @Override
    public void deleteById(Long id) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        jpaRepository.delete(productEntity);
    }

}
