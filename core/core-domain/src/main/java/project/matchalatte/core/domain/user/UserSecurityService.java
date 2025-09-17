package project.matchalatte.core.domain.user;

import org.springframework.stereotype.Service;

@Service
public class UserSecurityService {

    private final UserSignIn userSignIn;

    private final UserSignUp userSignUp;

    public UserSecurityService(UserSignIn userSignIn, UserSignUp userSignUp) {
        this.userSignIn = userSignIn;
        this.userSignUp = userSignUp;
    }

    public User signUp(String username, String password, String nickname) {
        return userSignUp.signUp(username, password, nickname);
    }

    public String signIn(String username, String password) {
        return userSignIn.singIn(username, password);
    }

}
