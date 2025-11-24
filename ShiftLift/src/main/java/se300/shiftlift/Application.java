package se300.shiftlift;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

@SpringBootApplication
@Theme("default")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner createDefaultAdmin(UserService userService, UserRepository userRepository) {
        return args -> {
            boolean hasAdmin = userRepository.findAll().stream()
                    .anyMatch(u -> u instanceof ManagerUser);
            if (!hasAdmin) {
                try {
                    userService.createManagerUser("admin@erau.edu", "admin");
                    System.out.println("Created default admin: admin@erau.edu / admin");
                } catch (Exception e) {
                }
            }
        };
    }

}
