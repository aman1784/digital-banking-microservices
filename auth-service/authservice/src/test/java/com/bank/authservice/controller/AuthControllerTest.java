package com.bank.authservice.controller;

import com.bank.authservice.dto.AuthResponse;
import com.bank.authservice.dto.LoginRequest;
import com.bank.authservice.dto.RegisterRequest;
import com.bank.authservice.dto.TokenRefreshRequest;
import com.bank.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class) // @WebMvcTest tells Spring to ONLY load the web layer (Controller), not the whole database/application.
@AutoConfigureMockMvc(addFilters = false) // Disables Spring Security filters for simple unit testing
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // fake web browser

    @Autowired
    private ObjectMapper objectMapper; // Converts Java objects to JSON strings

    @MockBean // @MockBean is used in Spring tests to put a fake AuthService into the application context
    private AuthService authService;

    @Test
    void shouldReturn201CreatedWhenUserRegisters() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("abhishek", "password123");

        // Since register() returns void, we use doNothing()
        doNothing().when(authService).register(any(RegisterRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))) // Convert our request to JSON
                .andExpect(status().isCreated())
                .andExpect(content().string("User Registered Successfully"));
    }

    @Test
    void shouldReturn200AndTokensWhenLoginIsSuccessful() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("aman", "password123");
        AuthResponse mockResponse = new AuthResponse("mock-access-token", "mock-refresh-token");

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Expect 200 HTTP Status
                .andExpect(jsonPath("$.accessToken").value("mock-access-token")) // Check JSON body
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"));
    }

    @Test
    void shouldReturn200AndNewAccessTokenWhenRefreshTokenIsValid() throws Exception {
        // Arrange
        TokenRefreshRequest request = new TokenRefreshRequest("valid-refresh-token");
        AuthResponse mockResponse = new AuthResponse("new-access-token", "valid-refresh-token");

        when(authService.refreshToken(anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("valid-refresh-token"));
    }
}
