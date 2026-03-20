package com.poly.taskapi.todo;

import com.poly.taskapi.todo.todoEnum.Priority;
import com.poly.taskapi.todo.todoEnum.RepeatType;
import com.poly.taskapi.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "todos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Todo {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  @Column(name = "title", nullable = false, length = 128)
  String title;

  @Column(name = "content", length = 2048)
  String content;

  @Column(name = "deadline")
  Instant deadline;

  @Column(name = "done", nullable = false)
  boolean done = false;

  @Column(name = "is_deleted", nullable = false)
  boolean isDeleted;

  @UpdateTimestamp
  @Column(name = "updated_at")
  Instant updatedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority")
  Priority priority;

  @Enumerated(EnumType.STRING)
  @Column(name = "repeat_type")
  private RepeatType repeatType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_todo_id")
  private Todo parentTodo;

}
