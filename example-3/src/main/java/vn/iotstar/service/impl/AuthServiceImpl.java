package vn.iotstar.service.impl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.RegisterDTO;
import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.AuthService;
import vn.iotstar.service.OtpService;
@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    @Override @Transactional
    public void register(RegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername()))
        throw new IllegalArgumentException("Username đã tồn tại");
        if (userRepository.existsByEmail(dto.getEmail()))
        throw new IllegalArgumentException("Email đã tồn tại");
        if (dto.getPassword() == null || dto.getPassword().length() < 6 || dto.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
        throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự và tối đa 72 byte UTF-8");
        if (!dto.getPassword().equals(dto.getConfirmPassword()))
        throw new IllegalArgumentException("Mật khẩu xác nhận không đúng");
        Role role = roleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new IllegalStateException("Chưa có ROLE_USER"));
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setEnabled(false);
        user.setRole(role);
        userRepository.save(user);
        otpService.sendRegisterOtp(dto.getEmail());
    }
    @Override @Transactional
    public boolean verifyRegister(String email, String otp) {
        boolean ok = otpService.verifyRegisterOtp(email, otp);
        if (!ok) return false;
        User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        user.setEnabled(true);
        return true;
    }
    @Override @Transactional
    public void forgotPassword(String email) {
        if (!userRepository.existsByEmail(email))
        throw new IllegalArgumentException("Email không tồn tại");
        otpService.sendResetPasswordOtp(email);
    }
    @Override @Transactional
    public boolean resetPassword(String email, String otp, String password) {
        if (password == null || password.length() < 6 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
        throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự và tối đa 72 byte UTF-8");
        User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        if (!otpService.verifyResetPasswordOtp(email, otp)) return false;
        user.setPassword(passwordEncoder.encode(password));
        return true;
    }
    @Override @Transactional
    public void resendRegisterOtp(String email) {
        User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Email chưa đăng ký"));
        if (user.isEnabled()) throw new IllegalArgumentException("Tài khoản đã được kích hoạt");
        otpService.sendRegisterOtp(email);
    }

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, OtpService otpService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }
}
