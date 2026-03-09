package com.poly.taskapi.user.storage;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStorageRepository extends JpaRepository<UserStorage, UUID> {

  Optional<UserStorage> findByUserId(UUID userId);
}
