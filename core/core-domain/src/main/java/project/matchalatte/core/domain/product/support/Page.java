package project.matchalatte.core.domain.product.support;

import java.util.List;

public record Page(List<?> content, long getTotalElements, long getTotalPages) {
}
