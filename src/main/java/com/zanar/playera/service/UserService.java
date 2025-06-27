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

  // Simulated in-memory token store for demo purposes
  private final java.util.Map<String, String> passwordResetTokens = new java.util.HashMap<>();

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

  public void requestPasswordReset(String email) {
    var userOpt = userRepository.findAll().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    if (userOpt.isEmpty()) throw new RuntimeException("User not found");
    // Generate a simple token (in production, use a secure random token)
    String token = java.util.UUID.randomUUID().toString();
    passwordResetTokens.put(token, email);
    // Simulate sending email (in production, send real email)
    System.out.println("Password reset link: https://your-app/reset-password?token=" + token);
  }

  public void resetPassword(String token, String newPassword) {
    String email = passwordResetTokens.get(token);
    if (email == null) throw new RuntimeException("Invalid or expired reset token");
    var userOpt = userRepository.findAll().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    if (userOpt.isEmpty()) throw new RuntimeException("User not found");
    var user = userOpt.get();
    user.setPassword(newPassword); // In production, hash the password!
    userRepository.save(user);
    passwordResetTokens.remove(token);
  }
}
