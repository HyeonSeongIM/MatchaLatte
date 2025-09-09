package project.matchalatte.core.api.controller.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import project.matchalatte.core.api.controller.v1.request.UserWriterRequest;
import project.matchalatte.core.api.controller.v1.response.UserReadResponse;
import project.matchalatte.core.api.controller.v1.response.UserWriterResponse;
import project.matchalatte.core.domain.user.User;
import project.matchalatte.core.domain.user.UserService;
import project.matchalatte.core.support.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;
    private final Logger log = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<UserWriterResponse> addUser(@RequestBody UserWriterRequest userWriterRequest) {
        User result = userService.add(userWriterRequest.username());
        log.info("[UserAPI] [addUser] ");
        return ApiResponse.success(new UserWriterResponse(result.id()));
    }

    // 파라미터에 값이 존재하면 따로 가공해주기
    @GetMapping("/{id}")
    public ApiResponse<UserReadResponse> getUser(@PathVariable("id") Long id) {
        User result = userService.read(id);
        return ApiResponse.success(new UserReadResponse(result.id(), result.username()))
                ;
    }
}
