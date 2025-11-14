package project.matchalatte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class AdminMatchaLatteApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminMatchaLatteApplication.class, args);
    }

}
