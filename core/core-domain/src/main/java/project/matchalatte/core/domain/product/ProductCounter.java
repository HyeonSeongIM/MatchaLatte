package project.matchalatte.core.domain.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class ProductCounter {

    private final ProductRepository productRepository;

    private final Logger log = LoggerFactory.getLogger(ProductCounter.class);

    public ProductCounter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "productsCount")
    public long getTotalCount() {
        return productRepository.countTotal();
    }

}
