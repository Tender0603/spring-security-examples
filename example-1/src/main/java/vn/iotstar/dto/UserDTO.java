package vn.iotstar.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserDTO - theo tai lieu Vi du 1 trang 10
 * Co them productCount (so san pham cua user)
 */
@Data
public class UserDTO {
    private Long id;
    @Email @NotBlank
    private String email;
    @NotBlank
    private String fullName;
    @NotNull
    private Long roleId;
    private String roleName;
    private boolean enabled;
    private long productCount;       // them theo tai lieu trang 10
    private LocalDateTime createdAt;
}