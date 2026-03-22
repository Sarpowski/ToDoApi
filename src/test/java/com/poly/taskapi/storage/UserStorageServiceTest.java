package com.poly.taskapi.storage;

import com.poly.taskapi.common.error.BadRequestException;
import com.poly.taskapi.common.error.NotFoundException;
import com.poly.taskapi.user.storage.UserStorage;
import com.poly.taskapi.user.storage.UserStorageRepository;
import com.poly.taskapi.user.storage.UserStorageService;
import com.poly.taskapi.user.storage.dto.UserStorageDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("integration")
class UserStorageServiceTest {

    private final UserStorageRepository repository = mock(UserStorageRepository.class);
    private final long quotaLimit = 1610612736L; // 1.5 GiB
    private final UserStorageService service = new UserStorageService(repository, quotaLimit);

    private UserStorage buildStorage(UUID userId, long usedBytes) {
        UserStorage s = new UserStorage();
        s.setUserId(userId);
        s.setTotalUsedBytes(usedBytes);
        s.setQuotaLimitBytes(quotaLimit);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }

    @Nested
    @DisplayName("initializeForUser()")
    class InitializeTests {

        @Test
        @DisplayName("Should create storage for new user")
        void initializeNewUser() {
            UUID userId = UUID.randomUUID();
            when(repository.findByUserId(userId)).thenReturn(Optional.empty());

            service.initializeForUser(userId);

            ArgumentCaptor<UserStorage> captor = ArgumentCaptor.forClass(UserStorage.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(userId);
            assertThat(captor.getValue().getTotalUsedBytes()).isZero();
            assertThat(captor.getValue().getQuotaLimitBytes()).isEqualTo(quotaLimit);
        }

        @Test
        @DisplayName("Should skip if storage already exists")
        void initializeExistingUser() {
            UUID userId = UUID.randomUUID();
            when(repository.findByUserId(userId)).thenReturn(Optional.of(buildStorage(userId, 0)));

            service.initializeForUser(userId);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("Should return storage DTO")
        void getSuccess() {
            UUID userId = UUID.randomUUID();
            UserStorage storage = buildStorage(userId, 1024);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(storage));

            UserStorageDto dto = service.get(userId);

            assertThat(dto.userId()).isEqualTo(userId);
            assertThat(dto.totalUsedBytes()).isEqualTo(1024);
            assertThat(dto.quotaLimitBytes()).isEqualTo(quotaLimit);
        }

        @Test
        @DisplayName("Should throw NotFoundException when not found")
        void getNotFound() {
            UUID userId = UUID.randomUUID();
            when(repository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(userId))
                .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("ensureQuotaAvailable()")
    class QuotaTests {

        @Test
        @DisplayName("Should pass when quota available")
        void quotaAvailable() {
            UUID userId = UUID.randomUUID();
            UserStorage storage = buildStorage(userId, 0);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(storage));

            assertThatCode(() -> service.ensureQuotaAvailable(userId, 1024))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw BadRequestException when quota exceeded")
        void quotaExceeded() {
            UUID userId = UUID.randomUUID();
            UserStorage storage = buildStorage(userId, quotaLimit - 100);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(storage));

            assertThatThrownBy(() -> service.ensureQuotaAvailable(userId, 200))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Storage quota exceeded");
        }

        @Test
        @DisplayName("Should throw BadRequestException for negative bytes")
        void negativeBytesToAdd() {
            UUID userId = UUID.randomUUID();
            assertThatThrownBy(() -> service.ensureQuotaAvailable(userId, -1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid file size");
        }

        @Test
        @DisplayName("Should pass when exactly at quota boundary")
        void exactlyAtQuota() {
            UUID userId = UUID.randomUUID();
            UserStorage storage = buildStorage(userId, quotaLimit - 100);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(storage));

            assertThatCode(() -> service.ensureQuotaAvailable(userId, 100))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when 1 byte over quota")
        void oneByteOverQuota() {
            UUID userId = UUID.randomUUID();
            UserStorage storage = buildStorage(userId, quotaLimit);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(storage));

            assertThatThrownBy(() -> service.ensureQuotaAvailable(userId, 1))
                .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("addUsedBytes()")
    class AddBytesTests {

        @Test
        @DisplayName("Should add bytes and save")
        void addBytesSuccess() {
            UUID userId = UUID.randomUUID();
            UserStorage storage = buildStorage(userId, 1000);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(storage));

            service.addUsedBytes(userId, 500);

            assertThat(storage.getTotalUsedBytes()).isEqualTo(1500);
            verify(repository).save(storage);
        }

        @Test
        @DisplayName("Should throw when adding would exceed quota")
        void addBytesExceedsQuota() {
            UUID userId = UUID.randomUUID();
            UserStorage storage = buildStorage(userId, quotaLimit);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(storage));

            assertThatThrownBy(() -> service.addUsedBytes(userId, 1))
                .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("subtractUsedBytes()")
    class SubtractBytesTests {

        @Test
        @DisplayName("Should subtract bytes and save")
        void subtractBytesSuccess() {
            UUID userId = UUID.randomUUID();
            UserStorage storage = buildStorage(userId, 1000);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(storage));

            service.subtractUsedBytes(userId, 500);

            assertThat(storage.getTotalUsedBytes()).isEqualTo(500);
            verify(repository).save(storage);
        }

        @Test
        @DisplayName("Should clamp to zero when subtracting more than available")
        void subtractClampToZero() {
            UUID userId = UUID.randomUUID();
            UserStorage storage = buildStorage(userId, 100);
            when(repository.findByUserId(userId)).thenReturn(Optional.of(storage));

            service.subtractUsedBytes(userId, 500);

            assertThat(storage.getTotalUsedBytes()).isZero();
        }
    }
}
