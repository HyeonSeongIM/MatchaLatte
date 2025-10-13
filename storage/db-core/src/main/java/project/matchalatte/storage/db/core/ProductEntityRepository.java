package project.matchalatte.storage.db.core;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import project.matchalatte.core.domain.product.Product;
import project.matchalatte.core.domain.product.ProductRepository;

import java.util.List;

@Repository
public class ProductEntityRepository implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    public ProductEntityRepository(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        ProductEntity productEntity = jpaRepository
            .save(new ProductEntity(product.name(), product.description(), product.price(), product.userId()));

        return new Product(productEntity.getName(), productEntity.getDescription(), productEntity.getPrice(),
                productEntity.getUserId());
    }

    @Override
    public Product findById(Long id) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        return new Product(productEntity.getName(), productEntity.getDescription(), productEntity.getPrice(),
                productEntity.getUserId());
    }

    @Transactional
    @Override
    public Product update(Long id, Product newProduct) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        productEntity.setName(newProduct.name());
        productEntity.setDescription(newProduct.description());
        productEntity.setPrice(newProduct.price());

        return new Product(productEntity.getName(), productEntity.getDescription(), productEntity.getPrice(),
                productEntity.getUserId());
    }

    @Override
    public void deleteById(Long id) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        jpaRepository.delete(productEntity);
    }

    @Override
    public List<Product> findByUserId(Long userId) {
        List<ProductEntity> productEntityList = jpaRepository.findByUserId(userId);

        return productEntityList.stream()
            .map(entity -> new Product(entity.getName(), entity.getDescription(), entity.getPrice(),
                    entity.getUserId()))
            .toList();

    }

    @Override
    public List<Product> findAll() {
        List<ProductEntity> productEntityList = jpaRepository.findAll();

        return productEntityList.stream()
            .map(entity -> new Product(entity.getName(), entity.getDescription(), entity.getPrice(), entity.getId()))
            .toList();
    }

    @Override
    public List<Product> findProductsPageable(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<ProductEntity> productEntityPage = jpaRepository.findAll(pageable);

        List<ProductEntity> productEntityList = productEntityPage.getContent();

        return productEntityList.stream()
            .map(entity -> new Product(entity.getName(), entity.getDescription(), entity.getPrice(), entity.getId()))
            .toList();
    }

    @Override
    public List<Product> findProductsSlice(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Slice<ProductEntity> productEntitySlice = jpaRepository.findAllBy(pageable);

        List<ProductEntity> productEntityList = productEntitySlice.getContent();

        return productEntityList.stream()
            .map(entity -> new Product(entity.getName(), entity.getDescription(), entity.getPrice(), entity.getId()))
            .toList();
    }

}
