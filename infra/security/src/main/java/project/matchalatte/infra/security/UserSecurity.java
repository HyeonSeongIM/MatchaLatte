package project.matchalatte.infra.security;

public record UserSecurity(
        String username,
        String password,
        String nickname
) {

    public static UserSecurity of(String username, String password, String nickname) {
        return new UserSecurity(username, password, nickname);
    }
}
