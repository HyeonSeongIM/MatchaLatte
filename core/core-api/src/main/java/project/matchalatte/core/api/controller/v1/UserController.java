package project.matchalatte.core.api.controller.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import project.matchalatte.core.support.log.TraceContext;
import project.matchalatte.core.support.error.UserException;
import project.matchalatte.core.support.response.ApiResponse;
import project.matchalatte.infra.security.CustomUserDetails;
import project.matchalatte.infra.security.UserSecurity;
import project.matchalatte.infra.security.UserSecurityService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserSecurityService userSecurityService;
    private final ObjectMapper objectMapper;

    public UserController(UserService userService, UserSecurityService userSecurityService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.userSecurityService = userSecurityService;
        this.objectMapper = objectMapper;
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
        String traceId = TraceContext.traceId();
        Long userId = getCurrentUserId();
        log.info("{}", toJson(new LogEntry(traceId, userId, "/api/v1/user/" + id, "요청시작")));
        User result;
        try {
            result = userService.read(id);
        } catch (UserException e) {
            log.error("{} | 에러 발생", traceId, e);
            throw e;
        }
        log.info("{} | API api/v1/user/{} 요청처리 완료 > userId {}", traceId, id, userId);
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

    public static String traceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public record LogEntry(String traceId, Long userId, String api, String message) {}

    public String toJson(LogEntry logEntry) {
        try {
            return objectMapper.writeValueAsString(logEntry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
