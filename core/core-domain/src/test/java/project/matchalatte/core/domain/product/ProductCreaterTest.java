package project.matchalatte.core.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductCreaterTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductCreater productCreater;

    // ========== 정상 케이스 ==========
    // 해당 메서드가 실행되었을 때 어떤 결과를 만들어내는가
    @Test
    @DisplayName("상품 생성 여부 확인")
    void createProduct() {
        // given
        String name = "곰 인형";
        String description = "거의 새 상품이에요! 네고 안됩니다.";
        Long price = 10000L;
        Long userId = 1L;

        Product product = new Product(name, description, price, userId);

        given(productRepository.save(any(Product.class))).willReturn(product);

        // when
        Product actualProduct = productCreater.createProduct(product.name(), product.description(), product.price(),
                product.userId());

        // then
        assertThat(actualProduct).isNotNull();
        assertThat(actualProduct.name()).isEqualTo(name);
        assertThat(actualProduct.description()).isEqualTo(description);
        assertThat(actualProduct.price()).isEqualTo(price);
        assertThat(actualProduct.userId()).isEqualTo(userId);

        verify(productRepository, times(1)).save(any(Product.class));
    }

}