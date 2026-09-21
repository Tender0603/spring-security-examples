package vn.iotstar.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataInitializerTest {
    @Test
    void createsBothAccountsWithEncodedPasswords() throws Exception {
        RoleRepository roles = mock(RoleRepository.class);
        UserRepository users = mock(UserRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        when(roles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        new DataInitializer().initData(roles, users, encoder,
                "admin", "admin@iotstar.vn", "admin-test",
                "user01", "user01@gmail.com", "user-test").run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users, times(2)).save(saved.capture());
        User admin = saved.getAllValues().get(0);
        User user = saved.getAllValues().get(1);
        assertEquals("ROLE_ADMIN", admin.getRole().getName());
        assertEquals("ROLE_USER", user.getRole().getName());
        assertTrue(encoder.matches("admin-test", admin.getPassword()));
        assertTrue(encoder.matches("user-test", user.getPassword()));
        assertNotEquals("admin-test", admin.getPassword());
        assertNotEquals("user-test", user.getPassword());
        assertTrue(admin.isEnabled());
        assertTrue(user.isEnabled());
    }

    @Test
    void existingUsernameOrEmailIsNeverOverwritten() throws Exception {
        RoleRepository roles = mock(RoleRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(roles.findByName(anyString()))
        .thenAnswer(invocation -> {
            Role role = new Role();
            role.setName(invocation.getArgument(0));
            return Optional.of(role);
        });
        when(users.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(users.findByEmail("user01@gmail.com")).thenReturn(Optional.of(new User()));
        var runner = new DataInitializer().initData(roles, users, new BCryptPasswordEncoder(),
                "admin", "admin@iotstar.vn", "unused", "user01", "user01@gmail.com", "unused");
        runner.run();
        runner.run();
        verify(users, never()).save(any());
        verify(roles, never()).save(any());
    }
}
