package com.poly.taskapi.user.dto;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponseDto(
    UUID id,
    String username,
    String email,
    Instant createdAt
) {
}
