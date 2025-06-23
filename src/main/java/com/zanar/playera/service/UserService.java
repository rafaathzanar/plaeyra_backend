package com.zanar.playera.service;

import com.zanar.playera.dto.UserLoginDTO;
import com.zanar.playera.dto.UserRegistrationDTO;
import com.zanar.playera.dto.UserResponseDTO;
import com.zanar.playera.entity.User;
import com.zanar.playera.mapper.UserMapper;
import com.zanar.playera.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;

  public UserResponseDTO registerUser(UserRegistrationDTO dto) {
    if (userRepository.findByEmail(dto.getEmail()) != null) {
      throw new RuntimeException("Email already registered");
    }
    User user = UserMapper.toUserEntity(dto);
    user.setPassword(passwordEncoder.encode(dto.getPassword()));
    User saved = userRepository.save(user);
    return UserMapper.toUserResponseDTO(saved);
  }

  public UserResponseDTO loginUser(UserLoginDTO dto) {
    User user = userRepository.findByEmail(dto.getEmail());
    if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
      throw new RuntimeException("Invalid email or password");
    }
    return UserMapper.toUserResponseDTO(user);
  }

  public UserResponseDTO getUserById(Long id) {
    User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    return UserMapper.toUserResponseDTO(user);
  }

  public UserResponseDTO getUserByEmail(String email) {
    User user = userRepository.findByEmail(email);
    if (user == null)
      throw new RuntimeException("User not found");
    return UserMapper.toUserResponseDTO(user);
  }

  public List<UserResponseDTO> listUsers() {
    return userRepository.findAll().stream().map(UserMapper::toUserResponseDTO).collect(Collectors.toList());
  }
}
