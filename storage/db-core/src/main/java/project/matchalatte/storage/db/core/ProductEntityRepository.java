package project.matchalatte.storage.db.core;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import project.matchalatte.core.domain.product.Product;
import project.matchalatte.core.domain.product.ProductRepository;
import project.matchalatte.storage.db.core.support.OffsetLimit;

import java.util.List;

@Repository
public class ProductEntityRepository implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    private final ProductJpaQueryRepository jpaQueryRepository;

    public ProductEntityRepository(ProductJpaRepository jpaRepository,
            ProductJpaQueryRepository productJpaQueryRepository) {
        this.jpaRepository = jpaRepository;
        this.jpaQueryRepository = productJpaQueryRepository;
    }

    @Override
    public Product save(Product product) {
        ProductEntity productEntity = jpaRepository
            .save(new ProductEntity(product.name(), product.description(), product.price(), product.userId()));

        return of(productEntity);
    }

    @Override
    public Product findById(Long id) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        return of(productEntity);
    }

    @Transactional
    @Override
    public Product update(Long id, Product newProduct) {
        ProductEntity productEntity = jpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        productEntity.setName(newProduct.name());
        productEntity.setDescription(newProduct.description());
        productEntity.setPrice(newProduct.price());

        return of(productEntity);
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

        return productEntityList.stream().map(this::of).toList();

    }

    @Override
    public List<Product> findAll() {
        List<ProductEntity> productEntityList = jpaRepository.findAll();

        return productEntityList.stream().map(this::of).toList();
    }

    @Override
    public long countTotal() {
        return jpaQueryRepository.countAllProducts();
    }

    @Override
    public List<Product> findProducts(int offset, int limit) {
        Pageable offsetLimit = OffsetLimit.toPageable(offset, limit);

        List<ProductEntity> productEntityList = jpaQueryRepository.findProducts(offsetLimit);

        return productEntityList.stream().map(this::of).toList();
    }

    @Override
    public List<Product> findProductsNoOffsetNotNull(int limit, Long lastId) {
        List<ProductEntity> productEntityList = jpaQueryRepository.findProductsNoOffsetNotNull(limit, lastId);

        return productEntityList.stream().map(this::of).toList();
    }

    @Override
    public List<Product> findProductsNoOffsetNull(int limit) {
        List<ProductEntity> productEntityList = jpaQueryRepository.findProductsNoOffsetNull(limit);

        return productEntityList.stream().map(this::of).toList();
    }

    @Override
    public List<Product> findProductsByKeyword(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Slice<ProductEntity> productEntitySlice = jpaRepository.findProductsByKeyword(keyword, pageable);

        List<ProductEntity> productEntityList = productEntitySlice.getContent();

        return productEntityList.stream().map(this::of).toList();
    }

    private Product of(ProductEntity productEntity) {
        return new Product(productEntity.getId(), productEntity.getName(), productEntity.getDescription(),
                productEntity.getPrice(), productEntity.getUserId());
    }

}
