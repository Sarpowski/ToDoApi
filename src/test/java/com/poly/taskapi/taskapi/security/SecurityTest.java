package com.poly.taskapi.security;

import com.poly.taskapi.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityTest extends BaseIntegrationTest {

  @Test
  @DisplayName("74. Should reject GET /todos without token")
  void getTodosNoToken() throws Exception {
    mockMvc.perform(get(BASE_URL + "/todos"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("75. Should reject POST /todos without token")
  void createTodoNoToken() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"No auth"}
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("76. Should reject PUT /todos/{id} without token")
  void updateTodoNoToken() throws Exception {
    mockMvc.perform(put(BASE_URL + "/todos/" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"No auth"}
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("77. Should reject DELETE /todos/{id} without token")
  void deleteTodoNoToken() throws Exception {
    mockMvc.perform(delete(BASE_URL + "/todos/" + UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("78. Should reject GET /user-storage/{id} without token")
  void getStorageNoToken() throws Exception {
    mockMvc.perform(get(BASE_URL + "/user-storage/" + UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("79. Should reject with invalid token")
  void invalidToken() throws Exception {
    mockMvc.perform(get(BASE_URL + "/todos")
            .header("Authorization", "Bearer invalid.token.here"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("80. Should reject with malformed Authorization header")
  void malformedAuthHeader() throws Exception {
    mockMvc.perform(get(BASE_URL + "/todos")
            .header("Authorization", "NotBearer sometoken"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("81. Should reject with empty Bearer token")
  void emptyBearerToken() throws Exception {
    mockMvc.perform(get(BASE_URL + "/todos")
            .header("Authorization", "Bearer "))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("82. Should allow POST /auth/register without token")
  void registerNoTokenAllowed() throws Exception {
    String body = """
        {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
        """.formatted(uniqueUsername(), VALID_PASSWORD, uniqueEmail());
    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("83. Should allow POST /auth/login without token")
  void loginNoTokenAllowed() throws Exception {
    String username = uniqueUsername();
    String body = """
        {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
        """.formatted(username, VALID_PASSWORD, uniqueEmail());
    mockMvc.perform(post(BASE_URL + "/auth/register")
        .contentType(MediaType.APPLICATION_JSON).content(body));

    mockMvc.perform(post(BASE_URL + "/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"%s"}
                """.formatted(username, VALID_PASSWORD)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("84. Should reject search without token")
  void searchNoToken() throws Exception {
    mockMvc.perform(get(BASE_URL + "/todos/search?q=test"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("85. Should reject filter without token")
  void filterNoToken() throws Exception {
    mockMvc.perform(get(BASE_URL + "/todos/filter?done=true"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("86. Should reject smart-list without token")
  void smartListNoToken() throws Exception {
    mockMvc.perform(get(BASE_URL + "/todos/smart-list"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("87. Should reject accessing another user's storage")
  void otherUserStorage() throws Exception {
    String token = registerAndLogin();
    mockMvc.perform(get(BASE_URL + "/user-storage/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("88. Should work with valid token on protected endpoint")
  void validTokenWorks() throws Exception {
    String token = registerAndLogin();
    mockMvc.perform(get(BASE_URL + "/todos")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }
}
