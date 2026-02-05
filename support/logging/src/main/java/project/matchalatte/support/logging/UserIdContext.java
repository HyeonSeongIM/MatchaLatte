package project.matchalatte.support.logging;

import org.springframework.security.core.context.SecurityContextHolder;
import project.matchalatte.infra.security.CustomUserDetails;

public final class UserIdContext {

    public static Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null)
            return null;

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails user) {
            return user.getId();
        }

        return null;
    }

}
