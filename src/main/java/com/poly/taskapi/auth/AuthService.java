package com.poly.taskapi.auth;

import com.poly.taskapi.auth.dto.JwtResponseDto;
import com.poly.taskapi.auth.dto.LoginRequestDto;
import com.poly.taskapi.auth.dto.RegisterRequestDto;
import com.poly.taskapi.common.error.ConflictException;
import com.poly.taskapi.user.UserRepository;
import com.poly.taskapi.user.dto.RegisterResponseDto;
import com.poly.taskapi.user.storage.UserStorageService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import com.poly.taskapi.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final UserStorageService userStorageService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Transactional
  public RegisterResponseDto register(RegisterRequestDto request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new ConflictException("Username already exists");
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("Email already exists");
    }

    User user = new User();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());

    user = userRepository.save(user);

    userStorageService.initializeForUser(user.getId());

    return new RegisterResponseDto(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getCreatedAt()
    );
  }

  public JwtResponseDto login(LoginRequestDto request) {
    User user = userRepository.findByUsername(request.username())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }

    String token = jwtService.generateToken(user.getId(), user.getUsername());
    return new JwtResponseDto(token);
  }
}