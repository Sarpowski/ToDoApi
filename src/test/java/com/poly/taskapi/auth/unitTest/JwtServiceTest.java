package com.poly.taskapi.auth.unitTest;

import com.poly.taskapi.auth.JwtService;
import com.poly.taskapi.common.security.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        setField(jwtService, "secret", "f8a3cde9b8e42a1d2a7c3d9f6e4b8c1a9d3e7f1b2c6a4e8f0d1c3b5a7e9f2d6");
        setField(jwtService, "issuer", "testIssuer");
        setField(jwtService, "ttl", Duration.ofHours(24));
        jwtService.init();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate non-null token")
        void generatesNonNullToken() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "testuser");
            assertThat(token).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("Should generate token with three parts (JWT format)")
        void generatesValidJwtFormat() {
            String token = jwtService.generateToken(UUID.randomUUID(), "user");
            String[] parts = token.split("\\.");
            assertThat(parts).hasSize(3);
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void differentTokensForDifferentUsers() {
            String token1 = jwtService.generateToken(UUID.randomUUID(), "user1");
            String token2 = jwtService.generateToken(UUID.randomUUID(), "user2");
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("parseToken()")
    class ParseTokenTests {

        @Test
        @DisplayName("Should parse userId from token")
        void parsesUserId() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "testuser");

            JwtPrincipal principal = jwtService.parseToken(token);
            assertThat(principal.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should parse username from token")
        void parsesUsername() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "testuser");

            JwtPrincipal principal = jwtService.parseToken(token);
            assertThat(principal.username()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should throw exception for tampered token")
        void rejectsTamperedToken() {
            String token = jwtService.generateToken(UUID.randomUUID(), "user");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";

            assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should throw exception for garbage token")
        void rejectsGarbageToken() {
            assertThatThrownBy(() -> jwtService.parseToken("not.a.jwt"))
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("isValid()")
    class IsValidTests {

        @Test
        @DisplayName("Should return true for valid token")
        void validTokenReturnsTrue() {
            String token = jwtService.generateToken(UUID.randomUUID(), "user");
            assertThat(jwtService.isValid(token)).isTrue();
        }

        @Test
        @DisplayName("Should return false for tampered token")
        void tamperedTokenReturnsFalse() {
            String token = jwtService.generateToken(UUID.randomUUID(), "user");
            String tampered = token + "tamper";
            assertThat(jwtService.isValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("Should return false for null token")
        void nullTokenReturnsFalse() {
            assertThat(jwtService.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty token")
        void emptyTokenReturnsFalse() {
            assertThat(jwtService.isValid("")).isFalse();
        }

        @Test
        @DisplayName("Should return false for random string")
        void randomStringReturnsFalse() {
            assertThat(jwtService.isValid("random_string")).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void expiredTokenReturnsFalse() throws Exception {
            JwtService shortLived = new JwtService();
            setField(shortLived, "secret", "f8a3cde9b8e42a1d2a7c3d9f6e4b8c1a9d3e7f1b2c6a4e8f0d1c3b5a7e9f2d6");
            setField(shortLived, "issuer", "testIssuer");
            setField(shortLived, "ttl", Duration.ofMillis(1));
            shortLived.init();

            String token = shortLived.generateToken(UUID.randomUUID(), "user");
            Thread.sleep(50);
            assertThat(shortLived.isValid(token)).isFalse();
        }

        @Test
        @DisplayName("Should return false for token with wrong issuer")
        void wrongIssuerReturnsFalse() throws Exception {
            JwtService otherIssuer = new JwtService();
            setField(otherIssuer, "secret", "f8a3cde9b8e42a1d2a7c3d9f6e4b8c1a9d3e7f1b2c6a4e8f0d1c3b5a7e9f2d6");
            setField(otherIssuer, "issuer", "otherIssuer");
            setField(otherIssuer, "ttl", Duration.ofHours(1));
            otherIssuer.init();

            String token = otherIssuer.generateToken(UUID.randomUUID(), "user");
            assertThat(jwtService.isValid(token)).isFalse();
        }
    }
}
