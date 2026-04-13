package project.matchalatte.core.domain.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import project.matchalatte.core.domain.product.support.Page;
import project.matchalatte.core.domain.product.support.Slice;
import project.matchalatte.support.logging.LogData;

import java.util.List;

@Service
public class ProductService {

    private final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductCreater productCreater;

    private final ProductReader productReader;

    private final ProductUpdater productUpdater;

    private final ProductDeleter productDeleter;

    private final ProductEventer productEventer;

    public ProductService(ProductCreater productCreater, ProductReader productReader, ProductUpdater productUpdater,
            ProductDeleter productDeleter, ProductEventer productEventer) {
        this.productEventer = productEventer;
        this.productCreater = productCreater;
        this.productReader = productReader;
        this.productUpdater = productUpdater;
        this.productDeleter = productDeleter;
    }

    public Product createProduct(String name, String description, Long price, Long userId) {
        log.debug(LogData.of("상품 생성 로직", "요청 받음 name={} userId={}"), name, userId);
        Product product = productCreater.createProduct(name, description, price, userId);
        productEventer.createPublish(product);
        log.info(LogData.of("상품 생성 로직", "생성 완료 [ID:{}]"), product.id());
        return product;
    }

    public Product readProductById(Long id) {
        log.debug(LogData.of("상품 단건 읽기 로직", "요청 받음 ID={}"), id);
        Product product = productReader.readProductByProductId(id);
        log.debug(LogData.of("상품 단건 읽기 로직", "조회 완료"));
        return product;
    }

    public Product updateProduct(Long productId, String name, String description, Long price, Long userId) {
        log.debug(LogData.of("상품 수정 로직", "요청 받음 ID={} userId={}"), productId, userId);
        Product product = productUpdater.updateProduct(productId, name, description, price, userId);
        productEventer.updatePublish(product);
        log.info(LogData.of("상품 수정 로직", "수정 완료 [ID:{}]"), productId);
        return product;
    }

    public void deleteProductById(Long productId, Long userId) {
        log.debug(LogData.of("상품 삭제 로직", "요청 받음 ID={} userId={}"), productId, userId);
        productDeleter.deleteById(productId, userId);
        productEventer.deletePublish(productId);
        log.info(LogData.of("상품 삭제 로직", "삭제 완료 [ID:{}]"), productId);
    }

    public List<Product> readProductsByUserId(Long userId) {
        log.debug(LogData.of("상품 유저 별 읽기 로직", "요청 받음 userId={}"), userId);
        return productReader.readProductsByUserId(userId);
    }

    public Page readProductsPage(int offset, int limit) {
        log.debug(LogData.of("상품 페이지 읽기 로직", "요청 받음 offset={} limit={}"), offset, limit);
        return productReader.readProductsPage(offset, limit);
    }

    public Slice readProductsSlice(int offset, int limit) {
        log.debug(LogData.of("상품 슬라이스 읽기 로직", "요청 받음 offset={} limit={}"), offset, limit);
        return productReader.readProductsSlice(offset, limit);
    }

    public Slice readProductsSliceNoOffset(int limit, Long lastId) {
        log.debug(LogData.of("상품 슬라이스(NoOffset) 읽기 로직", "요청 받음 limit={} lastId={}"), limit, lastId);
        return productReader.readProductsSliceNoOffset(limit, lastId);
    }

    public List<Product> findProductsByName(String name, int page, int size) {
        log.debug(LogData.of("상품 검색 로직", "요청 받음 name={} page={} size={}"), name, page, size);
        return productReader.findProductByCondition(name, page, size);
    }

}