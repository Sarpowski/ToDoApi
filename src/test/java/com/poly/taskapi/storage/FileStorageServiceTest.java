package com.poly.taskapi.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class FileStorageServiceTest {

  @Mock
  private S3Client s3Client;

  @InjectMocks
  private FileStorageService fileStorageService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(fileStorageService, "bucket", "test-bucket");
  }

  @Test
  void upload_sendsPutObjectWithBucketContentTypeAndKeyUnderUserAndTodo() throws IOException {
    UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID todoId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    byte[] bytes = "payload".getBytes(StandardCharsets.UTF_8);
    MultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", bytes);

    String key = fileStorageService.upload(userId, todoId, file);

    assertThat(key).startsWith(userId + "/" + todoId + "/");
    assertThat(key).endsWith("_doc.txt");

    ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
    verify(s3Client).putObject(putCaptor.capture(), bodyCaptor.capture());

    PutObjectRequest req = putCaptor.getValue();
    assertThat(req.bucket()).isEqualTo("test-bucket");
    assertThat(req.key()).isEqualTo(key);
    assertThat(req.contentType()).isEqualTo("text/plain");
    assertThat(req.contentLength()).isEqualTo(bytes.length);
  }

  @Test
  void delete_sendsDeleteObjectWithBucketAndKey() {
    fileStorageService.delete("user/todo/file-key");

    ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(captor.capture());
    DeleteObjectRequest req = captor.getValue();
    assertThat(req.bucket()).isEqualTo("test-bucket");
    assertThat(req.key()).isEqualTo("user/todo/file-key");
  }

  @Test
  void upload_propagatesSdkClientExceptionWhenPutObjectFails() {
    UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    UUID todoId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    MultipartFile file = new MockMultipartFile("file", "a.bin", "application/octet-stream", new byte[]{1});
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(SdkClientException.create("s3 unavailable"));

    assertThatThrownBy(() -> fileStorageService.upload(userId, todoId, file))
        .isInstanceOf(SdkClientException.class)
        .hasMessageContaining("s3 unavailable");
  }

  @Test
  void upload_propagatesIOExceptionWhenInputStreamFails() throws Exception {
    UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    UUID todoId = UUID.fromString("66666666-6666-6666-6666-666666666666");
    MultipartFile file = mock(MultipartFile.class);
    when(file.getSize()).thenReturn(2L);
    when(file.getOriginalFilename()).thenReturn("broken.txt");
    when(file.getContentType()).thenReturn("text/plain");
    when(file.getInputStream()).thenThrow(new IOException("read failed"));

    assertThatThrownBy(() -> fileStorageService.upload(userId, todoId, file))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("read failed");
    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void upload_sendsNullContentTypeWhenMultipartHasNone() throws IOException {
    UUID userId = UUID.fromString("77777777-7777-7777-7777-777777777777");
    UUID todoId = UUID.fromString("88888888-8888-8888-8888-888888888888");
    MultipartFile file = new MockMultipartFile("f", "raw.dat", null, new byte[]{0});

    fileStorageService.upload(userId, todoId, file);

    ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
    assertThat(putCaptor.getValue().contentType()).isNull();
  }
}
