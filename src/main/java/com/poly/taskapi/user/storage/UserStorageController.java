package com.poly.taskapi.user.storage;

import com.poly.taskapi.common.error.ForbiddenException;
import com.poly.taskapi.common.security.CurrentUser;
import com.poly.taskapi.user.storage.dto.UserStorageDto;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-storage")
public class UserStorageController {

  private final UserStorageService service;

  public UserStorageController(UserStorageService service) {
    this.service = service;
  }

  @GetMapping("/{userId}")
  public UserStorageDto get(@PathVariable UUID userId) {
    UUID currentUserId = CurrentUser.requireUserId();
    if (!currentUserId.equals(userId)) {
      throw new ForbiddenException("Cannot access another user's storage");
    }
    return service.get(userId);
  }
}
