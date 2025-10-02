package project.matchalatte.core.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductReaderTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductReader productReader;

    @Test
    @DisplayName("특정 id에 대해 상품 읽기")
    void readProductById() {
        // given
        String name = "아이돌 굿즈";
        String description = "BTS 아이돌 굿즈입니다.";
        Long price = 50000L;
        Long productId = 1L;

        Product product = new Product(name, description, price);

        given(productRepository.findById(productId)).willReturn(product);

        // when
        Product actualProduct = productReader.readProductById(productId);

        // then
        assertThat(actualProduct).isNotNull();
        assertThat(actualProduct.name()).isEqualTo(name);
        assertThat(actualProduct.description()).isEqualTo(description);
        assertThat(actualProduct.price()).isEqualTo(price);

        verify(productRepository, times(1)).findById(productId);
    }

}