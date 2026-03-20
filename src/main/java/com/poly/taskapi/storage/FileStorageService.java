package com.poly.taskapi.storage;


import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

  private final S3Client s3Client;

  @Value("${app.storage.s3.bucket}")
  private String bucket;

  public String upload(UUID userId, UUID todoId, MultipartFile file) throws IOException {
    String s3Key =
        userId + "/" + todoId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucket)
        .key(s3Key)
        .contentType(file.getContentType())
        .contentLength(file.getSize())
        .build();

    s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

    log.debug("Uploaded file to S3: {}", s3Key);
    return s3Key;
  }

  public void delete(String s3Key) {
    DeleteObjectRequest request = DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(s3Key)
        .build();

    s3Client.deleteObject(request);
    log.debug("Deleted file from S3: {}", s3Key);
  }
}
