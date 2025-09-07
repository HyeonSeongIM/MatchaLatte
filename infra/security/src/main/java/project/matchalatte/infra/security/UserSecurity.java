package project.matchalatte.infra.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import project.matchalatte.storage.db.core.UserSecurityEntity;

import java.util.Collection;
import java.util.List;

public class UserSecurity implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;

    private UserSecurity(Long userId, String username, String password) {
        this.userId = userId;
        this.username = username;
        this.password = password;
    }

    public static UserSecurity from(UserSecurityEntity user) {
        return new UserSecurity(
                user.getId(),
                user.username(),
                user.password()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
