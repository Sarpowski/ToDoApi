package com.poly.taskapi.storage;


import com.poly.taskapi.common.error.BadRequestException;
import com.poly.taskapi.storage.dto.FileResponseDto;
import com.poly.taskapi.todo.Todo;
import com.poly.taskapi.user.storage.UserStorageService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3MetaDataService {

  private final S3MetaDataRepository repository;
  private final FileStorageService fileStorageService;
  private final UserStorageService userStorageService;

  @Transactional
  public List<FileResponseDto> uploadFiles(UUID userId, Todo todo, List<MultipartFile> files) {
    List<FileResponseDto> result = new ArrayList<>();

    for (MultipartFile file : files) {
      if (file.isEmpty()) {
        continue;
      }

      long fileSize = file.getSize();

      userStorageService.ensureQuotaAvailable(userId, fileSize);

      try {
        String s3Key = fileStorageService.upload(userId, todo.getId(), file);

        S3MetaData metadata = S3MetaData.builder()
            .directory(userId + "/" + todo.getId())
            .fileName(file.getOriginalFilename())
            .s3Key(s3Key)
            .fileSize(fileSize)
            .isDeleted(false)
            .todo(todo)
            .build();

        metadata = repository.save(metadata);

        userStorageService.addUsedBytes(userId, fileSize);

        result.add(toDto(metadata));

        log.debug("Saved file metadata: {} ({} bytes)", file.getOriginalFilename(), fileSize);

      } catch (IOException e) {
        throw new BadRequestException("Failed to upload file: " + file.getOriginalFilename());
      }
    }

    return result;
  }

  @Transactional
  public void deleteByTodo(UUID userId, UUID todoId) {
    List<S3MetaData> files = repository.findByTodoIdAndIsDeletedFalse(todoId);

    for (S3MetaData metadata : files) {
      try {
        fileStorageService.delete(metadata.getS3Key());
      } catch (Exception e) {
        log.warn("Failed to delete S3 object: {}", metadata.getS3Key(), e);
      }

      userStorageService.subtractUsedBytes(userId, metadata.getFileSize());

      metadata.setDeleted(true);
      repository.save(metadata);
    }

    log.debug("Deleted {} files for todo {}", files.size(), todoId);
  }

  @Transactional
  public List<FileResponseDto> getByTodoId(UUID todoId) {
    return repository.findByTodoIdAndIsDeletedFalse(todoId).stream()
        .map(this::toDto)
        .toList();
  }

  private FileResponseDto toDto(S3MetaData metadata) {
    return new FileResponseDto(
        metadata.getId(),
        metadata.getFileName(),
        metadata.getFileSize(),
        metadata.getUploadedAt()
    );
  }
}