package com.poly.taskapi.todo.dto;

import java.util.List;

public record TodoResponsePageableDto (
    List<TodoResponseDto> items,
    int totalPages,
    long totalElements,
    int currentPage,
    int pageSize
) {
}
