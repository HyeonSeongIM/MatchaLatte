package project.matchalatte.infra.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDetailsRepository userRepository;

    public CustomUserDetailsService(UserDetailsRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsernameEntity(username);

        return new CustomUserDetails(user.getId(), user.getUsername(), user.getPassword());
    }

}
