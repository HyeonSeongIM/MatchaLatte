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
class ProductUpdaterTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductUpdater productUpdater;

    @Test
    @DisplayName("UPDATE 로직 정상 여부 확인")
    void updateProduct() {
        // given
        Long productId = 1L;

        String newName = "파란색 선풍기";
        String newDescription = "살짝 생활 기스가 있어요.";
        Long newPrice = 29000L;
        Long userId = 1L;

        Product newProduct = new Product(newName, newDescription, newPrice, userId);

        given(productRepository.update(productId, newProduct)).willReturn(newProduct);

        // when
        Product actualProduct = productUpdater.updateProduct(productId, newName, newDescription, newPrice, userId);

        // then
        assertThat(actualProduct).isNotNull();
        assertThat(actualProduct.name()).isEqualTo(newName);
        assertThat(actualProduct.description()).isEqualTo(newDescription);
        assertThat(actualProduct.price()).isEqualTo(newPrice);
        assertThat(actualProduct.userId()).isEqualTo(userId);

        verify(productRepository, times(1)).update(productId, newProduct);
    }

}