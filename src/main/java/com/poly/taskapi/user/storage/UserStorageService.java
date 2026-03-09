package com.poly.taskapi.user.storage;

import com.poly.taskapi.common.error.BadRequestException;
import com.poly.taskapi.common.error.NotFoundException;
import com.poly.taskapi.user.storage.dto.UserStorageDto;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserStorageService {

  private final UserStorageRepository repository;
  private final long quotaLimitBytes;

  public UserStorageService(UserStorageRepository repository,
      @Value("${app.storage.quotaLimitBytes}") long quotaLimitBytes) {
    this.repository = repository;
    this.quotaLimitBytes = quotaLimitBytes;
  }

  @Transactional
  public void initializeForUser(UUID userId) {
    if (repository.findByUserId(userId).isPresent()) {
      return;
    }
    UserStorage storage = new UserStorage();
    storage.setUserId(userId);
    storage.setTotalUsedBytes(0);
    storage.setQuotaLimitBytes(quotaLimitBytes);
    repository.save(storage);
  }

  @Transactional(readOnly = true)
  public UserStorageDto get(UUID userId) {
    UserStorage storage = repository.findByUserId(userId)
        .orElseThrow(() -> new NotFoundException("User storage not found"));
    return toDto(storage);
  }

  @Transactional
  public void ensureQuotaAvailable(UUID userId, long bytesToAdd) {
    if (bytesToAdd < 0) {
      throw new BadRequestException("Invalid file size");
    }
    UserStorage storage = repository.findByUserId(userId)
        .orElseThrow(() -> new NotFoundException("User storage not found"));
    long newTotal = storage.getTotalUsedBytes() + bytesToAdd;
    if (newTotal > storage.getQuotaLimitBytes()) {
      throw new BadRequestException("Storage quota exceeded");
    }
  }

  @Transactional
  public void addUsedBytes(UUID userId, long bytesToAdd) {
    UserStorage storage = repository.findByUserId(userId)
        .orElseThrow(() -> new NotFoundException("User storage not found"));
    long newTotal = storage.getTotalUsedBytes() + bytesToAdd;
    if (newTotal > storage.getQuotaLimitBytes()) {
      throw new BadRequestException("Storage quota exceeded");
    }
    storage.setTotalUsedBytes(newTotal);
    repository.save(storage);
  }

  @Transactional
  public void subtractUsedBytes(UUID userId, long bytesToSubtract) {
    UserStorage storage = repository.findByUserId(userId)
        .orElseThrow(() -> new NotFoundException("User storage not found"));
    long newTotal = storage.getTotalUsedBytes() - bytesToSubtract;
    storage.setTotalUsedBytes(Math.max(newTotal, 0));
    repository.save(storage);
  }

  private static UserStorageDto toDto(UserStorage storage) {
    return new UserStorageDto(
        storage.getUserId(),
        storage.getTotalUsedBytes(),
        storage.getQuotaLimitBytes(),
        storage.getUpdatedAt(),
        storage.getCreatedAt()
    );
  }
}
