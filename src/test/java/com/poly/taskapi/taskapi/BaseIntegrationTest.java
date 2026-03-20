package com.poly.taskapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  private int userCounter = 0;

  protected String uniqueUsername() {
    return "testuser_" + System.nanoTime() + "_" + (userCounter++);
  }

  protected String uniqueEmail() {
    return "test_" + System.nanoTime() + "_" + (userCounter++) + "@test.com";
  }

  protected static final String VALID_PASSWORD = "Test123!@";
  protected static final String BASE_URL = "/api/v1";

  protected String registerAndLogin(String username, String email) throws Exception {
    String registerBody = """
        {
          "username": "%s",
          "password": "%s",
          "email": "%s",
          "firstName": "Test",
          "lastName": "User"
        }
        """.formatted(username, VALID_PASSWORD, email);

    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody))
        .andExpect(status().isCreated());

    String loginBody = """
        {
          "username": "%s",
          "password": "%s"
        }
        """.formatted(username, VALID_PASSWORD);

    MvcResult result = mockMvc.perform(post(BASE_URL + "/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody))
        .andExpect(status().isOk())
        .andReturn();

    var json = objectMapper.readTree(result.getResponse().getContentAsString());
    return json.get("token").asText();
  }

  protected String registerAndLogin() throws Exception {
    String username = uniqueUsername();
    String email = uniqueEmail();
    return registerAndLogin(username, email);
  }

  protected String createTodo(String token, String title, String priority) throws Exception {
    String body = """
        {
          "title": "%s",
          "content": "Test content",
          "priority": "%s"
        }
        """.formatted(title, priority);

    MvcResult result = mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn();

    var json = objectMapper.readTree(result.getResponse().getContentAsString());
    return json.get("id").asText();
  }
}
