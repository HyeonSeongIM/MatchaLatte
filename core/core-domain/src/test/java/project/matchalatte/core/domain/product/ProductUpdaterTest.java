package project.matchalatte.core.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductUpdaterTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductValidator productValidator;

    @InjectMocks
    private ProductUpdater productUpdater;

    @Test
    @DisplayName("상품 소유권이 확인되면 상품 정보가 성공적으로 수정된다")
    void updateProduct_success() {
        // given
        Long productId = 1L;
        String newName = "파란색 선풍기";
        String newDescription = "살짝 생활 기스가 있어요.";
        Long newPrice = 29000L;
        Long userId = 1L;

        Product newProduct = new Product(newName, newDescription, newPrice, userId);

        given(productRepository.update(productId, newProduct)).willReturn(newProduct);

        given(productValidator.matchUserById(productId, userId)).willReturn(true);

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

    @Test
    @DisplayName("상품 소유권 검사를 통과하지 못하면 에러를 던진다.")
    void updateProduct_fail() {
        // given
        Long productId = 1L;
        String newName = "파란색 선풍기";
        String newDescription = "살짝 생활 기스가 있어요.";
        Long newPrice = 29000L;
        Long invalidUserId = 99L;
        given(productValidator.matchUserById(productId, invalidUserId)).willReturn(false);

        // when & then
        assertThatThrownBy(
                () -> productUpdater.updateProduct(productId, newName, newDescription, newPrice, invalidUserId))
            .isInstanceOf(IllegalArgumentException.class);

        verify(productValidator, times(1)).matchUserById(productId, invalidUserId);
        verify(productRepository, never()).update(any(), any());
    }

}