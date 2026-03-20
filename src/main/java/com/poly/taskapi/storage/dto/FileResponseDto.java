package com.poly.taskapi.storage.dto;


import java.time.Instant;
import java.util.UUID;

public record FileResponseDto(
    UUID id,
    String fileName,
    long fileSize,
    Instant createdAt
) {
}