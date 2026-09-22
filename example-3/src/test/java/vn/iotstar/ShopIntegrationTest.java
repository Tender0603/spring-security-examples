package vn.iotstar;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSendException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.DataInitializer;
import vn.iotstar.dto.*;
import vn.iotstar.entity.*;
import vn.iotstar.mapper.*;
import vn.iotstar.repository.*;
import vn.iotstar.service.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired RoleRepository roles;
    @Autowired OtpTokenRepository tokens;
    @Autowired AuthService auth;
    @Autowired OtpService otp;
    @Autowired UserService userService;
    @Autowired ProductService productService;
    @Autowired PasswordEncoder encoder;
    @Autowired DataInitializer initializer;
    @Autowired UserMapper userMapper;
    @Autowired ProductMapper productMapper;
    @MockitoBean EmailService email;
    @MockitoBean CloudinaryService cloud;
    private final AtomicReference<String> sentOtp = new AtomicReference<>();

    @BeforeEach
    void prepare() {
        products.deleteAll();
        tokens.deleteAll();
        users.deleteAll();
        initializer.run();
        sentOtp.set(null);
        doAnswer(call -> { sentOtp.set(call.getArgument(1)); return null; })
        .when(email).sendOtp(anyString(), anyString(), anyString());
    }

    private RegisterDTO registration() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newuser");
        dto.setEmail("newuser@example.com");
        dto.setFullName("Nguyễn Văn An");
        dto.setPassword("secret123");
        dto.setConfirmPassword("secret123");
        return dto;
    }

    private ProductDTO product(String name) {
        ProductDTO dto = new ProductDTO();
        dto.setName(name);
        dto.setDescription("Điện thoại có camera");
        dto.setPrice(new BigDecimal("1200000.50"));
        dto.setUserId(users.findByUsername("user01").orElseThrow().getId());
        return dto;
    }

    private UserDTO userDto(String username) {
        UserDTO dto = new UserDTO();
        dto.setUsername(username);
        dto.setEmail(username + "@example.com");
        dto.setFullName("Người dùng " + username);
        dto.setRoleName("ROLE_USER");
        dto.setEnabled(true);
        return dto;
    }

    @Test void anonymousPagesAndTemplatesRender() throws Exception {
        for (String path : new String[]{"/", "/login", "/register", "/verify-otp", "/forgot-password", "/reset-password", "/css/app.css"})
        mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(get("/products")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/users")).andExpect(status().is3xxRedirection());
    }

    @Test void loginUsesRealBcryptAndSession() throws Exception {
        User user = users.findByUsername("user01").orElseThrow();
        assertThat(user.getPassword()).startsWith("$2").isNotEqualTo("123456");
        assertThat(encoder.matches("123456", user.getPassword())).isTrue();
        MockHttpSession session = (MockHttpSession) mvc.perform(formLogin().user("user01").password("123456"))
        .andExpect(authenticated().withUsername("user01")).andExpect(redirectedUrl("/"))
        .andReturn().getRequest().getSession(false);
        mvc.perform(get("/products").session(session)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("user01")));
    }

    @Test void invalidLoginFails() throws Exception {
        mvc.perform(formLogin().user("user01").password("wrong")).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error=true"));
    }

    @Test void inactiveAccountCannotLogin() throws Exception {
        auth.register(registration());
        mvc.perform(formLogin().user("newuser").password("secret123")).andExpect(unauthenticated());
    }

    @Test void rolesProtectAllUserOperations() throws Exception {
        Long id = users.findByUsername("admin").orElseThrow().getId();
        for (String path : new String[]{"/users", "/users/create", "/users/edit/" + id})
        mvc.perform(get(path).with(user("user01").roles("USER"))).andExpect(status().isForbidden());
        for (String path : new String[]{"/users/create", "/users/edit/" + id, "/users/delete/" + id})
        mvc.perform(post(path).with(user("user01").roles("USER")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/users").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test void adminAndUserCanAccessProductForms() throws Exception {
        Long id = productService.create(product("Điện thoại"), null).getId();
        for (String role : new String[]{"USER", "ADMIN"}) {
            for (String path : new String[]{"/products", "/products/create", "/products/edit/" + id})
            mvc.perform(get(path).with(user("viewer").roles(role))).andExpect(status().isOk());
        }
        mvc.perform(get("/users/create").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/users/edit/" + users.findByUsername("user01").orElseThrow().getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test void csrfProtectsMutations() throws Exception {
        mvc.perform(post("/register")).andExpect(status().isForbidden());
        mvc.perform(post("/products/delete/1").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(user("user01"))).andExpect(status().isForbidden());
    }

    @Test void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(formLogin().user("user01").password("123456"))
        .andReturn().getRequest().getSession(false);
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?logout=true"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test void secondLoginExpiresFirstSession() throws Exception {
        MockHttpSession first = (MockHttpSession) mvc.perform(formLogin().user("user01").password("123456"))
        .andReturn().getRequest().getSession(false);
        mvc.perform(formLogin().user("user01").password("123456")).andExpect(authenticated());
        mvc.perform(get("/products").session(first)).andExpect(content().string(org.hamcrest.Matchers.containsString("expired")));
    }

    @Test void invalidRegisterIsRenderedWithoutSendingEmail() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "").param("email", "bad").param("password", "123").param("confirmPassword", "123"))
        .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registerDTO", "username", "email", "password", "fullName"));
        verifyNoInteractions(email);
        assertThat(users.count()).isEqualTo(2);
    }

    @Test void registerMismatchAndDuplicatesAreRejected() {
        RegisterDTO dto = registration();
        dto.setConfirmPassword("different");
        assertThatThrownBy(() -> auth.register(dto)).isInstanceOf(IllegalArgumentException.class);
        dto.setConfirmPassword(dto.getPassword());
        dto.setUsername("admin");
        assertThatThrownBy(() -> auth.register(dto)).isInstanceOf(IllegalArgumentException.class);
        dto.setUsername("newuser");
        dto.setEmail("admin@iotstar.vn");
        assertThatThrownBy(() -> auth.register(dto)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(email);
    }

    @Test void registrationOtpActivationAndLoginFlow() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "newuser").param("email", "newuser@example.com")
        .param("fullName", "Nguyễn Văn An").param("password", "secret123").param("confirmPassword", "secret123"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/verify-otp?email=newuser%40example.com"));
        assertThat(users.findByUsername("newuser").orElseThrow().isEnabled()).isFalse();
        assertThat(sentOtp.get()).matches("[0-9]{6}");
        OtpToken token = tokens.findTopByEmailAndTypeOrderByCreatedAtDesc("newuser@example.com", "REGISTER").orElseThrow();
        assertThat(token.getOtpHash()).isNotEqualTo(sentOtp.get());
        assertThat(encoder.matches(sentOtp.get(), token.getOtpHash())).isTrue();
        mvc.perform(post("/verify-otp").with(csrf()).param("email", "newuser@example.com").param("otp", sentOtp.get()))
        .andExpect(redirectedUrl("/login"));
        assertThat(users.findByUsername("newuser").orElseThrow().isEnabled()).isTrue();
        assertThat(auth.verifyRegister("newuser@example.com", sentOtp.get())).isFalse();
        mvc.perform(formLogin().user("newuser").password("secret123")).andExpect(authenticated());
    }

    @Test void otpExpires() {
        auth.register(registration());
        OtpToken token = tokens.findAll().getFirst();
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        tokens.save(token);
        assertThat(auth.verifyRegister("newuser@example.com", sentOtp.get())).isFalse();
    }

    @Test void otpLocksAfterFiveFailuresAndPersistsAttempts() {
        auth.register(registration());
        for (int i = 0; i < 5; i++) assertThat(auth.verifyRegister("newuser@example.com", "wrong!")).isFalse();
        assertThat(tokens.findAll().getFirst().getAttempts()).isEqualTo(5);
        assertThat(auth.verifyRegister("newuser@example.com", sentOtp.get())).isFalse();
        assertThat(users.findByUsername("newuser").orElseThrow().isEnabled()).isFalse();
    }

    @Test void resendReplacesOtpAndRejectsUnknownOrActiveUsers() throws Exception {
        auth.register(registration());
        Long previous = tokens.findAll().getFirst().getId();
        mvc.perform(post("/resend-register-otp").with(csrf()).param("email", "newuser@example.com")).andExpect(status().is3xxRedirection());
        assertThat(tokens.findAll()).hasSize(1);
        assertThat(tokens.findAll().getFirst().getId()).isNotEqualTo(previous);
        assertThatThrownBy(() -> auth.resendRegisterOtp("missing@example.com")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> auth.resendRegisterOtp("admin@iotstar.vn")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void registerOtpCannotResetPassword() {
        auth.register(registration());
        assertThat(auth.resetPassword("newuser@example.com", sentOtp.get(), "newpass123")).isFalse();
        assertThat(encoder.matches("secret123", users.findByUsername("newuser").orElseThrow().getPassword())).isTrue();
    }

    @Test void forgotPasswordAndResetFlow() throws Exception {
        mvc.perform(post("/forgot-password").with(csrf()).param("email", "user01@gmail.com")).andExpect(redirectedUrl("/reset-password"));
        mvc.perform(post("/reset-password").with(csrf()).param("email", "user01@gmail.com").param("otp", sentOtp.get())
        .param("password", "newpass123").param("confirmPassword", "newpass123")).andExpect(redirectedUrl("/login"));
        assertThat(encoder.matches("newpass123", users.findByUsername("user01").orElseThrow().getPassword())).isTrue();
        assertThat(auth.resetPassword("user01@gmail.com", sentOtp.get(), "another123")).isFalse();
        mvc.perform(formLogin().user("user01").password("123456")).andExpect(unauthenticated());
        mvc.perform(formLogin().user("user01").password("newpass123")).andExpect(authenticated());
    }

    @Test void invalidResetDoesNotConsumeOtp() throws Exception {
        auth.forgotPassword("user01@gmail.com");
        mvc.perform(post("/reset-password").with(csrf()).param("email", "user01@gmail.com").param("otp", sentOtp.get())
        .param("password", "newpass123").param("confirmPassword", "different"))
        .andExpect(status().isOk()).andExpect(model().attributeHasErrors("resetPasswordDTO"));
        assertThat(tokens.findAll().getFirst().isUsed()).isFalse();
    }

    @Test void unknownForgotEmailShowsError() throws Exception {
        mvc.perform(post("/forgot-password").with(csrf()).param("email", "missing@example.com"))
        .andExpect(status().isOk()).andExpect(model().attributeHasErrors("forgotPasswordDTO"));
        verifyNoInteractions(email);
    }

    @Test void smtpFailureRollsBackRegistration() {
        doThrow(new MailSendException("mock SMTP failure")).when(email).sendOtp(anyString(), anyString(), anyString());
        assertThatThrownBy(() -> auth.register(registration())).isInstanceOf(MailSendException.class);
        assertThat(users.existsByUsername("newuser")).isFalse();
        assertThat(tokens.count()).isZero();
    }

    @Test void userCrudSearchPaginationAndDuplicateUpdate() {
        UserDTO created = userService.create(userDto("alice"));
        assertThat(encoder.matches("123456", users.findById(created.getId()).orElseThrow().getPassword())).isTrue();
        assertThat(userService.findAll("ALICE", 0, 1).getTotalElements()).isEqualTo(1);
        assertThat(userService.findAll("", 1, 1).getNumber()).isEqualTo(1);
        created.setFullName("Tên đã sửa");
        userService.update(created.getId(), created);
        assertThat(userService.findById(created.getId()).getFullName()).isEqualTo("Tên đã sửa");
        created.setUsername("admin");
        assertThatThrownBy(() -> userService.update(created.getId(), created)).isInstanceOf(IllegalArgumentException.class);
        userService.delete(created.getId());
        assertThat(userService.countUsers()).isEqualTo(2);
    }

    @Test void userWithProductsCannotBeDeleted() {
        ProductDTO dto = productService.create(product("Phone"), null);
        assertThat(userService.countProducts(dto.getUserId())).isEqualTo(1);
        assertThat(userService.findById(dto.getUserId()).getProductCount()).isEqualTo(1);
        assertThatThrownBy(() -> userService.delete(dto.getUserId())).isInstanceOf(IllegalArgumentException.class);
        productService.delete(dto.getId());
        userService.delete(dto.getUserId());
        assertThat(users.existsById(dto.getUserId())).isFalse();
    }

    @Test void productCrudSearchPaginationAndCounts() {
        ProductDTO first = productService.create(product("Phone A"), null);
        ProductDTO second = productService.create(product("Phone B"), null);
        assertThat(productService.findAll("phone", 0, 1).getTotalElements()).isEqualTo(2);
        assertThat(productService.findAll("camera", 1, 1).getContent()).hasSize(1);
        assertThat(productService.countByUser(first.getUserId())).isEqualTo(2);
        first.setName("Updated");
        first.setUserId(users.findByUsername("admin").orElseThrow().getId());
        productService.update(first.getId(), first, null);
        assertThat(productService.findById(first.getId()).getUsername()).isEqualTo("user01");
        assertThat(productService.findById(first.getId()).getName()).isEqualTo("Updated");
        productService.delete(second.getId());
        assertThat(productService.countProducts()).isEqualTo(1);
        verifyNoInteractions(cloud);
    }

    @Test void productImageUploadReplaceAndDeleteUseCloudinary() {
        MockMultipartFile image = new MockMultipartFile("image", "phone.png", "image/png", new byte[]{1, 2});
        when(cloud.upload(image)).thenReturn(new CloudinaryUploadResult("https://example.invalid/first.png", "first"), new CloudinaryUploadResult("https://example.invalid/second.png", "second"));
        ProductDTO dto = productService.create(product("Image"), image);
        assertThat(dto.getImageUrl()).isEqualTo("https://example.invalid/first.png");
        assertThat(products.findById(dto.getId()).orElseThrow().getImagePublicId()).isEqualTo("first");
        dto = productService.update(dto.getId(), dto, image);
        verify(cloud).delete("first");
        assertThat(dto.getImageUrl()).isEqualTo("https://example.invalid/second.png");
        assertThat(products.findById(dto.getId()).orElseThrow().getImagePublicId()).isEqualTo("second");
        productService.delete(dto.getId());
        verify(cloud).delete("second");
    }

    @Test void failedImageUploadPreservesExistingProduct() {
        ProductDTO dto = productService.create(product("Original"), null);
        dto.setName("Changed");
        MockMultipartFile image = new MockMultipartFile("image", "bad.png", "image/png", new byte[]{1});
        when(cloud.upload(image)).thenThrow(new IllegalStateException("mock failure"));
        assertThatThrownBy(() -> productService.update(dto.getId(), dto, image)).isInstanceOf(IllegalStateException.class);
        assertThat(productService.findById(dto.getId()).getName()).isEqualTo("Original");
        verify(cloud, never()).delete(anyString());
    }

    @Test void createProductUsesLoggedInOwnerAndIgnoresForgedFields() throws Exception {
        User owner = users.findByUsername("user01").orElseThrow();
        mvc.perform(multipart("/products/create").with(user(new vn.iotstar.security.CustomUserDetails(owner))).with(csrf())
        .param("name", "Phone").param("price", "10.50").param("userId", "99999").param("id", "88888").param("imageUrl", "forged"))
        .andExpect(redirectedUrl("/products"));
        Product saved = products.findAll().getFirst();
        assertThat(saved.getUser().getId()).isEqualTo(owner.getId());
        assertThat(saved.getId()).isNotEqualTo(88888L);
        assertThat(saved.getImageUrl()).isNull();
    }

    @Test void productValidationErrorsRenderEditWithPathId() throws Exception {
        ProductDTO dto = productService.create(product("Valid"), null);
        mvc.perform(post("/products/edit/" + dto.getId()).with(user("user01")).with(csrf()).param("name", "").param("price", "-1"))
        .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("productDTO", "name", "price"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("/products/edit/" + dto.getId())));
    }

    @Test void multipartUploadPersistsUrlAndRedirectsToRenderedImage() throws Exception {
        User owner = users.findByUsername("user01").orElseThrow();
        MockMultipartFile image = new MockMultipartFile("image", "phone.png", "image/png", new byte[]{1, 2});
        when(cloud.upload(any())).thenReturn(new CloudinaryUploadResult("https://example.invalid/uploaded.png", "shop/products/uploaded"));
        mvc.perform(multipart("/products/create").file(image)
            .with(user(new vn.iotstar.security.CustomUserDetails(owner))).with(csrf())
            .param("name", "Uploaded phone").param("price", "123.45"))
            .andExpect(redirectedUrl("/products"));
        Product saved = products.findAll().getFirst();
        assertThat(saved.getImageUrl()).isEqualTo("https://example.invalid/uploaded.png");
        assertThat(saved.getImagePublicId()).isEqualTo("shop/products/uploaded");
        mvc.perform(get("/products").with(user("user01")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("src=\"https://example.invalid/uploaded.png\"")));
    }

    @Test void uploadFailureStaysOnCreateFormAndDoesNotInsertProduct() throws Exception {
        User owner = users.findByUsername("user01").orElseThrow();
        when(cloud.upload(any())).thenThrow(CloudinaryOperationException.from(new RuntimeException("Invalid image file")));
        mvc.perform(multipart("/products/create").file(new MockMultipartFile("image", "broken.png", "image/png", new byte[]{1}))
            .with(user(new vn.iotstar.security.CustomUserDetails(owner))).with(csrf())
            .param("name", "Keep this name").param("price", "123.45"))
            .andExpect(status().isOk()).andExpect(view().name("products/form"))
            .andExpect(model().attributeHasErrors("productDTO"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Keep this name")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("JPG/PNG")));
        assertThat(products.count()).isZero();
    }

    @Test void editUploadFailurePreservesOldImageAndForm() throws Exception {
        when(cloud.upload(any())).thenReturn(new CloudinaryUploadResult("https://example.invalid/original.png", "original"));
        MockMultipartFile image = new MockMultipartFile("image", "phone.png", "image/png", new byte[]{1});
        ProductDTO dto = productService.create(product("Original"), image);
        when(cloud.upload(any())).thenThrow(CloudinaryOperationException.from(new RuntimeException("Invalid image file")));
        mvc.perform(multipart("/products/edit/" + dto.getId()).file(image)
            .with(user("user01")).with(csrf()).param("name", "Changed").param("price", "10"))
            .andExpect(status().isOk()).andExpect(view().name("products/form"))
            .andExpect(model().attributeHasErrors("productDTO"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("https://example.invalid/original.png")));
        assertThat(productService.findById(dto.getId()).getName()).isEqualTo("Original");
        verify(cloud, never()).delete(anyString());
    }

    @Test void mapperPreservesFieldsAndExcludesPasswordAndRelationshipsOnInput() {
        User user = users.findByUsername("user01").orElseThrow();
        UserDTO dto = userMapper.toDTO(user);
        assertThat(dto.getRoleName()).isEqualTo("ROLE_USER");
        assertThat(userMapper.toEntity(dto).getPassword()).isNull();
        assertThat(userMapper.toEntity(dto).getRole()).isNull();
        Product entity = productMapper.toEntity(product("Mapping"));
        assertThat(entity.getUser()).isNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(productMapper.toDTO(entity).getName()).isEqualTo("Mapping");
    }

    @Test void initializerIsIdempotentAndNeverOverwritesExistingAccounts() {
        User user = users.findByUsername("admin").orElseThrow();
        user.setPassword(encoder.encode("changed123"));
        user.setFullName("Existing administrator");
        users.save(user);
        initializer.run();
        initializer.run();
        assertThat(users.count()).isEqualTo(2);
        assertThat(roles.count()).isEqualTo(2);
        User existing = users.findByUsername("admin").orElseThrow();
        assertThat(existing.getFullName()).isEqualTo("Existing administrator");
        assertThat(encoder.matches("changed123", existing.getPassword())).isTrue();
    }
}
