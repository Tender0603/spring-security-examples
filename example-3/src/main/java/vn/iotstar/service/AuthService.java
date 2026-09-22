package vn.iotstar.service;
import vn.iotstar.dto.RegisterDTO;
public interface AuthService {
    void register(RegisterDTO dto);
    boolean verifyRegister(String email, String otp);
    void forgotPassword(String email);
    boolean resetPassword(String email, String otp, String password);
    void resendRegisterOtp(String email);
}
