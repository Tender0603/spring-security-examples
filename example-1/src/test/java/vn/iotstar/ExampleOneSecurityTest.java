package vn.iotstar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.controller.AuthController;
import vn.iotstar.controller.HomeController;
import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.CustomUserDetailsService;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Exercise the real security filter chain and Thymeleaf views without database writes.
@WebMvcTest(controllers = {AuthController.class, HomeController.class})
@Import({SecurityConfig.class, CustomUserDetailsService.class})
class ExampleOneSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean UserRepository users;

    private void account(boolean enabled) {
        User account = new User();
        account.setEmail("admin@example.test");
        account.setPassword(encoder.encode("test-password"));
        account.setRole(new Role("ADMIN"));
        account.setEnabled(enabled);
        when(users.findByEmailWithRole(account.getEmail())).thenReturn(Optional.of(account));
    }

    @Test
    void loginPageUsesEmailAndKeepsThePdfLinks() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("href=\"/register\"")))
                .andExpect(content().string(containsString("href=\"/forgot-password\"")));
    }

    @Test
    void anonymousDashboardRequiresLogin() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminCanLoginSeeDashboardAndLogout() throws Exception {
        account(true);
        MockHttpSession session = (MockHttpSession) mvc.perform(formLogin()
                        .user("email", "admin@example.test").password("test-password"))
                .andExpect(authenticated().withUsername("admin@example.test"))
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("admin@example.test")))
                .andExpect(content().string(containsString("action=\"/logout\"")));

        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(unauthenticated())
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertTrue(session.isInvalid());
        mvc.perform(get("/dashboard"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void wrongPasswordCannotLogin() throws Exception {
        account(true);
        mvc.perform(formLogin().user("email", "admin@example.test").password("wrong"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        account(false);
        mvc.perform(formLogin().user("email", "admin@example.test").password("test-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void userRoleIsDeniedAndTheErrorPageExists() throws Exception {
        mvc.perform(get("/dashboard").with(user("member@example.test").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
        mvc.perform(get("/access-denied").with(user("member@example.test").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(view().name("access-denied"))
                .andExpect(content().string(containsString("Không có quyền truy cập")));
    }

    @Test
    void anonymousHomeDoesNotClaimSuccessfulLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>Home</h1>")))
                .andExpect(content().string(not(containsString("Bạn đã đăng nhập thành công"))));
    }
}
