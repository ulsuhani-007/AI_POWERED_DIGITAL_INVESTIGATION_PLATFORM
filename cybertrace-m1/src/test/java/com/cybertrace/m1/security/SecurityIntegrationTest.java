package com.cybertrace.m1.security;

import com.cybertrace.m1.dto.CreateCaseRequest;
import com.cybertrace.m1.dto.CreateUserRequest;
import com.cybertrace.m1.dto.LoginRequest;
import com.cybertrace.m1.dto.LoginResponse;
import com.cybertrace.m1.entity.UserEntity;
import com.cybertrace.m1.entity.enums.UserRole;
import com.cybertrace.m1.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Ensure test users exist with known credentials
        if (!userRepository.existsByUsername("test_investigator")) {
            UserEntity inv = new UserEntity(
                    "test_investigator",
                    passwordEncoder.encode("InvestigatorSecret1!"),
                    UserRole.INVESTIGATOR,
                    true
            );
            inv.setCreatedAt(Instant.now());
            userRepository.save(inv);
        }

        if (!userRepository.existsByUsername("test_admin")) {
            UserEntity adm = new UserEntity(
                    "test_admin",
                    passwordEncoder.encode("AdminSecret1!"),
                    UserRole.ADMIN,
                    true
            );
            adm.setCreatedAt(Instant.now());
            userRepository.save(adm);
        }

        if (!userRepository.existsByUsername("test_disabled")) {
            UserEntity dis = new UserEntity(
                    "test_disabled",
                    passwordEncoder.encode("DisabledSecret1!"),
                    UserRole.INVESTIGATOR,
                    false
            );
            dis.setCreatedAt(Instant.now());
            userRepository.save(dis);
        }
    }

    @Test
    @DisplayName("1. Valid login returns authentication token and user role")
    void testValidLogin() throws Exception {
        LoginRequest req = new LoginRequest("test_investigator", "InvestigatorSecret1!");

        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("test_investigator"))
                .andExpect(jsonPath("$.role").value("INVESTIGATOR"))
                .andReturn().getResponse().getContentAsString();

        LoginResponse loginResponse = objectMapper.readValue(resp, LoginResponse.class);
        assertNotNull(loginResponse.getToken());
        assertFalse(loginResponse.getToken().isBlank());
    }

    @Test
    @DisplayName("2. Login with invalid username returns 401 Unauthorized")
    void testInvalidUsername() throws Exception {
        LoginRequest req = new LoginRequest("non_existent_analyst", "SomePassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("3. Login with invalid password returns 401 Unauthorized")
    void testInvalidPassword() throws Exception {
        LoginRequest req = new LoginRequest("test_investigator", "WrongPassword999!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("4. Login with disabled user account returns 403 Forbidden")
    void testDisabledUserLogin() throws Exception {
        LoginRequest req = new LoginRequest("test_disabled", "DisabledSecret1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("disabled")));
    }

    @Test
    @DisplayName("5. Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void testUnauthenticatedProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("6. Investigator can access case endpoints with valid token")
    void testInvestigatorAuthorization() throws Exception {
        String token = obtainToken("test_investigator", "InvestigatorSecret1!");

        CreateCaseRequest caseReq = new CreateCaseRequest("CASE-SEC-01", "Security Audit", "Desc", "HIGH", "Analyst");

        mockMvc.perform(post("/api/cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseNumber").value("CASE-SEC-01"));

        mockMvc.perform(get("/api/cases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("7. Investigator cannot access Admin user-management endpoints (403 Forbidden)")
    void testInvestigatorForbiddenOnAdminEndpoints() throws Exception {
        String token = obtainToken("test_investigator", "InvestigatorSecret1!");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action."));
    }

    @Test
    @DisplayName("8. Admin can access user management endpoints")
    void testAdminAuthorization() throws Exception {
        String adminToken = obtainToken("test_admin", "AdminSecret1!");

        CreateUserRequest userReq = new CreateUserRequest(
                "new_investigator_99",
                "Password99!",
                UserRole.INVESTIGATOR,
                true
        );

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_investigator_99"))
                .andExpect(jsonPath("$.role").value("INVESTIGATOR"));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("9. Password is saved as BCrypt hash, never plaintext")
    void testPasswordIsStoredHashed() {
        UserEntity user = userRepository.findByUsername("test_investigator").orElseThrow();

        assertNotNull(user.getPasswordHash());
        assertNotEquals("InvestigatorSecret1!", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches("InvestigatorSecret1!", user.getPasswordHash()));
    }

    @Test
    @DisplayName("10. Logout invalidates token, subsequent requests return 401")
    void testLogoutInvalidatesToken() throws Exception {
        String token = obtainToken("test_investigator", "InvestigatorSecret1!");

        // Token works
        mockMvc.perform(get("/api/cases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Token is now revoked -> 401
        mockMvc.perform(get("/api/cases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("11. Health endpoint is public and returns status UP and database CONNECTED")
    void testHealthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("CONNECTED"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("12. OpenAPI documentation endpoints are public")
    void testOpenApiEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.info.title").value("CyberTrace AI - Digital Investigation Platform API"));
    }

    @Test
    @DisplayName("13. Safe error responses: 400 Bad Request contains clean message without stack trace")
    void testSafeErrorResponseFormat() throws Exception {
        String token = obtainToken("test_investigator", "InvestigatorSecret1!");

        // Send invalid empty JSON object
        mockMvc.perform(post("/api/cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        LoginResponse loginResponse = objectMapper.readValue(resp, LoginResponse.class);
        return loginResponse.getToken();
    }
}

