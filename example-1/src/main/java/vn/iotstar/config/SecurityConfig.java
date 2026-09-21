package vn.iotstar.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cau hinh Spring Security - theo tai lieu Vi du 1 Buoc 5 (trang 7)
 * - Login bang email
 * - BCrypt password encoder
 * - formLogin / logout / exceptionHandling
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Cac URL public theo tai lieu: login, css, images, error
                .requestMatchers(
                    "/", "/login", "/register", "/verify-otp", "/forgot-password",
                    "/reset-password", "/register/resend-otp",
                    "/css/**", "/images/**", "/error"
                ).permitAll()
                .requestMatchers("/users/**", "/dashboard").hasRole("ADMIN")
                .requestMatchers("/categories/**").authenticated()
                .requestMatchers("/products/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")        // login bang email theo tai lieu
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));
        return http.build();
    }
}
