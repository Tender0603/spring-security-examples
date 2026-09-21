package vn.iotstar.mapper;

import org.springframework.stereotype.Component;

import vn.iotstar.dto.UserDTO;
import vn.iotstar.entity.User;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();

        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setImages(user.getImages());
        dto.setEnabled(user.isEnabled());

        if (user.getRole() != null) {
            dto.setRoleName(user.getRole().getName());
        }

        return dto;
    }
}