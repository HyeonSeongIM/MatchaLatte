package project.matchalatte.core.support.common;

// util/TraceContext.java
import java.util.UUID;

public final class TraceContext {
    private static final ThreadLocal<String> TL = new ThreadLocal<>();

    public static String traceId() {
        String id = TL.get();
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "");
            TL.set(id);
        }
        return id;
    }

    public static void clear() { TL.remove(); }
}
