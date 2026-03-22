package com.poly.taskapi.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poly.taskapi.common.error.BadRequestException;
import com.poly.taskapi.storage.dto.FileResponseDto;
import com.poly.taskapi.todo.Todo;
import com.poly.taskapi.user.storage.UserStorageService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class S3MetaDataServiceTest {

  @Mock
  private S3MetaDataRepository repository;

  @Mock
  private FileStorageService fileStorageService;

  @Mock
  private UserStorageService userStorageService;

  @InjectMocks
  private S3MetaDataService s3MetaDataService;

  private UUID userId;
  private UUID todoId;
  private Todo todo;

  @BeforeEach
  void init() {
    userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    todoId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    todo = Todo.builder().id(todoId).title("t").build();
  }

  @Test
  void uploadFiles_skipsEmptyFilesAndReturnsEmptyList() throws IOException {
    MultipartFile empty = new MockMultipartFile("e", "x.txt", "text/plain", new byte[0]);

    List<FileResponseDto> result = s3MetaDataService.uploadFiles(userId, todo, List.of(empty));

    assertThat(result).isEmpty();
    verify(userStorageService, never()).ensureQuotaAvailable(any(), any(Long.class));
    verify(fileStorageService, never()).upload(any(), any(), any());
    verify(repository, never()).save(any());
  }

  @Test
  void uploadFiles_savesMetadataAddsQuotaAndReturnsDto() throws IOException {
    byte[] data = "ok".getBytes(StandardCharsets.UTF_8);
    MultipartFile file = new MockMultipartFile("f", "a.png", "image/png", data);
    when(fileStorageService.upload(eq(userId), eq(todoId), eq(file))).thenReturn("k1/a.png");

    UUID metaId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    when(repository.save(any(S3MetaData.class))).thenAnswer(inv -> {
      S3MetaData m = inv.getArgument(0);
      m.setId(metaId);
      return m;
    });

    List<FileResponseDto> result = s3MetaDataService.uploadFiles(userId, todo, List.of(file));

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo(metaId);
    assertThat(result.getFirst().fileName()).isEqualTo("a.png");
    assertThat(result.getFirst().fileSize()).isEqualTo(data.length);

    verify(userStorageService).ensureQuotaAvailable(userId, data.length);
    verify(userStorageService).addUsedBytes(userId, data.length);
    verify(repository).save(any(S3MetaData.class));
  }

  @Test
  void uploadFiles_throwsBadRequestWhenUploadFails() throws Exception {
    MultipartFile file = new MockMultipartFile("f", "bad.bin", "application/octet-stream", new byte[]{1});
    when(fileStorageService.upload(userId, todoId, file)).thenThrow(new IOException("network"));

    assertThatThrownBy(() -> s3MetaDataService.uploadFiles(userId, todo, List.of(file)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("bad.bin");
  }

  @Test
  void deleteByTodo_deletesObjectSubtractsBytesAndMarksDeleted() {
    UUID metaId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    S3MetaData meta = S3MetaData.builder()
        .id(metaId)
        .s3Key("s3/key/1")
        .fileName("f.bin")
        .fileSize(100L)
        .isDeleted(false)
        .todo(todo)
        .directory(userId + "/" + todoId)
        .build();
    when(repository.findByTodoIdAndIsDeletedFalse(todoId)).thenReturn(List.of(meta));

    s3MetaDataService.deleteByTodo(userId, todoId);

    verify(fileStorageService).delete("s3/key/1");
    verify(userStorageService).subtractUsedBytes(userId, 100L);
    verify(repository).save(ArgumentMatchers.argThat(m -> m.isDeleted()));
  }

  @Test
  void deleteByTodo_continuesWhenS3DeleteThrows() {
    S3MetaData meta = S3MetaData.builder()
        .id(UUID.randomUUID())
        .s3Key("fragile")
        .fileName("f.bin")
        .fileSize(10L)
        .isDeleted(false)
        .todo(todo)
        .directory("d")
        .build();
    when(repository.findByTodoIdAndIsDeletedFalse(todoId)).thenReturn(List.of(meta));
    doThrow(new RuntimeException("s3 down")).when(fileStorageService).delete("fragile");

    s3MetaDataService.deleteByTodo(userId, todoId);

    verify(userStorageService).subtractUsedBytes(userId, 10L);
    verify(repository).save(any(S3MetaData.class));
  }

  @Test
  void getByTodoId_mapsMetadataToDtos() {
    UUID metaId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    S3MetaData meta = S3MetaData.builder()
        .id(metaId)
        .s3Key("k")
        .fileName("n.txt")
        .fileSize(3L)
        .isDeleted(false)
        .todo(todo)
        .directory("d")
        .build();
    when(repository.findByTodoIdAndIsDeletedFalse(todoId)).thenReturn(List.of(meta));

    List<FileResponseDto> list = s3MetaDataService.getByTodoId(todoId);

    assertThat(list).hasSize(1);
    assertThat(list.getFirst().id()).isEqualTo(metaId);
    assertThat(list.getFirst().fileName()).isEqualTo("n.txt");
    assertThat(list.getFirst().fileSize()).isEqualTo(3L);
  }

  @Test
  void uploadFiles_processesTwoFilesAndAddsQuotaTwice() throws IOException {
    MultipartFile a = new MockMultipartFile("a", "one.txt", "text/plain", "a".getBytes(StandardCharsets.UTF_8));
    MultipartFile b = new MockMultipartFile("b", "two.txt", "text/plain", "bb".getBytes(StandardCharsets.UTF_8));
    when(fileStorageService.upload(eq(userId), eq(todoId), eq(a))).thenReturn("k/one.txt");
    when(fileStorageService.upload(eq(userId), eq(todoId), eq(b))).thenReturn("k/two.txt");
    when(repository.save(any(S3MetaData.class))).thenAnswer(inv -> {
      S3MetaData m = inv.getArgument(0);
      if (m.getFileName().equals("one.txt")) {
        m.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1"));
      } else {
        m.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"));
      }
      return m;
    });

    List<FileResponseDto> result = s3MetaDataService.uploadFiles(userId, todo, List.of(a, b));

    assertThat(result).hasSize(2);
    verify(userStorageService).ensureQuotaAvailable(userId, 1L);
    verify(userStorageService).ensureQuotaAvailable(userId, 2L);
    verify(userStorageService).addUsedBytes(userId, 1L);
    verify(userStorageService).addUsedBytes(userId, 2L);
    verify(repository, times(2)).save(any(S3MetaData.class));
  }

  @Test
  void uploadFiles_skipsLeadingEmptyAndProcessesNext() throws IOException {
    MultipartFile empty = new MockMultipartFile("e", "skip.txt", "text/plain", new byte[0]);
    MultipartFile real = new MockMultipartFile("r", "keep.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));
    when(fileStorageService.upload(eq(userId), eq(todoId), eq(real))).thenReturn("k/keep.txt");
    when(repository.save(any(S3MetaData.class))).thenAnswer(inv -> {
      S3MetaData m = inv.getArgument(0);
      m.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1"));
      return m;
    });

    List<FileResponseDto> result = s3MetaDataService.uploadFiles(userId, todo, List.of(empty, real));

    assertThat(result).hasSize(1);
    verify(fileStorageService, never()).upload(eq(userId), eq(todoId), eq(empty));
    verify(repository, times(1)).save(any(S3MetaData.class));
  }

  @Test
  void uploadFiles_savedEntityUsesUserTodoDirectoryAndOriginalName() throws IOException {
    MultipartFile file = new MockMultipartFile("f", "name.pdf", "application/pdf", new byte[]{9});
    when(fileStorageService.upload(userId, todoId, file)).thenReturn("k/name.pdf");
    when(repository.save(any(S3MetaData.class))).thenAnswer(inv -> {
      S3MetaData m = inv.getArgument(0);
      m.setId(UUID.randomUUID());
      return m;
    });

    s3MetaDataService.uploadFiles(userId, todo, List.of(file));

    verify(repository).save(ArgumentMatchers.argThat(m ->
        m.getDirectory().equals(userId + "/" + todoId)
            && m.getFileName().equals("name.pdf")
            && m.getS3Key().equals("k/name.pdf")
            && m.getFileSize() == 1L
            && m.getTodo() == todo
            && !m.isDeleted()));
  }

  @Test
  void deleteByTodo_whenNoMetadata_skipsStorageAndQuota() {
    when(repository.findByTodoIdAndIsDeletedFalse(todoId)).thenReturn(List.of());

    s3MetaDataService.deleteByTodo(userId, todoId);

    verify(fileStorageService, never()).delete(any());
    verify(userStorageService, never()).subtractUsedBytes(any(), any(Long.class));
    verify(repository, never()).save(any());
  }

  @Test
  void getByTodoId_returnsEmptyWhenRepositoryEmpty() {
    when(repository.findByTodoIdAndIsDeletedFalse(todoId)).thenReturn(List.of());

    assertThat(s3MetaDataService.getByTodoId(todoId)).isEmpty();
  }

  @Test
  void deleteByTodo_processesMultipleFiles() {
    S3MetaData m1 = S3MetaData.builder()
        .id(UUID.randomUUID())
        .s3Key("k1")
        .fileName("a")
        .fileSize(5L)
        .isDeleted(false)
        .todo(todo)
        .directory("d")
        .build();
    S3MetaData m2 = S3MetaData.builder()
        .id(UUID.randomUUID())
        .s3Key("k2")
        .fileName("b")
        .fileSize(7L)
        .isDeleted(false)
        .todo(todo)
        .directory("d")
        .build();
    when(repository.findByTodoIdAndIsDeletedFalse(todoId)).thenReturn(List.of(m1, m2));

    s3MetaDataService.deleteByTodo(userId, todoId);

    verify(fileStorageService).delete("k1");
    verify(fileStorageService).delete("k2");
    verify(userStorageService).subtractUsedBytes(userId, 5L);
    verify(userStorageService).subtractUsedBytes(userId, 7L);
    verify(repository, times(2)).save(any(S3MetaData.class));
  }
}
