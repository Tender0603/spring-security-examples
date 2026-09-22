package vn.iotstar.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.UserRepository;

@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public DataInitializer(RoleRepository roles, UserRepository users, PasswordEncoder encoder) {
        this.roles = roles;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Role admin = role("ROLE_ADMIN");
        Role user = role("ROLE_USER");
        seed("admin", "admin@iotstar.vn", "Administrator", admin);
        seed("user01", "user01@gmail.com", "User 01", user);
    }

    private Role role(String name) {
        return roles.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return roles.save(role);
        });
    }

    private void seed(String username, String email, String fullName, Role role) {
        if (users.existsByUsername(username) || users.existsByEmail(email)) return;
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(encoder.encode("123456"));
        user.setEnabled(true);
        user.setRole(role);
        users.save(user);
    }
}
