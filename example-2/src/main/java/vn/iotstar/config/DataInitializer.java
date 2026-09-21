package vn.iotstar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            RoleRepository roles,
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${ADMIN_USERNAME:admin}") String adminUsername,
            @Value("${ADMIN_EMAIL:admin@iotstar.vn}") String adminEmail,
            @Value("${ADMIN_PASSWORD:123456}") String adminPassword,
            @Value("${USER_USERNAME:user01}") String userUsername,
            @Value("${USER_EMAIL:user01@gmail.com}") String userEmail,
            @Value("${USER_PASSWORD:123456}") String userPassword) {

        return args -> {

            // Tạo ROLE_USER nếu chưa có
            Role userRole = roles.findByName("ROLE_USER").orElse(null);

            if (userRole == null) {
                userRole = new Role();
                userRole.setName("ROLE_USER");
                userRole = roles.save(userRole);
            }

            // Tạo ROLE_ADMIN nếu chưa có
            Role adminRole = roles.findByName("ROLE_ADMIN").orElse(null);

            if (adminRole == null) {
                adminRole = new Role();
                adminRole.setName("ROLE_ADMIN");
                adminRole = roles.save(adminRole);
            }

            // Tạo tài khoản ADMIN
            seed(
                    users,
                    encoder,
                    adminRole,
                    adminUsername,
                    adminEmail,
                    adminPassword,
                    "System Administrator"
            );

            // Tạo tài khoản USER
            seed(
                    users,
                    encoder,
                    userRole,
                    userUsername,
                    userEmail,
                    userPassword,
                    "Nguyễn Hữu Trung"
            );
        };
    }

    private void seed(
            UserRepository users,
            PasswordEncoder encoder,
            Role role,
            String username,
            String email,
            String password,
            String fullName) {

        // Nếu username hoặc email đã tồn tại thì không tạo lại
        if (users.findByUsername(username).isPresent()
                || users.findByEmail(email).isPresent()) {
            return;
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setFullName(fullName);
        user.setImages("/images/user.svg");
        user.setRole(role);
        user.setEnabled(true);

        users.save(user);
    }
}