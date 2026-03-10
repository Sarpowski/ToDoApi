package com.poly.taskapi.auth;

import com.poly.taskapi.auth.dto.JwtResponseDto;
import com.poly.taskapi.auth.dto.LoginRequestDto;
import com.poly.taskapi.auth.dto.RegisterRequestDto;
import com.poly.taskapi.common.ApiVersion.ApiVersion;
import com.poly.taskapi.user.dto.RegisterResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(ApiVersion.V1 +"/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping(
      "/register")
  public ResponseEntity<RegisterResponseDto> register(
      @Valid @RequestBody RegisterRequestDto request) {
    RegisterResponseDto response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<JwtResponseDto> login(
      @Valid @RequestBody LoginRequestDto request) {
    JwtResponseDto response = authService.login(request);
    return ResponseEntity.ok(response);
  }
}