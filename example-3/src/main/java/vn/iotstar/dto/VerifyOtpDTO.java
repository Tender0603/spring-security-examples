package vn.iotstar.dto;
import jakarta.validation.constraints.*;
public class VerifyOtpDTO {
    @NotBlank @Email
    private String email;
    @NotBlank
    @Pattern(regexp = "[0-9]{6}", message = "OTP phải gồm 6 chữ số")
    private String otp;

    public VerifyOtpDTO() {}
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
