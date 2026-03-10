package com.poly.taskapi.todo;

import com.poly.taskapi.common.error.BadRequestException;
import com.poly.taskapi.common.error.NotFoundException;
import com.poly.taskapi.common.security.CurrentUser;
import com.poly.taskapi.todo.dto.CreateTodoRequestDto;
import com.poly.taskapi.todo.dto.TodoResponseDto;
import com.poly.taskapi.todo.dto.TodoResponsePageableDto;
import com.poly.taskapi.todo.dto.UpdateTodoRequestDto;
import com.poly.taskapi.todo.todoEnum.Priority;
import com.poly.taskapi.user.User;
import com.poly.taskapi.user.UserRepository;
import jakarta.transaction.Transactional;
import java.awt.print.Pageable;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TodoService {

  private final TodoRepository todoRepository;
  private final UserRepository userRepository;

  @Transactional
  public TodoResponseDto create(CreateTodoRequestDto request) {
    UUID userId = CurrentUser.requireUserId();

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found"));

    if (request.deadline() != null && request.deadline().isBefore(Instant.now())) {
      throw new BadRequestException("Deadline must be in the future");
    }

    Todo todo = Todo.builder()
        .title(request.title())
        .content(request.content())
        .deadline(request.deadline())
        .done(false)
        .isDeleted(false)
        .priority(request.priority() != null ? request.priority() : Priority.NONE)
        .repeatType(request.repeatType())
        .user(user)
        .build();

    todo = todoRepository.save(todo);
    return toDto(todo);
  }

  @Transactional
  public TodoResponseDto findById(UUID todoId) {
    UUID userId = CurrentUser.requireUserId();

    Todo todo = todoRepository.findByIdAndUserIdAndIsDeletedFalse(todoId, userId)
        .orElseThrow(() -> new NotFoundException("Todo not found"));

    return toDto(todo);
  }

  @Transactional
  public TodoResponsePageableDto findAll(Pageable pageable) {
    UUID userId = CurrentUser.requireUserId();

    Page<Todo> page = todoRepository.findByUserIdAndIsDeletedFalse(userId, pageable);

    return new TodoResponsePageableDto(
        page.getContent().stream().map(this::toDto).toList(),
        page.getTotalPages(),
        page.getTotalElements(),
        page.getNumber(),
        page.getSize()
    );
  }

  @Transactional
  public TodoResponseDto update(UUID todoId, UpdateTodoRequestDto request) {
    UUID userId = CurrentUser.requireUserId();

    Todo todo = todoRepository.findByIdAndUserIdAndIsDeletedFalse(todoId, userId)
        .orElseThrow(() -> new NotFoundException("Todo not found"));

    if (request.title() != null) {
      if (request.title().isBlank()) {
        throw new BadRequestException("Title cannot be blank");
      }
      todo.setTitle(request.title());
    }
    if (request.content() != null) {
      todo.setContent(request.content());
    }
    if (request.deadline() != null) {
      if (request.deadline().isBefore(Instant.now())) {
        throw new BadRequestException("Deadline must be in the future");
      }
      todo.setDeadline(request.deadline());
    }
    if (request.done() != null) {
      todo.setDone(request.done());
    }
    if (request.priority() != null) {
      todo.setPriority(request.priority());
    }
    if (request.repeatType() != null) {
      todo.setRepeatType(request.repeatType());
    }

    todo = todoRepository.save(todo);
    return toDto(todo);
  }

  @Transactional
  public boolean delete(UUID todoId) {
    UUID userId = CurrentUser.requireUserId();

    Todo todo = todoRepository.findByIdAndUserIdAndIsDeletedFalse(todoId, userId)
        .orElseThrow(() -> new NotFoundException("Todo not found"));

    todo.setDeleted(true);
    todoRepository.save(todo);
    return true;
  }

  private TodoResponseDto toDto(Todo todo) {
    return new TodoResponseDto(
        todo.getId(),
        todo.getTitle(),
        todo.getContent(),
        todo.getDeadline(),
        todo.isDone(),
        todo.getPriority(),
        todo.getRepeatType(),
        todo.getUser().getId(),
        todo.getCreatedAt(),
        todo.getUpdatedAt()
    );
  }
}
