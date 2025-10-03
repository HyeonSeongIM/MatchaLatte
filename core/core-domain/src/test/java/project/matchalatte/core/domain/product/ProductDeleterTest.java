package project.matchalatte.core.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class ProductDeleterTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductDeleter productDeleter;

    @Test
    @DisplayName("상품 단건 삭제")
    void deleteProductById() {
        // given
        Long productId = 1L;

        Product product = new Product("립밤", "새거", 5000L);

        willDoNothing().given(productRepository).deleteById(productId);

        // when
        productDeleter.deleteById(productId);

        // then
        verify(productRepository, times(1)).deleteById(productId);


    }

}