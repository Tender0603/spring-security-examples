package vn.iotstar.dto;
import jakarta.validation.constraints.*;
public class RegisterDTO {
    @NotBlank(message = "Username không được để trống")
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "Username chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang")
    private String username;
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150)
    private String email;
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 72, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;
    @NotBlank(message = "Xác nhận mật khẩu")
    private String confirmPassword;
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 500)
    private String fullName;

    public RegisterDTO() {}
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
