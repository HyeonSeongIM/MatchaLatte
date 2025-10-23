package project.matchalatte.storage.db.core.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record OffsetLimit(int offset, int limit) {
    public static Pageable toPageable(int offset, int limit) {
        return PageRequest.of(offset, limit);
    }
}
