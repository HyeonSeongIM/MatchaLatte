package project.matchalatte.core;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

@ActiveProfiles("local")
@Tag("context")
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@EntityScan(basePackages = "project.matchalatte.storage.db.core")
@EnableJpaRepositories(basePackages = "project.matchalatte.storage.db.core")
public abstract class CoreDbContextTest {

    @Test
    void contextLoads() {
    }

}
