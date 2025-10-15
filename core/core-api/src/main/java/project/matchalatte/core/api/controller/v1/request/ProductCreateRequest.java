package project.matchalatte.core.api.controller.v1.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(@Size(min = 1, max = 10, message = "상품의 이름은 1자 이상 10자 이하로 허용됩니다.") String name,

        @Size(min = 10, max = 256, message = "상품 설명은 10자 이상 256자 이하로 허용됩니다.") String description,

        @PositiveOrZero(message = "가격은 0원 이상 허용됩니다.") Long price) {
}
