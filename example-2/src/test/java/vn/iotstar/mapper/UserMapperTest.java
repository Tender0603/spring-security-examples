package vn.iotstar.mapper;

import org.junit.jupiter.api.Test;

import vn.iotstar.dto.UserDTO;
import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    @Test
    void mapperIncludesProfileAndRole() {

        // Tạo role
        Role role = new Role();
        role.setName("ROLE_USER");

        // Tạo user
        User user = new User();
        user.setId(5L);
        user.setUsername("user01");
        user.setEmail("user01@gmail.com");
        user.setFullName("Nguyễn Hữu Trung");
        user.setImages("/images/user.svg");
        user.setEnabled(true);
        user.setRole(role);

        // Mapper hiện tại là class @Component, không còn là MapStruct interface
        UserMapper mapper = new UserMapper();

        UserDTO dto = mapper.toDTO(user);

        assertAll(
                () -> assertEquals(user.getId(), dto.getId()),
                () -> assertEquals(user.getUsername(), dto.getUsername()),
                () -> assertEquals(user.getEmail(), dto.getEmail()),
                () -> assertEquals(user.getFullName(), dto.getFullName()),
                () -> assertEquals(user.getImages(), dto.getImages()),
                () -> assertEquals("ROLE_USER", dto.getRoleName()),
                () -> assertTrue(dto.isEnabled())
        );
    }
}