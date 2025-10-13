package project.matchalatte.core.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
        Long userId = 1L;

        Product product = new Product(name, description, price, userId);

        given(productRepository.findById(productId)).willReturn(product);

        // when
        Product actualProduct = productReader.readProductByProductId(productId);

        // then
        assertThat(actualProduct).isNotNull();
        assertThat(actualProduct.name()).isEqualTo(name);
        assertThat(actualProduct.description()).isEqualTo(description);
        assertThat(actualProduct.price()).isEqualTo(price);
        assertThat(actualProduct.userId()).isEqualTo(userId);

        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("특정 유저 id가 등록한 상품 조회")
    void readProductByUserId() {
        // given
        Long userId = 1L;

        List<Product> products = List.of(new Product("맥북 m1", "맥북 m1 입니다.", 30000L, userId),
                new Product("맥북 m2", "맥북 m2 입니다.", 40000L, userId), new Product("맥북 m3", "맥북 m3 입니다.", 50000L, userId),
                new Product("맥북 m4", "맥북 m4 입니다.", 60000L, userId));

        given(productRepository.findByUserId(userId)).willReturn(products);

        // when
        List<Product> productList = productReader.readProductsByUserId(userId);

        // then
        assertThat(productList).isNotNull();
        assertThat(productList.size()).isEqualTo(4);
        verify(productRepository, times(1)).findByUserId(userId);

    }

    @Test
    @DisplayName("상품 전체 조회")
    void readAllProducts() {
        // given
        Long userId = 1L;

        List<Product> products = List.of(new Product("맥북 m1", "맥북 m1 입니다.", 30000L, userId),
                new Product("맥북 m2", "맥북 m2 입니다.", 40000L, userId), new Product("맥북 m3", "맥북 m3 입니다.", 50000L, userId),
                new Product("맥북 m4", "맥북 m4 입니다.", 60000L, userId));

        given(productRepository.findAll()).willReturn(products);

        // when
        List<Product> productList = productReader.readAllProducts();

        // then
        assertThat(productList).isNotNull();
        assertThat(productList.size()).isEqualTo(4);
        verify(productRepository, times(1)).findAll();


    }

}