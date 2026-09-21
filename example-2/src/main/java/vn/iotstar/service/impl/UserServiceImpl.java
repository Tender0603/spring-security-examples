package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;

import vn.iotstar.dto.UserDTO;
import vn.iotstar.entity.User;
import vn.iotstar.mapper.UserMapper;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // Constructor thủ công, không dùng Lombok
    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper) {

        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserDTO findById(Long id) {

        User user = userRepository
                .findById(id)
                .orElseThrow();

        return userMapper.toDTO(user);
    }
}