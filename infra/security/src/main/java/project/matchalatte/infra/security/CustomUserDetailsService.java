package project.matchalatte.infra.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import project.matchalatte.storage.db.core.UserSecurityRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserSecurityRepository userRepository;

    public CustomUserDetailsService(UserSecurityRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user =  userRepository.findByUsernameEntity(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return new CustomUserDetails(user);
    }
}
