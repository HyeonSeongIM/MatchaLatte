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

@ExtendWith(MockitoExtension.class)
public class ProductValidatorTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductValidator productValidator;

    @Test
    @DisplayName("상품 ID와 사용자 ID가 일치하면 true를 반환한다.")
    void matchUserById() {
        // given
        Long productId = 1L;
        Long userId = 2L;

        Product product = new Product("장난감", "초딩때 부터 사용했어요", 5000L, userId);

        given(productRepository.findById(any())).willReturn(product);

        // when
        boolean result = productValidator.matchUserById(productId, userId);

        // then
        assertThat(result).isTrue();

    }

    @Test
    @DisplayName("상품 ID와 사용자 ID가 일치하지 않으면 false를 반환한다")
    void matchUserById_ReturnsFalse_WhenUserDoesNotMatch() {
        // given
        Long productId = 1L;
        Long ownerUserId = 2L;
        Long otherUserId = 99L;
        Product product = new Product("장난감", "초딩때 부터 사용했어요", 5000L, ownerUserId);

        given(productRepository.findById(any())).willReturn(product);

        // when
        boolean result = productValidator.matchUserById(productId, otherUserId);

        // then
        assertThat(result).isFalse();
    }

}
