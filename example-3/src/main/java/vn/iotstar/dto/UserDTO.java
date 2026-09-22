package vn.iotstar.dto;
import jakarta.validation.constraints.*;
public class UserDTO {
    private Long id;
    @NotBlank(message = "Username không được để trống")
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "Username chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang")
    private String username;
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150)
    private String email;
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 500)
    private String fullName;
    private boolean enabled;
    @NotBlank
    @Pattern(regexp = "ROLE_USER|ROLE_ADMIN")
    private String roleName;
    private long productCount;

    public UserDTO() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public long getProductCount() { return productCount; }
    public void setProductCount(long productCount) { this.productCount = productCount; }
}
