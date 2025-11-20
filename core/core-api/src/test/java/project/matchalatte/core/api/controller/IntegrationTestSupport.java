package project.matchalatte.core.api.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import project.matchalatte.core.api.controller.v1.UserController;
import project.matchalatte.core.domain.product.ProductService;
import project.matchalatte.core.domain.user.UserSecurityService;
import project.matchalatte.core.domain.user.UserService;

@WithMockUser
@WebMvcTest(controllers = {})
public abstract class IntegrationTestSupport {
    @MockBean
    public ProductService productService;
    @MockBean
    public UserService userService;
    @MockBean
    public UserSecurityService userSecurityService;
}
