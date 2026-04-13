package project.matchalatte.core.domain.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.matchalatte.support.logging.LogData;

@Component
public class ProductCreater {

    private final Logger log = LoggerFactory.getLogger(ProductCreater.class);

    private final ProductRepository productRepository;

    public ProductCreater(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(String name, String description, Long price, Long userId) {
        Product product = Product.from(name, description, price, userId);
        Product savedProduct = productRepository.save(product);
        log.debug(LogData.of("상품 생성 컴포넌트", "DB 저장 완료 ID={}"), savedProduct.id());
        return savedProduct;
    }

}
