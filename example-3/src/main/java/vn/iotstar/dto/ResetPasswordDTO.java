package vn.iotstar.dto;
import jakarta.validation.constraints.*;
public class ResetPasswordDTO {
    @NotBlank
    @Pattern(regexp = "[0-9]{6}", message = "OTP phải gồm 6 chữ số")
    private String otp;
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    @NotBlank @Email
    private String email;
    @NotBlank
    @Size(min = 6, max = 72)
    private String password;
    @NotBlank
    private String confirmPassword;

    public ResetPasswordDTO() {}
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
