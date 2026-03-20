package com.poly.taskapi.todo;

import com.poly.taskapi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TodoControllerTest extends BaseIntegrationTest {

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    token = registerAndLogin();
  }

  @Nested
  @DisplayName("POST /todos")
  class CreateTests {

    @Test
    @DisplayName("26. Should create todo with title only")
    void createWithTitleOnly() throws Exception {
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Simple todo"}
                  """))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.todoName").value("Simple todo"))
          .andExpect(jsonPath("$.done").value(false));
    }

    @Test
    @DisplayName("27. Should create todo with all fields")
    void createWithAllFields() throws Exception {
      String deadline = Instant.now().plus(7, ChronoUnit.DAYS).toString();
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Full todo","content":"Description","deadline":"%s","priority":"HIGH","repeatType":"WEEKLY"}
                  """.formatted(deadline)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.todoName").value("Full todo"))
          .andExpect(jsonPath("$.todoContent").value("Description"))
          .andExpect(jsonPath("$.priority").value("HIGH"))
          .andExpect(jsonPath("$.repeatType").value("WEEKLY"));
    }

    @Test
    @DisplayName("28. Should return 400 for missing title")
    void createMissingTitle() throws Exception {
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"content":"No title"}
                  """))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("29. Should return 400 for blank title")
    void createBlankTitle() throws Exception {
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":""}
                  """))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("30. Should return 400 for deadline in the past")
    void createPastDeadline() throws Exception {
      String pastDeadline = Instant.now().minus(1, ChronoUnit.DAYS).toString();
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Past deadline","deadline":"%s"}
                  """.formatted(pastDeadline)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("31. Should default priority to NONE")
    void createDefaultPriority() throws Exception {
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"No priority set"}
                  """))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.priority").value("NONE"));
    }

    @Test
    @DisplayName("32. Should create todo with BLOCKER priority")
    void createBlockerPriority() throws Exception {
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Urgent","priority":"BLOCKER"}
                  """))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.priority").value("BLOCKER"));
    }

    @Test
    @DisplayName("33. Should create todo with DAILY repeat")
    void createDailyRepeat() throws Exception {
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Daily task","repeatType":"DAILY"}
                  """))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.repeatType").value("DAILY"));
    }

    @Test
    @DisplayName("34. Should create todo with MONTHLY repeat")
    void createMonthlyRepeat() throws Exception {
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Monthly task","repeatType":"MONTHLY"}
                  """))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.repeatType").value("MONTHLY"));
    }
  }

  @Nested
  @DisplayName("GET /todos")
  class GetAllTests {

    @Test
    @DisplayName("35. Should return empty list for new user")
    void getAllEmpty() throws Exception {
      mockMvc.perform(get(BASE_URL + "/todos")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)))
          .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("36. Should return list with created todos")
    void getAllWithTodos() throws Exception {
      createTodo(token, "Todo 1", "HIGH");
      createTodo(token, "Todo 2", "LOW");

      mockMvc.perform(get(BASE_URL + "/todos")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(2)))
          .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("37. Should support pagination")
    void getAllPagination() throws Exception {
      for (int i = 0; i < 15; i++) {
        createTodo(token, "Todo " + i, "NONE");
      }

      mockMvc.perform(get(BASE_URL + "/todos?page=0&size=10")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(10)))
          .andExpect(jsonPath("$.totalElements").value(15))
          .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("38. Should return second page")
    void getAllSecondPage() throws Exception {
      for (int i = 0; i < 15; i++) {
        createTodo(token, "Todo " + i, "NONE");
      }

      mockMvc.perform(get(BASE_URL + "/todos?page=1&size=10")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(5)))
          .andExpect(jsonPath("$.currentPage").value(1));
    }
  }

  @Nested
  @DisplayName("GET /todos/{id}")
  class GetByIdTests {

    @Test
    @DisplayName("39. Should get todo by id")
    void getById() throws Exception {
      String todoId = createTodo(token, "Get me", "HIGH");

      mockMvc.perform(get(BASE_URL + "/todos/" + todoId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(todoId))
          .andExpect(jsonPath("$.todoName").value("Get me"));
    }

    @Test
    @DisplayName("40. Should return 404 for non-existent todo")
    void getByIdNotFound() throws Exception {
      mockMvc.perform(get(BASE_URL + "/todos/" + UUID.randomUUID())
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("41. Should not access another user's todo")
    void getByIdOtherUser() throws Exception {
      String todoId = createTodo(token, "My todo", "HIGH");
      String otherToken = registerAndLogin();

      mockMvc.perform(get(BASE_URL + "/todos/" + todoId)
              .header("Authorization", "Bearer " + otherToken))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("PUT /todos/{id}")
  class UpdateTests {

    @Test
    @DisplayName("42. Should update todo title")
    void updateTitle() throws Exception {
      String todoId = createTodo(token, "Original", "HIGH");

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Updated"}
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.todoName").value("Updated"));
    }

    @Test
    @DisplayName("43. Should mark todo as done")
    void updateMarkDone() throws Exception {
      String todoId = createTodo(token, "Complete me", "HIGH");

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"done":true}
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.done").value(true));
    }

    @Test
    @DisplayName("44. Should update priority")
    void updatePriority() throws Exception {
      String todoId = createTodo(token, "Reprioritize", "LOW");

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"priority":"BLOCKER"}
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.priority").value("BLOCKER"));
    }

    @Test
    @DisplayName("45. Should update content")
    void updateContent() throws Exception {
      String todoId = createTodo(token, "Content test", "NONE");

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"content":"New description"}
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.todoContent").value("New description"));
    }

    @Test
    @DisplayName("46. Should return 404 updating non-existent todo")
    void updateNotFound() throws Exception {
      mockMvc.perform(put(BASE_URL + "/todos/" + UUID.randomUUID())
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Ghost"}
                  """))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("47. Should not update another user's todo")
    void updateOtherUser() throws Exception {
      String todoId = createTodo(token, "My todo", "HIGH");
      String otherToken = registerAndLogin();

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + otherToken)
              .content("""
                  {"title":"Hacked"}
                  """))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("48. Should return 400 for blank title update")
    void updateBlankTitle() throws Exception {
      String todoId = createTodo(token, "Valid title", "HIGH");

      mockMvc.perform(put(BASE_URL + "/todos/" + todoId)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"   "}
                  """))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("DELETE /todos/{id}")
  class DeleteTests {

    @Test
    @DisplayName("49. Should soft delete todo")
    void deleteTodo() throws Exception {
      String todoId = createTodo(token, "Delete me", "HIGH");

      mockMvc.perform(delete(BASE_URL + "/todos/" + todoId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isNoContent());

      mockMvc.perform(get(BASE_URL + "/todos/" + todoId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("50. Should return 404 deleting non-existent todo")
    void deleteNotFound() throws Exception {
      mockMvc.perform(delete(BASE_URL + "/todos/" + UUID.randomUUID())
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("51. Should not delete another user's todo")
    void deleteOtherUser() throws Exception {
      String todoId = createTodo(token, "Protected", "HIGH");
      String otherToken = registerAndLogin();

      mockMvc.perform(delete(BASE_URL + "/todos/" + todoId)
              .header("Authorization", "Bearer " + otherToken))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("52. Should not show deleted todo in list")
    void deletedNotInList() throws Exception {
      createTodo(token, "Keep me", "HIGH");
      String deleteId = createTodo(token, "Delete me", "LOW");

      mockMvc.perform(delete(BASE_URL + "/todos/" + deleteId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isNoContent());

      mockMvc.perform(get(BASE_URL + "/todos")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(1)))
          .andExpect(jsonPath("$.items[0].todoName").value("Keep me"));
    }

    @Test
    @DisplayName("53. Should not allow double delete")
    void doubleDelete() throws Exception {
      String todoId = createTodo(token, "Delete once", "HIGH");

      mockMvc.perform(delete(BASE_URL + "/todos/" + todoId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isNoContent());

      mockMvc.perform(delete(BASE_URL + "/todos/" + todoId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("54. Should create multiple and delete one")
    void deleteOneOfMany() throws Exception {
      createTodo(token, "A", "HIGH");
      String deleteId = createTodo(token, "B", "LOW");
      createTodo(token, "C", "MEDIUM");

      mockMvc.perform(delete(BASE_URL + "/todos/" + deleteId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isNoContent());

      mockMvc.perform(get(BASE_URL + "/todos")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(2));
    }
  }
}
