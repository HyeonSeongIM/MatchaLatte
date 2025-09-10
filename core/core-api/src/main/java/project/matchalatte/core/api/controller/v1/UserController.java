package project.matchalatte.core.api.controller.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import project.matchalatte.core.api.controller.v1.request.SignInRequest;
import project.matchalatte.core.api.controller.v1.request.SignUpRequest;
import project.matchalatte.core.api.controller.v1.response.SignInResponse;
import project.matchalatte.core.api.controller.v1.response.SignUpResponse;
import project.matchalatte.core.api.controller.v1.response.UserReadResponse;
import project.matchalatte.core.domain.user.User;
import project.matchalatte.core.domain.user.UserService;
import project.matchalatte.core.support.error.UserException;
import project.matchalatte.core.support.response.ApiResponse;
import project.matchalatte.infra.security.UserSecurity;
import project.matchalatte.infra.security.UserSecurityService;
import project.matchalatte.support.logging.LogData;
import project.matchalatte.support.logging.TraceIdContext;
import project.matchalatte.support.logging.UserIdContext;

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
        String traceId = TraceIdContext.traceId();
        Long userId = UserIdContext.getCurrentUserId();
        log.info("{}", LogData.of(traceId, userId, "회원가입", "회원가입API 처리시작"));
        UserSecurity result = userSecurityService.signUp(signUpRequest.username(), signUpRequest.password(), signUpRequest.nickname());
        log.info("{}", LogData.of(traceId, userId, "회원가입", "회원가입API 처리완료"));
        return ApiResponse.success(new SignUpResponse(result.nickname()));
    }

    @PostMapping("/signIn")
    public ApiResponse<SignInResponse> signIn(@RequestBody SignInRequest signInRequest) {
        String traceId = TraceIdContext.traceId();
        Long userId = UserIdContext.getCurrentUserId();
        log.info("{}", LogData.of(traceId, userId, "로그인", "로그인API 처리시작"));
        String result = userSecurityService.signIn(signInRequest.username(), signInRequest.password());
        log.info("{}", LogData.of(traceId, userId, "로그인", "로그인API 처리완료"));
        return ApiResponse.success(new SignInResponse(result));
    }

    // 파라미터에 값이 존재하면 따로 가공해주기
    @GetMapping("/{id}")
    public ApiResponse<UserReadResponse> getUser(@PathVariable("id") Long id) {
        String traceId = TraceIdContext.traceId();
        Long userId = UserIdContext.getCurrentUserId();
        log.info("{}", LogData.of(traceId, userId, "유저정보 가져오기", "처리시작"));
        User result;
        try {
            result = userService.read(id);
        } catch (UserException e) {
            log.error("{} | 에러 발생", traceId, e);
            throw e;
        }
        log.info("{}", LogData.of(traceId, userId, "유저정보 가져오기", "처리완료"));
        return ApiResponse.success(new UserReadResponse(result.id(), result.username()));
    }
}
