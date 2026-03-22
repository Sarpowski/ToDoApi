package com.poly.taskapi.functionalReqs;

import com.poly.taskapi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
class SearchAndSmartListTest extends BaseIntegrationTest {

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    token = registerAndLogin();
  }

  @Nested
  @DisplayName("GET /todos/search")
  class SearchTests {

    @Test
    @DisplayName("Should find todos by title keyword")
    void searchByKeyword() throws Exception {
      createTodo(token, "Buy groceries", "HIGH");
      createTodo(token, "Buy new laptop", "MEDIUM");
      createTodo(token, "Read a book", "LOW");

      mockMvc.perform(get(BASE_URL + "/todos/search?q=Buy")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(2)))
          .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should return empty list when no match")
    void searchNoMatch() throws Exception {
      createTodo(token, "Buy groceries", "HIGH");

      mockMvc.perform(get(BASE_URL + "/todos/search?q=nonexistent")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)))
          .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Should search case-insensitively")
    void searchCaseInsensitive() throws Exception {
      createTodo(token, "Important Meeting", "HIGH");

      mockMvc.perform(get(BASE_URL + "/todos/search?q=important")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Important Meeting"));
    }

    @Test
    @DisplayName("Should not return other user's todos in search")
    void searchIsolatedByUser() throws Exception {
      createTodo(token, "My secret task", "HIGH");

      String otherToken = registerAndLogin();
      createTodo(otherToken, "My secret task", "LOW");

      mockMvc.perform(get(BASE_URL + "/todos/search?q=secret")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("Should support pagination in search")
    void searchWithPagination() throws Exception {
      for (int i = 0; i < 15; i++) {
        createTodo(token, "Task item " + i, "NONE");
      }

      mockMvc.perform(get(BASE_URL + "/todos/search?q=Task&page=0&size=10")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(10)))
          .andExpect(jsonPath("$.totalElements").value(15))
          .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("Should not return deleted todos in search")
    void searchExcludesDeleted() throws Exception {
      createTodo(token, "Keep this task", "HIGH");
      String deleteId = createTodo(token, "Delete this task", "LOW");

      mockMvc.perform(delete(BASE_URL + "/todos/" + deleteId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isNoContent());

      mockMvc.perform(get(BASE_URL + "/todos/search?q=task")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Keep this task"));
    }
  }

  @Nested
  @DisplayName("GET /todos/smart-list")
  class SmartListTests {

    @Test
    @DisplayName("Should return only undone todos with deadline today or earlier")
    void smartListBasic() throws Exception {
      String futureDeadline = Instant.now().plus(7, ChronoUnit.DAYS).toString();
      String soonDeadline = Instant.now().plus(1, ChronoUnit.HOURS).toString();

      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Urgent today","deadline":"%s","priority":"HIGH"}
                  """.formatted(soonDeadline)))
          .andExpect(status().isCreated());

      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Next week task","deadline":"%s","priority":"LOW"}
                  """.formatted(futureDeadline)))
          .andExpect(status().isCreated());

      createTodo(token, "No deadline", "NONE");

      mockMvc.perform(get(BASE_URL + "/todos/smart-list")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Urgent today"));
    }

    @Test
    @DisplayName("Should exclude done todos from smart-list")
    void smartListExcludesDone() throws Exception {
      String soonDeadline = Instant.now().plus(1, ChronoUnit.HOURS).toString();

      String body = """
          {"title":"Done urgent","deadline":"%s","priority":"HIGH"}
          """.formatted(soonDeadline);
      var result = mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content(body))
          .andExpect(status().isCreated())
          .andReturn();

      String todoId = objectMapper.readTree(
          result.getResponse().getContentAsString()).get("id").asText();

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"done":true}
                  """))
          .andExpect(status().isOk());

      mockMvc.perform(get(BASE_URL + "/todos/smart-list")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("63. Should return empty smart-list for new user")
    void smartListEmpty() throws Exception {
      mockMvc.perform(get(BASE_URL + "/todos/smart-list")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)))
          .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Should not return other user's todos in smart-list")
    void smartListIsolatedByUser() throws Exception {
      String soonDeadline = Instant.now().plus(1, ChronoUnit.HOURS).toString();

      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"User1 urgent","deadline":"%s","priority":"HIGH"}
                  """.formatted(soonDeadline)))
          .andExpect(status().isCreated());

      String otherToken = registerAndLogin();

      mockMvc.perform(get(BASE_URL + "/todos/smart-list")
              .header("Authorization", "Bearer " + otherToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)));
    }
  }

  @Nested
  @DisplayName("GET /todos/filter")
  class FilterTests {

    @Test
    @DisplayName("Should filter by done=true")
    void filterByDone() throws Exception {
      String todoId = createTodo(token, "Complete me", "HIGH");
      createTodo(token, "Still pending", "LOW");

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"done":true}
                  """))
          .andExpect(status().isOk());

      mockMvc.perform(get(BASE_URL + "/todos/filter?done=true")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Complete me"));
    }

    @Test
    @DisplayName("Should filter by priority")
    void filterByPriority() throws Exception {
      createTodo(token, "High task", "HIGH");
      createTodo(token, "Low task", "LOW");
      createTodo(token, "Another high", "HIGH");

      mockMvc.perform(get(BASE_URL + "/todos/filter?priority=HIGH")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(2)))
          .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should filter by done and priority combined")
    void filterByDoneAndPriority() throws Exception {
      String todoId = createTodo(token, "Done high", "HIGH");
      createTodo(token, "Pending high", "HIGH");
      createTodo(token, "Done low", "LOW");

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"done":true}
                  """))
          .andExpect(status().isOk());

      mockMvc.perform(get(BASE_URL + "/todos/filter?done=true&priority=HIGH")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Done high"));
    }

    @Test
    @DisplayName("Should return all todos when no filter params")
    void filterNoParams() throws Exception {
      createTodo(token, "Task 1", "HIGH");
      createTodo(token, "Task 2", "LOW");

      mockMvc.perform(get(BASE_URL + "/todos/filter")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(2)));
    }
  }
}
