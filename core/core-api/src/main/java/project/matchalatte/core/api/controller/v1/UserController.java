package project.matchalatte.core.api.controller.v1;

import org.hibernate.sql.exec.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import project.matchalatte.core.api.controller.v1.request.SignInRequest;
import project.matchalatte.core.api.controller.v1.request.SignUpRequest;
import project.matchalatte.core.api.controller.v1.response.SignInResponse;
import project.matchalatte.core.api.controller.v1.response.SignUpResponse;
import project.matchalatte.core.api.controller.v1.response.UserReadResponse;
import project.matchalatte.core.domain.user.User;
import project.matchalatte.core.domain.user.UserService;
import project.matchalatte.core.support.response.ApiResponse;
import project.matchalatte.infra.security.CustomUserDetails;
import project.matchalatte.infra.security.UserSecurity;
import project.matchalatte.infra.security.UserSecurityService;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserSecurityService userSecurityService;

    public UserController(UserService userService, UserSecurityService userSecurityService) {
        this.userService = userService;
        this.userSecurityService = userSecurityService;
    }

    @PostMapping("/signUp")
    public ApiResponse<SignUpResponse> signUp(@RequestBody SignUpRequest signUpRequest) {
        UserSecurity result = userSecurityService.signUp(signUpRequest.username(), signUpRequest.password(), signUpRequest.nickname());
        log.info("[UserAPI] [addUser] : 회원가입 요청");
        return ApiResponse.success(new SignUpResponse(result.nickname()));
    }

    @PostMapping("/signIn")
    public ApiResponse<SignInResponse> signIn(@RequestBody SignInRequest signInRequest) {
        String result = userSecurityService.signIn(signInRequest.username(), signInRequest.password());
        log.info("[UserAPI] [signIn] : 로그인 요청");
        return ApiResponse.success(new SignInResponse(result));
    }

    // 파라미터에 값이 존재하면 따로 가공해주기
    @GetMapping("/{id}")
    public ApiResponse<UserReadResponse> getUser(@PathVariable("id") Long id) {
        log.info("API api/v1/user/{} > userId {} 요청처리 시작", id, getCurrentUserId());
        User result = userService.read(id);
        log.info("API api/v1/user/{} 요청처리 완료 > userId {}", id, getCurrentUserId());
        return ApiResponse.success(new UserReadResponse(result.id(), result.username()));
    }

    public static Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails user) {
            return user.getId();
        }
        return null;
    }

    public boolean example(Long id){
        return id.equals(getCurrentUserId());
    }
}
