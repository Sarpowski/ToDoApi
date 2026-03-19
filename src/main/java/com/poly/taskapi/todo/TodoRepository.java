package com.poly.taskapi.todo;

import com.poly.taskapi.todo.todoEnum.Priority;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, UUID> {

  Optional<Todo> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

  Page<Todo> findByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

  Page<Todo> findByUserIdAndIsDeletedFalseAndTitleContainingIgnoreCase(
      UUID userId, String title, Pageable pageable);

  Page<Todo> findByUserIdAndIsDeletedFalseAndDone(
      UUID userId, boolean done, Pageable pageable);

  Page<Todo> findByUserIdAndIsDeletedFalseAndPriority(
      UUID userId, Priority priority, Pageable pageable);

  Page<Todo> findByUserIdAndIsDeletedFalseAndDoneAndPriority(
      UUID userId, boolean done, Priority priority, Pageable pageable);


  Page<Todo> findByUserIdAndIsDeletedFalseAndDoneFalseAndDeadlineBefore(
      UUID userId, Instant before, Pageable pageable);
}
