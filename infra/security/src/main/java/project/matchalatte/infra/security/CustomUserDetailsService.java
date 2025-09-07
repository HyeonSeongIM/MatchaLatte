package project.matchalatte.infra.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import project.matchalatte.storage.db.core.UserSecurityRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserSecurityRepository userSecurityRepository;

    public CustomUserDetailsService(UserSecurityRepository userSecurityRepository) {
        this.userSecurityRepository = userSecurityRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userSecurityRepository.findByUsername(username)
                .map(UserSecurity::from)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
