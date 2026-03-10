package com.poly.taskapi.todo.dto;

import com.poly.taskapi.todo.todoEnum.Priority;
import com.poly.taskapi.todo.todoEnum.RepeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Builder;


//TODO add multipart support S3
@Builder
public record CreateTodoRequestDto
    (
        @NotBlank(message = "title is required")
        @Size(max = 128, message = "title must be <= 128 characters")
        String title,

        @Size(max = 2048, message = "content must be <= 2048 characters")
        String content,

        Instant deadline,

        Priority priority,

        RepeatType repeatType
    ) {
}
