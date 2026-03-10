package com.poly.taskapi.todo;

import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, UUID> {

  Optional<Todo> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

  Page<Todo> findByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);
}
