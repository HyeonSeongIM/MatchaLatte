package project.matchalatte.core.domain.user;

import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserReader userReader;
    private final UserWriter userWriter;

    public UserService(UserReader userReader, UserWriter userWriter) {
        this.userReader = userReader;
        this.userWriter = userWriter;
    }

    public Long add(String username) {
        return userWriter.add(username);
    }

    public User read(Long id){
        return userReader.read(id);
    }
}
