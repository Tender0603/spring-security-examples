package vn.iotstar.mapper;
import org.springframework.stereotype.Component;
import vn.iotstar.dto.UserDTO;
import vn.iotstar.entity.User;
/** Equivalent to the PDF mapping, without annotation processing in STS. */
@Component
public class UserMapper {
    public UserDTO toDTO(User entity) {
        if (entity == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setFullName(entity.getFullName());
        dto.setEnabled(entity.isEnabled());
        dto.setRoleName(entity.getRole() == null ? null : entity.getRole().getName());
        return dto;
    }
    public User toEntity(UserDTO dto) {
        if (dto == null) return null;
        User entity = new User();
        entity.setId(dto.getId());
        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setFullName(dto.getFullName());
        entity.setEnabled(dto.isEnabled());
        return entity;
    }
}
