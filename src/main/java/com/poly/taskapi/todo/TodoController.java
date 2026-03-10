package com.poly.taskapi.todo;


import com.poly.taskapi.common.ApiVersion.ApiVersion;
import com.poly.taskapi.todo.dto.CreateTodoRequestDto;
import com.poly.taskapi.todo.dto.TodoResponseDto;
import com.poly.taskapi.todo.dto.TodoResponsePageableDto;
import com.poly.taskapi.todo.dto.UpdateTodoRequestDto;
import jakarta.validation.Valid;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping(ApiVersion.V1 + "/todos")
@RequiredArgsConstructor
public class TodoController {

  private final TodoService todoService;

  @PostMapping
  public ResponseEntity<TodoResponseDto> create(
      @Valid @RequestBody CreateTodoRequestDto request) {
    TodoResponseDto response = todoService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<TodoResponsePageableDto> findAll(
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
      Pageable pageable) {
    TodoResponsePageableDto response = todoService.findAll(pageable);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{todoId}")
  public ResponseEntity<TodoResponseDto> findById(@PathVariable UUID todoId) {
    TodoResponseDto response = todoService.findById(todoId);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{todoId}")
  public ResponseEntity<TodoResponseDto> update(
      @PathVariable UUID todoId,
      @Valid @RequestBody UpdateTodoRequestDto request) {
    TodoResponseDto response = todoService.update(todoId, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{todoId}")
  public ResponseEntity<Void> delete(@PathVariable UUID todoId) {
    todoService.delete(todoId);
    return ResponseEntity.noContent().build();
  }
}
