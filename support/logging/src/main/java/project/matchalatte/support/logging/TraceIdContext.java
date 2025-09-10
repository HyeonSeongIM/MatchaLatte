package project.matchalatte.support.logging;

import java.util.UUID;

public final class TraceIdContext {
    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static String traceId() {
        String id = threadLocal.get();
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "");
            threadLocal.set(id);
        }
        return id;
    }

    public static void clear() {
        threadLocal.remove();
    }
}
