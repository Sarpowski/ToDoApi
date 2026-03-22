package com.poly.taskapi.boundary;

import com.poly.taskapi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
class BoundaryTest extends BaseIntegrationTest {

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    token = registerAndLogin();
  }


  @Test
  @DisplayName("89. Should accept title with exactly 128 characters")
  void titleMax128() throws Exception {
    String title = "A".repeat(128);
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
                {"title":"%s"}
                """.formatted(title)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("90. Should reject title with 129 characters")
  void titleOver128() throws Exception {
    String title = "A".repeat(129);
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
                {"title":"%s"}
                """.formatted(title)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("91. Should accept title with 1 character")
  void titleMin1() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
                {"title":"A"}
                """))
        .andExpect(status().isCreated());
  }


  @Test
  @DisplayName("92. Should accept content with exactly 2048 characters")
  void contentMax2048() throws Exception {
    String content = "B".repeat(2048);
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
                {"title":"Content test","content":"%s"}
                """.formatted(content)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("93. Should reject content with 2049 characters")
  void contentOver2048() throws Exception {
    String content = "B".repeat(2049);
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
                {"title":"Content test","content":"%s"}
                """.formatted(content)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("94. Should accept empty content")
  void contentEmpty() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
                {"title":"No content"}
                """))
        .andExpect(status().isCreated());
  }


  @Test
  @DisplayName("96. Should reject username with 65 characters")
  void usernameOver64() throws Exception {
    String username = "u".repeat(65);
    String body = """
        {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
        """.formatted(username, VALID_PASSWORD, uniqueEmail());
    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }


  @Test
  @DisplayName("97. Should reject email over 320 characters")
  void emailOver320() throws Exception {
    String email = "a".repeat(310) + "@test.com";
    String body = """
        {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"B"}
        """.formatted(uniqueUsername(), VALID_PASSWORD, email);
    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }


  @Test
  @DisplayName("98. Should accept firstName with exactly 100 characters")
  void firstNameMax100() throws Exception {
    String firstName = "F".repeat(100);
    String body = """
        {"username":"%s","password":"%s","email":"%s","firstName":"%s","lastName":"B"}
        """.formatted(uniqueUsername(), VALID_PASSWORD, uniqueEmail(), firstName);
    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("99. Should reject firstName with 101 characters")
  void firstNameOver100() throws Exception {
    String firstName = "F".repeat(101);
    String body = """
        {"username":"%s","password":"%s","email":"%s","firstName":"%s","lastName":"B"}
        """.formatted(uniqueUsername(), VALID_PASSWORD, uniqueEmail(), firstName);
    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("100. Should accept lastName with exactly 100 characters")
  void lastNameMax100() throws Exception {
    String lastName = "L".repeat(100);
    String body = """
        {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"%s"}
        """.formatted(uniqueUsername(), VALID_PASSWORD, uniqueEmail(), lastName);
    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("101. Should reject lastName with 101 characters")
  void lastNameOver100() throws Exception {
    String lastName = "L".repeat(101);
    String body = """
        {"username":"%s","password":"%s","email":"%s","firstName":"A","lastName":"%s"}
        """.formatted(uniqueUsername(), VALID_PASSWORD, uniqueEmail(), lastName);
    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }



  @Test
  @DisplayName("102. Should accept all valid priority values")
  void allPriorities() throws Exception {
    for (String p : new String[]{"BLOCKER", "HIGH", "MEDIUM", "LOW", "NONE"}) {
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Priority %s","priority":"%s"}
                  """.formatted(p, p)))
          .andExpect(status().isCreated());
    }
  }



  @Test
  @DisplayName("104. Should accept all valid repeatType values")
  void allRepeatTypes() throws Exception {
    for (String r : new String[]{"DAILY", "WEEKLY", "MONTHLY", "YEARLY"}) {
      mockMvc.perform(post(BASE_URL + "/todos")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .content("""
                  {"title":"Repeat %s","repeatType":"%s"}
                  """.formatted(r, r)))
          .andExpect(status().isCreated());
    }
  }

  @Test
  @DisplayName("105. Should reject invalid repeatType value")
  void invalidRepeatType() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
                {"title":"Bad repeat","repeatType":"HOURLY"}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("106. Should reject empty title")
  void titleEmpty() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
              {"title":""}
              """))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("107. Should reject blank title")
  void titleBlank() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
              {"title":"   "}
              """))
        .andExpect(status().isBadRequest());
  }





  @Test
  @DisplayName("117. Should reject creating todo with invalid authorization token")
  void createTodoWithInvalidToken() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer invalid_token"))
        .andExpect(status().isForbidden());  }



  @Test
  @DisplayName("109. Should reject invalid email format")
  void invalidEmailFormat() throws Exception {
    String body = """
      {"username":"%s","password":"%s","email":"invalid-email","firstName":"A","lastName":"B"}
      """.formatted(uniqueUsername(), VALID_PASSWORD);

    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }




  @Test
  @DisplayName("110. Should reject invalid priority value")
  void invalidPriority() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
              {"title":"Bad priority","priority":"URGENT"}
              """))
        .andExpect(status().isBadRequest());
  }




  @Test
  @DisplayName("111. Should reject lowercase repeatType")
  void lowercaseRepeatType() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
              {"title":"Repeat test","repeatType":"daily"}
              """))
        .andExpect(status().isBadRequest());
  }




  @Test
  @DisplayName("112. Should accept content with 1 character")
  void contentMin1() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
              {"title":"Min content","content":"A"}
              """))
        .andExpect(status().isCreated());
  }




  @Test
  @DisplayName("113. Should reject registration without username")
  void registerWithoutUsername() throws Exception {
    String body = """
      {"password":"%s","email":"%s","firstName":"A","lastName":"B"}
      """.formatted(VALID_PASSWORD, uniqueEmail());

    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }


  @Test
  @DisplayName("114. Should reject registration without password")
  void registerWithoutPassword() throws Exception {
    String body = """
      {"username":"%s","email":"%s","firstName":"A","lastName":"B"}
      """.formatted(uniqueUsername(), uniqueEmail());

    mockMvc.perform(post(BASE_URL + "/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }




  @Test
  @DisplayName("115. Should reject invalid deadline format")
  void invalidDeadlineFormat() throws Exception {
    mockMvc.perform(post(BASE_URL + "/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("""
              {"title":"Bad deadline","deadline":"tomorrow"}
              """))
        .andExpect(status().isBadRequest());
  }

}
