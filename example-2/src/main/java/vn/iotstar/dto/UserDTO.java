package vn.iotstar.dto;

public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String images;
    private String roleName;
    private boolean enabled;

    public UserDTO() {
    }

    public UserDTO(
            Long id,
            String username,
            String email,
            String fullName,
            String images,
            String roleName,
            boolean enabled) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.images = images;
        this.roleName = roleName;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}