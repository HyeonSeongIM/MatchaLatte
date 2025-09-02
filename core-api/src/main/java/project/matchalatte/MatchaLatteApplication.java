package project.matchalatte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MatchaLatteApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchaLatteApplication.class, args);
    }

}
