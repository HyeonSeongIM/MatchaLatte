package project.matchalatte.core.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductDeleterTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductValidator productValidator;

    @InjectMocks
    private ProductDeleter productDeleter;

    @Test
    @DisplayName("상품 소유권이 확인되면 상품이 성공적으로 삭제된다")
    void deleteProductById_success() {
        // given
        Long productId = 1L;
        Long userId = 2L;

        given(productValidator.matchUserById(userId, productId)).willReturn(true);

        // when
        productDeleter.deleteById(productId, userId);

        // then
        verify(productRepository, times(1)).deleteById(productId);

    }

    @Test
    @DisplayName("상품 소유권 검사를 통과하지 못하면 에러를 반환한다.")
    void deleteProductById_fail() {
        // given
        Long productId = 1L;
        Long invalidUserId = 2L;

        given(productValidator.matchUserById(invalidUserId, productId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> productDeleter.deleteById(productId, invalidUserId))
            .isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).deleteById(productId);
    }

}