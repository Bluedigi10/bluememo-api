package com.bluedigi.bluememo.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bluedigi.bluememo.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.bluedigi.bluememo.todo.infrastructure.persistance.repository.TodoJpaRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HttpIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoJpaRepository todoRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        todoRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerLoginAndReadCurrentUser() throws Exception {
        String email = uniqueEmail();

        String token = register("David", email);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(get("/users/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("David"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void rejectInvalidRegistrationAndDuplicatedEmail() throws Exception {
        String email = uniqueEmail();
        register("David", email);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("David", email, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.path").value("/auth/register"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("D", "invalid-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void requireAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/todos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/todos"));
    }

    @Test
    void executeCompleteTodoLifecycle() throws Exception {
        String token = register("David", uniqueEmail());

        String todoId = JsonPath.read(mockMvc.perform(post("/todos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(todoBody("Integration test", "Exercise the HTTP flow")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.todoId");

        mockMvc.perform(get("/todos")
                        .header("Authorization", bearer(token))
                        .param("status", "PENDING")
                        .param("sortBy", "title")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].todoId").value(todoId))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/todos/{todoId}", todoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(todoBody("Updated integration test", "Updated through HTTP")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated integration test"));

        mockMvc.perform(patch("/todos/{todoId}", todoId)
                        .header("Authorization", bearer(token))
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(delete("/todos/{todoId}", todoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/todos/{todoId}", todoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Todo not found"));
    }

    @Test
    void isolateTodosBetweenUsers() throws Exception {
        String ownerToken = register("Owner", uniqueEmail());
        String otherUserToken = register("Other", uniqueEmail());

        String todoId = JsonPath.read(mockMvc.perform(post("/todos")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(todoBody("Private todo", "Only its owner can read it")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.todoId");

        mockMvc.perform(get("/todos/{todoId}", todoId)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Todo not found"));
    }

    private String register(String name, String email) throws Exception {
        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(name, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.token");
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String registerBody(String name, String email, String password) {
        return """
                {"name":"%s","email":"%s","password":"%s"}
                """.formatted(name, email, password);
    }

    private String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private String todoBody(String title, String description) {
        return """
                {"title":"%s","description":"%s"}
                """.formatted(title, description);
    }
}