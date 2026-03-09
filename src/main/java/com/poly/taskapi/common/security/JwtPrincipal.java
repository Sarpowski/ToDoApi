package com.poly.taskapi.common.security;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String username) {
}
