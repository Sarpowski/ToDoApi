package com.poly.taskapi.storage;

import com.poly.taskapi.todo.Todo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "s3_metadata")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class S3MetaData {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "directory", nullable = false, updatable = true)
  private String directory;

  @Column(name = "file_name", nullable = false, updatable = true)
  private String fileName;

  @Column(name = "s3_key", nullable = false, updatable = false)
  private String s3Key;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @UpdateTimestamp
  private Instant uploadedAt;

  @Column(name = "is_deleted")
  private boolean isDeleted;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "todo_id", nullable = false)
  private Todo todo;
}
