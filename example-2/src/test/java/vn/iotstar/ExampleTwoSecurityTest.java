package vn.iotstar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.controller.AuthController;
import vn.iotstar.controller.HomeController;
import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.security.CustomUserDetailsService;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// No JPA context or SQL Server connection. Exercise actual Security and Thymeleaf with a mocked repository.
@WebMvcTest(controllers = {AuthController.class, HomeController.class},
        properties = "spring.config.import=")
@Import({SecurityConfig.class, CustomUserDetailsService.class,
        ExampleTwoSecurityTest.AdminProbeController.class})
class ExampleTwoSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired CustomUserDetailsService detailsService;
    @MockitoBean UserRepository users;

    // Exists only in tests: the PDF defines /admin/** rules, but no production admin page.
    @RestController
    static class AdminProbeController {
        @GetMapping("/admin/probe")
        String probe() { return "admin-ok"; }
    }

    private User account(boolean enabled, String image) {

        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_ADMIN");

        User account = new User();
        account.setId(9L);
        account.setUsername("admin");
        account.setEmail("admin@iotstar.vn");
        account.setPassword(encoder.encode("test-password"));
        account.setFullName("Quản trị viên");
        account.setImages(image);
        account.setEnabled(enabled);
        account.setRole(role);

        when(users.findByUsernameOrEmail("admin", "admin"))
                .thenReturn(Optional.of(account));

        when(users.findByUsernameOrEmail(
                "admin@iotstar.vn",
                "admin@iotstar.vn"))
                .thenReturn(Optional.of(account));

        return account;
    }

    private MockHttpSession login(String login) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin().user(login).password("test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"))
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void anonymousHomeRedirectsToLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginPageHasCorrectInputsAndCsrf() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "admin@iotstar.vn"})
    void usernameAndEmailBothCreateTheCorrectPrincipalAndRenderHeader(String input) throws Exception {
        User account = account(true, "/images/user.svg");
        MockHttpSession session = login(input);
        SecurityContext context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        CustomUserDetails principal = assertInstanceOf(CustomUserDetails.class,
                context.getAuthentication().getPrincipal());
        assertAll(
                () -> assertEquals(9L, principal.getId()),
                () -> assertEquals("admin", principal.getUsername()),
                () -> assertEquals("admin@iotstar.vn", principal.getEmail()),
                () -> assertEquals("Quản trị viên", principal.getFullName()),
                () -> assertEquals("/images/user.svg", principal.getImages()),
                () -> assertEquals("ROLE_ADMIN", principal.getRole()),
                () -> assertEquals(account.getPassword(), principal.getPassword()),
                () -> assertTrue(principal.isEnabled()),
                () -> assertTrue(principal.isAccountNonExpired()),
                () -> assertTrue(principal.isAccountNonLocked()),
                () -> assertTrue(principal.isCredentialsNonExpired())
        );
        verify(users).findByUsernameOrEmail(input, input);
        mvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trang chủ - UTEShop")))
                .andExpect(content().string(containsString("<header>")))
                .andExpect(content().string(containsString("Quản trị viên")))
                .andExpect(content().string(containsString("(admin)")))
                .andExpect(content().string(containsString("admin@iotstar.vn")))
                .andExpect(content().string(containsString("ROLE_ADMIN")))
                .andExpect(content().string(containsString("src=\"/images/user.svg\"")))
                .andExpect(content().string(containsString("Copyright © UTEShop")))
                .andExpect(content().string(not(containsString(account.getPassword()))));
    }

    @Test
    void incorrectPasswordFailsAuthentication() throws Exception {
        account(true, null);
        mvc.perform(formLogin().user("admin").password("wrong"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void disabledAccountFailsAuthentication() throws Exception {
        account(false, null);
        mvc.perform(formLogin().user("admin@iotstar.vn").password("test-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void unknownAccountThrowsAndCannotLogin() throws Exception {
        when(users.findByUsernameOrEmail("missing", "missing")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> detailsService.loadUserByUsername("missing"));
        mvc.perform(formLogin().user("missing").password("test-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void headerFallsBackToDefaultAvatar(String image) throws Exception {
        account(true, image);
        mvc.perform(get("/").session(login("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("src=\"/images/avatar-default.svg\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/images/user.svg", "/images/avatar-default.svg"})
    void avatarFilesArePublicAndExist(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().string(containsString("<svg")));
    }

    @Test
    void userCannotAccessAdminUrls() throws Exception {
        mvc.perform(get("/admin/probe").with(user("user01").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminUrls() throws Exception {
        account(true, null);
        mvc.perform(get("/admin/probe").session(login("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-ok"));
    }

    @Test
    void logoutInvalidatesSessionAndClearsAuthentication() throws Exception {
        account(true, null);
        MockHttpSession session = login("admin");
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(unauthenticated())
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertTrue(session.isInvalid());
        mvc.perform(get("/")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login?logout=true"))
                .andExpect(content().string(containsString("Bạn đã đăng xuất")));
    }

    @Test
    void loginRequiresCsrfToken() throws Exception {
        mvc.perform(post("/login").param("username", "admin").param("password", "test-password"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(users);
    }
}
