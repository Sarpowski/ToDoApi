package com.poly.taskapi.auth;

import com.poly.taskapi.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

  @Nested
  @DisplayName("POST /auth/register")
  class RegisterTests {

    @Test
    @DisplayName("1. Should register user with valid data")
    void registerSuccess() throws Exception {
      String body = """
          {"username":"%s","password":"%s","email":"%s","firstName":"John","lastName":"Doe"}
          """.formatted(uniqueUsername(), VALID_PASSWORD, uniqueEmail());

      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.username").exists())
          .andExpect(jsonPath("$.email").exists())
          .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("2. Should return 409 for duplicate username")
    void registerDuplicateUsername() throws Exception {
      String username = uniqueUsername();
      String body1 = """
          {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(username, VALID_PASSWORD, uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
          .contentType(MediaType.APPLICATION_JSON).content(body1)).andExpect(status().isCreated());

      String body2 = """
          {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(username, VALID_PASSWORD, uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body2))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("3. Should return 409 for duplicate email")
    void registerDuplicateEmail() throws Exception {
      String email = uniqueEmail();
      String body1 = """
          {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), VALID_PASSWORD, email);
      mockMvc.perform(post(BASE_URL + "/auth/register")
          .contentType(MediaType.APPLICATION_JSON).content(body1)).andExpect(status().isCreated());

      String body2 = """
          {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), VALID_PASSWORD, email);
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body2))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("4. Should return 400 for missing username")
    void registerMissingUsername() throws Exception {
      String body = """
          {"password":"%s","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(VALID_PASSWORD, uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5. Should return 400 for missing password")
    void registerMissingPassword() throws Exception {
      String body = """
          {"username":"%s","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. Should return 400 for missing email")
    void registerMissingEmail() throws Exception {
      String body = """
          {"username":"%s","password":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), VALID_PASSWORD);
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Should return 400 for missing firstName")
    void registerMissingFirstName() throws Exception {
      String body = """
          {"username":"%s","password":"%s","email":"%s","lastName":"B"}
          """.formatted(uniqueUsername(), VALID_PASSWORD, uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. Should return 400 for missing lastName")
    void registerMissingLastName() throws Exception {
      String body = """
          {"username":"%s","password":"%s","email":"%s","firstName":"A"}
          """.formatted(uniqueUsername(), VALID_PASSWORD, uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. Should return 400 for invalid email format")
    void registerInvalidEmail() throws Exception {
      String body = """
          {"username":"%s","password":"%s","email":"not-an-email","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), VALID_PASSWORD);
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("10. Should return 400 for password too short (7 chars)")
    void registerPasswordTooShort() throws Exception {
      String body = """
          {"username":"%s","password":"Ab1!xyz","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. Should return 400 for password too long (21 chars)")
    void registerPasswordTooLong() throws Exception {
      String body = """
          {"username":"%s","password":"Ab1!xyzAbcDefGhiJklMn","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("12. Should return 400 for password without uppercase")
    void registerPasswordNoUppercase() throws Exception {
      String body = """
          {"username":"%s","password":"test123!@","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("13. Should return 400 for password without lowercase")
    void registerPasswordNoLowercase() throws Exception {
      String body = """
          {"username":"%s","password":"TEST123!@","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("14. Should return 400 for password without digit")
    void registerPasswordNoDigit() throws Exception {
      String body = """
          {"username":"%s","password":"TestPass!@","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("15. Should return 400 for password without special char")
    void registerPasswordNoSpecialChar() throws Exception {
      String body = """
          {"username":"%s","password":"TestPass123","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("16. Should return 400 for password with whitespace")
    void registerPasswordWithWhitespace() throws Exception {
      String body = """
          {"username":"%s","password":"Test 123!@","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("17. Should accept 8-char valid password")
    void registerPasswordMinLength() throws Exception {
      String body = """
          {"username":"%s","password":"Ab1!xyzw","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("18. Should accept 20-char valid password")
    void registerPasswordMaxLength() throws Exception {
      String body = """
          {"username":"%s","password":"Ab1!xyzwAbCdEfGhIjK","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(uniqueUsername(), uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("19. Should return 400 for empty body")
    void registerEmptyBody() throws Exception {
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("20. Should return 400 for blank username")
    void registerBlankUsername() throws Exception {
      String body = """
          {"username":"","password":"%s","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(VALID_PASSWORD, uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /auth/login")
  class LoginTests {

    @Test
    @DisplayName("21. Should login with valid credentials")
    void loginSuccess() throws Exception {
      String username = uniqueUsername();
      String email = uniqueEmail();
      String regBody = """
          {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(username, VALID_PASSWORD, email);
      mockMvc.perform(post(BASE_URL + "/auth/register")
          .contentType(MediaType.APPLICATION_JSON).content(regBody)).andExpect(status().isCreated());

      String loginBody = """
          {"username":"%s","password":"%s"}
          """.formatted(username, VALID_PASSWORD);
      mockMvc.perform(post(BASE_URL + "/auth/login")
              .contentType(MediaType.APPLICATION_JSON).content(loginBody))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").exists())
          .andExpect(jsonPath("$.token", not(emptyString())));
    }

    @Test
    @DisplayName("22. Should return 401 for wrong password")
    void loginWrongPassword() throws Exception {
      String username = uniqueUsername();
      String regBody = """
          {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
          """.formatted(username, VALID_PASSWORD, uniqueEmail());
      mockMvc.perform(post(BASE_URL + "/auth/register")
          .contentType(MediaType.APPLICATION_JSON).content(regBody)).andExpect(status().isCreated());

      String loginBody = """
          {"username":"%s","password":"WrongPass1!@"}
          """.formatted(username);
      mockMvc.perform(post(BASE_URL + "/auth/login")
              .contentType(MediaType.APPLICATION_JSON).content(loginBody))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("23. Should return 401 for non-existent username")
    void loginNonExistentUser() throws Exception {
      String loginBody = """
          {"username":"nonexistent_user_xyz","password":"%s"}
          """.formatted(VALID_PASSWORD);
      mockMvc.perform(post(BASE_URL + "/auth/login")
              .contentType(MediaType.APPLICATION_JSON).content(loginBody))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("24. Should return 400 for missing username in login")
    void loginMissingUsername() throws Exception {
      String loginBody = """
          {"password":"%s"}
          """.formatted(VALID_PASSWORD);
      mockMvc.perform(post(BASE_URL + "/auth/login")
              .contentType(MediaType.APPLICATION_JSON).content(loginBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("25. Should return 400 for missing password in login")
    void loginMissingPassword() throws Exception {
      String loginBody = """
          {"username":"someuser"}
          """;
      mockMvc.perform(post(BASE_URL + "/auth/login")
              .contentType(MediaType.APPLICATION_JSON).content(loginBody))
          .andExpect(status().isBadRequest());
    }
  }
}
