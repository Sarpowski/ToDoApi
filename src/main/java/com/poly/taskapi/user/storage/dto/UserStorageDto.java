package com.poly.taskapi.user.storage.dto;

import java.time.Instant;
import java.util.UUID;

public record UserStorageDto(
    UUID userId,
    long totalUsedBytes,
    long quotaLimitBytes,
    Instant updatedAt,
    Instant createdAt
) {
}
