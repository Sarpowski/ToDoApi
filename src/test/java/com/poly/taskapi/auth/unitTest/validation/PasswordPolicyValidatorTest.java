package com.poly.taskapi.auth.unitTest.validation;

import com.poly.taskapi.auth.validation.PasswordPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
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
        @ValueSource(strings = {"Ab1!xyz", "Ab1!", "A1!"})
        @DisplayName("Should reject passwords shorter than 8 characters")
        void tooShort(String password) {
            assertThat(validator.isValid(password, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Invalid passwords: too long")
    class TooLong {

        @ParameterizedTest
        @ValueSource(strings = {
            "Ab1!xyzAbcDefGhiJklMn",
            "Ab1!aaaaaaaaaaaaaaaaaaaaaaaaaa",
        })
        @DisplayName("Should reject passwords longer than 20 characters")
        void tooLong(String password) {
            assertThat(validator.isValid(password, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Invalid passwords: missing character types")
    class MissingCharTypes {

        @ParameterizedTest
        @ValueSource(strings = {
            "test123!@",
            "TEST123!@",
            "TestPass!@",
            "TestPass123",
            "ABC321456",
        })
        @DisplayName("Should reject passwords missing a required character type")
        void missingCharType(String password) {
            assertThat(validator.isValid(password, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Invalid passwords: whitespace")
    class Whitespace {

        @ParameterizedTest
        @ValueSource(strings = {"Test 123!@", "Test\t123!@"})
        @DisplayName("Should reject passwords containing whitespace")
        void withWhitespace(String password) {
            assertThat(validator.isValid(password, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Boundary lengths")
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