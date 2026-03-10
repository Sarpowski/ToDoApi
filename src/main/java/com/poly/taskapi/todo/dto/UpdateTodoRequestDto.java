package com.poly.taskapi.todo.dto;

import com.poly.taskapi.todo.todoEnum.Priority;
import com.poly.taskapi.todo.todoEnum.RepeatType;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Builder;

@Builder
public record UpdateTodoRequestDto(
    @Size(max = 128, message = "title must be <= 128 characters")
    String title,

    @Size(max = 2048, message = "content must be <= 2048 characters")
    String content,

    Instant deadline,

    Boolean done,

    Priority priority,

    RepeatType repeatType
) {
}
