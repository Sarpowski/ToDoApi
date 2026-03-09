package com.poly.taskapi.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
  UUID id;

  @Column(name = "directory", nullable = false, updatable = true)
  String directory;

  @Column(name = "file_name", nullable = false, updatable = true)
  String fileName;

  @Column(name = "s3_key", nullable = false,updatable = false)
  String s3Key;

  @Column(name = "file_size", nullable = false)
  Long fileSize;

  @UpdateTimestamp
  Instant uploadedAt;

  @Column(name = "is_deleted")
  boolean isDeleted;
}
