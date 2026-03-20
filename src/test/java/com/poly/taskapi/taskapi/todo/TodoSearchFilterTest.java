package com.poly.taskapi.todo;

import com.poly.taskapi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TodoSearchFilterTest extends BaseIntegrationTest {

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    token = registerAndLogin();
  }

  @Nested
  @DisplayName("GET /todos/search")
  class SearchTests {

    @Test
    @DisplayName("55. Should find todo by exact title")
    void searchExactTitle() throws Exception {
      createTodo(token, "Unique task XYZ", "HIGH");
      createTodo(token, "Other task", "LOW");

      mockMvc.perform(get(BASE_URL + "/todos/search?q=Unique task XYZ")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Unique task XYZ"));
    }

    @Test
    @DisplayName("56. Should find todos by partial title")
    void searchPartialTitle() throws Exception {
      createTodo(token, "Buy groceries", "HIGH");
      createTodo(token, "Buy medicine", "LOW");
      createTodo(token, "Clean house", "MEDIUM");

      mockMvc.perform(get(BASE_URL + "/todos/search?q=Buy")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("57. Should search case-insensitive")
    void searchCaseInsensitive() throws Exception {
      createTodo(token, "IMPORTANT Meeting", "BLOCKER");

      mockMvc.perform(get(BASE_URL + "/todos/search?q=important")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("58. Should return empty for no match")
    void searchNoMatch() throws Exception {
      createTodo(token, "Something", "HIGH");

      mockMvc.perform(get(BASE_URL + "/todos/search?q=nonexistent")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("59. Should not find other user's todos")
    void searchIsolation() throws Exception {
      createTodo(token, "My secret todo", "HIGH");
      String otherToken = registerAndLogin();

      mockMvc.perform(get(BASE_URL + "/todos/search?q=secret")
              .header("Authorization", "Bearer " + otherToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("60. Should not find deleted todos in search")
    void searchSkipsDeleted() throws Exception {
      String todoId = createTodo(token, "Delete me search", "HIGH");
      mockMvc.perform(delete(BASE_URL + "/todos/" + todoId)
          .header("Authorization", "Bearer " + token));

      mockMvc.perform(get(BASE_URL + "/todos/search?q=Delete me")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }
  }

  @Nested
  @DisplayName("GET /todos/filter")
  class FilterTests {

    @Test
    @DisplayName("61. Should filter by done=true")
    void filterDone() throws Exception {
      String todoId = createTodo(token, "Done task", "HIGH");
      createTodo(token, "Pending task", "LOW");

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + token)
          .content("""
              {"done":true}
              """));

      mockMvc.perform(get(BASE_URL + "/todos/filter?done=true")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Done task"));
    }

    @Test
    @DisplayName("62. Should filter by done=false")
    void filterNotDone() throws Exception {
      String todoId = createTodo(token, "Done", "HIGH");
      createTodo(token, "Pending", "LOW");

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + token)
          .content("""
              {"done":true}
              """));

      mockMvc.perform(get(BASE_URL + "/todos/filter?done=false")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Pending"));
    }

    @Test
    @DisplayName("63. Should filter by priority=HIGH")
    void filterByPriority() throws Exception {
      createTodo(token, "High task", "HIGH");
      createTodo(token, "Low task", "LOW");
      createTodo(token, "Another high", "HIGH");

      mockMvc.perform(get(BASE_URL + "/todos/filter?priority=HIGH")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("64. Should filter by BLOCKER priority")
    void filterBlocker() throws Exception {
      createTodo(token, "Blocker", "BLOCKER");
      createTodo(token, "Normal", "NONE");

      mockMvc.perform(get(BASE_URL + "/todos/filter?priority=BLOCKER")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("65. Should filter by done AND priority combined")
    void filterDoneAndPriority() throws Exception {
      String id1 = createTodo(token, "Done High", "HIGH");
      createTodo(token, "Pending High", "HIGH");
      String id3 = createTodo(token, "Done Low", "LOW");

      mockMvc.perform(put(BASE_URL + "/todos/" + id1)
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + token)
          .content("""
              {"done":true}
              """));
      mockMvc.perform(put(BASE_URL + "/todos/" + id3)
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + token)
          .content("""
              {"done":true}
              """));

      mockMvc.perform(get(BASE_URL + "/todos/filter?done=true&priority=HIGH")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Done High"));
    }

    @Test
    @DisplayName("66. Should return all when no filter params")
    void filterNoParams() throws Exception {
      createTodo(token, "A", "HIGH");
      createTodo(token, "B", "LOW");

      mockMvc.perform(get(BASE_URL + "/todos/filter")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("67. Should return empty when filter matches nothing")
    void filterNoMatch() throws Exception {
      createTodo(token, "Low task", "LOW");

      mockMvc.perform(get(BASE_URL + "/todos/filter?priority=BLOCKER")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("68. Should not show deleted in filter")
    void filterSkipsDeleted() throws Exception {
      String todoId = createTodo(token, "Delete filter", "HIGH");
      createTodo(token, "Keep filter", "HIGH");

      mockMvc.perform(delete(BASE_URL + "/todos/" + todoId)
          .header("Authorization", "Bearer " + token));

      mockMvc.perform(get(BASE_URL + "/todos/filter?priority=HIGH")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)));
    }
  }

  @Nested
  @DisplayName("GET /todos/smart-list")
  class SmartListTests {

    @Test
    @DisplayName("69. Should return empty smart list for new user")
    void smartListEmpty() throws Exception {
      mockMvc.perform(get(BASE_URL + "/todos/smart-list")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("70. Should not include todos without deadline")
    void smartListNoDeadline() throws Exception {
      createTodo(token, "No deadline", "HIGH");

      mockMvc.perform(get(BASE_URL + "/todos/smart-list")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("71. Should not include future deadline todos")
    void smartListFutureDeadline() throws Exception {
      String futureDeadline = Instant.now().plus(30, ChronoUnit.DAYS).toString();
      mockMvc.perform(post(BASE_URL + "/todos")
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + token)
          .content("""
              {"title":"Future","deadline":"%s"}
              """.formatted(futureDeadline)));

      mockMvc.perform(get(BASE_URL + "/todos/smart-list")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("72. Should not include completed todos")
    void smartListExcludesDone() throws Exception {
      String deadline = Instant.now().plus(1, ChronoUnit.HOURS).toString();
      String body = """
          {"title":"Today done","deadline":"%s"}
          """.formatted(deadline);

      var result = mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content(body))
          .andReturn();
      var json = objectMapper.readTree(result.getResponse().getContentAsString());
      String todoId = json.get("id").asText();

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + token)
          .content("""
              {"done":true}
              """));

      mockMvc.perform(get(BASE_URL + "/todos/smart-list")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("73. Should be paginated")
    void smartListPaginated() throws Exception {
      mockMvc.perform(get(BASE_URL + "/todos/smart-list?page=0&size=5")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.pageSize").value(5));
    }
  }
}
