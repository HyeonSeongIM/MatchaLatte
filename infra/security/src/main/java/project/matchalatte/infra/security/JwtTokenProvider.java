package project.matchalatte.infra.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private final JwtService jwtService;

    public JwtTokenProvider(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String createToken() {
        return jwtService.test();
    }

    public Authentication getAuthentication(String token) {
        throw new IllegalArgumentException("미생성");
    }

    public String getUsername(String token) {
        throw new IllegalArgumentException("미생성");
    }

    public String resolveToken(String token) {
        throw new IllegalArgumentException("미생성");
    }

    public boolean validateToken(String token) {
        throw new IllegalArgumentException("미생성");
    }

}
