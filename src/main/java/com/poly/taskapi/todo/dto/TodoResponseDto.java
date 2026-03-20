package com.poly.taskapi.todo.dto;

import com.poly.taskapi.storage.dto.FileResponseDto;
import com.poly.taskapi.todo.todoEnum.Priority;
import com.poly.taskapi.todo.todoEnum.RepeatType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TodoResponseDto(
    UUID id,
    String todoName,
    String todoContent,
    Instant deadline,
    boolean done,
    Priority priority,
    RepeatType repeatType,
    UUID todoUserId,
    List<FileResponseDto> attachments,
    Instant createdAt,
    Instant updatedAt
) {
}