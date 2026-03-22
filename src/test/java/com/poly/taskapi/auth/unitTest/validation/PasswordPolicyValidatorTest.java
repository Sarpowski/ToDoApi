package com.poly.taskapi.auth.unitTest.validation;

import com.poly.taskapi.auth.validation.PasswordPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class PasswordPolicyValidatorTest {

    private PasswordPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordPolicyValidator();
    }

    @Nested
    @DisplayName("Valid passwords")
    class ValidPasswords {

        @ParameterizedTest
        @ValueSource(strings = {
            "Test123!@",
            "Ab1!xyzw",
            "Ab1!xyzwAbCdEfGhIjK",
            "P@ssw0rd",
            "Complex1$",
        })
        @DisplayName("Should accept valid passwords")
        void validPasswords(String password) {
            assertThat(validator.isValid(password, null)).isTrue();
        }

        @Test
        @DisplayName("Should accept null (null handled by @NotBlank)")
        void nullAccepted() {
            assertThat(validator.isValid(null, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("Invalid passwords: too short")
    class TooShort {

        @ParameterizedTest
        @ValueSource(strings = {
            "Ab1!xyz",
            "Ab1!",
            "A1!",
        })
        @DisplayName("Should reject passwords shorter than 8 characters")
        void tooShort(String password) {
            assertThat(validator.isValid(password, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Invalid passwords: too long")
    class TooLong {

        @Test
        @DisplayName("Should reject password with 21 characters")
        void tooLong21() {
            String password = "Ab1!xyzAbcDefGhiJklMn"; // 21 chars
            assertThat(validator.isValid(password, null)).isFalse();
        }

        @Test
        @DisplayName("Should reject password with 30 characters")
        void tooLong30() {
            String password = "Ab1!" + "a".repeat(26);
            assertThat(validator.isValid(password, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Invalid passwords: missing character types")
    class MissingCharTypes {

        @Test
        @DisplayName("Should reject password without uppercase")
        void noUppercase() {
            assertThat(validator.isValid("test123!@", null)).isFalse();
        }

        @Test
        @DisplayName("Should reject password without lowercase")
        void noLowercase() {
            assertThat(validator.isValid("TEST123!@", null)).isFalse();
        }

        @Test
        @DisplayName("Should reject password without digit")
        void noDigit() {
            assertThat(validator.isValid("TestPass!@", null)).isFalse();
        }

        @Test
        @DisplayName("Should reject password without special character")
        void noSpecialChar() {
            assertThat(validator.isValid("TestPass123", null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Invalid passwords: whitespace")
    class Whitespace {

        @Test
        @DisplayName("Should reject password with space")
        void withSpace() {
            assertThat(validator.isValid("Test 123!@", null)).isFalse();
        }

        @Test
        @DisplayName("Should reject password with tab")
        void withTab() {
            assertThat(validator.isValid("Test\t123!@", null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Boundary: exactly 8 and 20 characters")
    class BoundaryLengths {

        @Test
        @DisplayName("Should accept exactly 8 valid characters")
        void exactly8() {
            assertThat(validator.isValid("Ab1!xyzw", null)).isTrue();
        }

        @Test
        @DisplayName("Should reject exactly 7 characters")
        void exactly7() {
            assertThat(validator.isValid("Ab1!xyz", null)).isFalse();
        }
    }
}
