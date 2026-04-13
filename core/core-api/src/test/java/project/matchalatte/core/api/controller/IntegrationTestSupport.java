package project.matchalatte.core.api.controller;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import project.matchalatte.core.domain.product.ProductService;
import project.matchalatte.core.domain.user.UserSecurityService;
import project.matchalatte.core.domain.user.UserService;

@WithMockUser
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    @Autowired
    protected ProductService productService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected UserSecurityService userSecurityService;

}
