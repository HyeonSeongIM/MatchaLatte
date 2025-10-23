package project.matchalatte.core.domain.product.support;

import java.util.List;

public record Slice(List<?> contents, Boolean hasNext) {
}
