package com.poly.taskapi.common.security;

import com.poly.taskapi.common.error.ForbiddenException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

  private CurrentUser() {
  }

  public static JwtPrincipal requirePrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
      throw new ForbiddenException("Not authenticated");
    }
    return principal;
  }

  public static UUID requireUserId() {
    return requirePrincipal().userId();
  }
}
