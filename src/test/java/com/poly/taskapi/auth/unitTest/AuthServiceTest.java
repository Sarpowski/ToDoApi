package com.poly.taskapi.auth.unitTest;

import com.poly.taskapi.auth.AuthService;
import com.poly.taskapi.auth.JwtService;
import com.poly.taskapi.auth.dto.JwtResponseDto;
import com.poly.taskapi.auth.dto.LoginRequestDto;
import com.poly.taskapi.auth.dto.RegisterRequestDto;
import com.poly.taskapi.common.error.ConflictException;
import com.poly.taskapi.user.User;
import com.poly.taskapi.user.UserRepository;
import com.poly.taskapi.user.dto.RegisterResponseDto;
import com.poly.taskapi.user.storage.UserStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserStorageService userStorageService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private RegisterRequestDto registerRequest(String username, String email) {
        return new RegisterRequestDto(username, "Test123!@", email, "John", "Doe");
    }

    private User savedUser(String username, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("encoded");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setCreatedAt(Instant.now());
        return user;
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully")
        void registerSuccess() {
            var request = registerRequest("john", "john@test.com");
            User user = savedUser("john", "john@test.com");

            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
            when(passwordEncoder.encode("Test123!@")).thenReturn("encoded");
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

            RegisterResponseDto result = authService.register(request);

            assertThat(result.id()).isEqualTo(user.getId());
            assertThat(result.username()).isEqualTo("john");
            assertThat(result.email()).isEqualTo("john@test.com");
            verify(userStorageService).initializeForUser(user.getId());
        }

        @Test
        @DisplayName("Should encode password before saving")
        void registerEncodesPassword() {
            var request = registerRequest("john", "john@test.com");
            User user = savedUser("john", "john@test.com");

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("Test123!@")).thenReturn("bcrypt_hash");
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt_hash");
        }

        @Test
        @DisplayName("Should throw ConflictException for duplicate username")
        void registerDuplicateUsername() {
            var request = registerRequest("john", "john@test.com");
            when(userRepository.existsByUsername("john")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username already exists");

            verify(userRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Should throw ConflictException for duplicate email")
        void registerDuplicateEmail() {
            var request = registerRequest("john", "john@test.com");
            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");

            verify(userRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Should initialize storage for new user")
        void registerInitializesStorage() {
            var request = registerRequest("john", "john@test.com");
            User user = savedUser("john", "john@test.com");

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

            authService.register(request);

            verify(userStorageService).initializeForUser(user.getId());
        }

        @Test
        @DisplayName("Should set all user fields from request")
        void registerSetsAllFields() {
            var request = new RegisterRequestDto("john", "Test123!@", "john@test.com", "John", "Doe");
            User user = savedUser("john", "john@test.com");

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).saveAndFlush(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getUsername()).isEqualTo("john");
            assertThat(saved.getEmail()).isEqualTo("john@test.com");
            assertThat(saved.getFirstName()).isEqualTo("John");
            assertThat(saved.getLastName()).isEqualTo("Doe");
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void loginSuccess() {
            User user = savedUser("john", "john@test.com");
            when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Test123!@", "encoded")).thenReturn(true);
            when(jwtService.generateToken(user.getId(), "john")).thenReturn("jwt_token");

            var request = new LoginRequestDto("john", "Test123!@");
            JwtResponseDto result = authService.login(request);

            assertThat(result.token()).isEqualTo("jwt_token");
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for non-existent user")
        void loginUserNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            var request = new LoginRequestDto("unknown", "Test123!@");
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for wrong password")
        void loginWrongPassword() {
            User user = savedUser("john", "john@test.com");
            when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

            var request = new LoginRequestDto("john", "wrong");
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateToken(any(), any());
        }

        @Test
        @DisplayName("Should generate token with correct userId and username")
        void loginGeneratesCorrectToken() {
            User user = savedUser("john", "john@test.com");
            when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateToken(any(), anyString())).thenReturn("token");

            authService.login(new LoginRequestDto("john", "Test123!@"));

            verify(jwtService).generateToken(user.getId(), "john");
        }
    }
}
