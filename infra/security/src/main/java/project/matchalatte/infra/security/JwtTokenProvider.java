package project.matchalatte.infra.security;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private final CustomUserDetailsService userDetailsService;

    public JwtTokenProvider(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.accessTime}")
    private long accessTtlMinutes;

    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String createToken(String username) {
        return Jwts.builder()
                .claim("username", username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 60))
                .signWith(secretKey)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        String username = getUsername(token);
        CustomUserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .after(new Date());
    }

}
