package project.matchalatte.infra.security;

public interface UserDetailsRepository {

    CustomUserDetails findByUsernameEntity(String username);

}
