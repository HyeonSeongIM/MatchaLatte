package project.matchalatte.storage.db.core;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    @Override
    public Product update(Long id, Product newProduct) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        productEntity.setName(newProduct.name());
        productEntity.setDescription(newProduct.description());
        productEntity.setPrice(newProduct.price());

        return new Product(productEntity.getName(), productEntity.getDescription(), productEntity.getPrice());
    }

    @Override
    public void deleteById(Long id) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        jpaRepository.delete(productEntity);
    }

}
