package com.poly.taskapi.auth.dto;

import com.poly.taskapi.auth.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
    @NotBlank(message = "username is required")
    @Size(max = 64, message = "username must be <= 64 characters")
    String username,

    @NotBlank(message = "password is required")
    @ValidPassword
    String password,

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 320, message = "email must be <= 320 characters")
    String email,

    @NotBlank(message = "firstName is required")
    @Size(max = 100, message = "firstName must be <= 100 characters")
    String firstName,

    @NotBlank(message = "lastName is required")
    @Size(max = 100, message = "lastName must be <= 100 characters")
    String lastName
) {
}
