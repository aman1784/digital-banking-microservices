package com.bank.authservice.service.impl;

import com.bank.authservice.dto.AuthResponse;
import com.bank.authservice.dto.LoginRequest;
import com.bank.authservice.dto.RegisterRequest;
import com.bank.authservice.entity.Role;
import com.bank.authservice.entity.User;
import com.bank.authservice.exception.DuplicateUserExistsException;
import com.bank.authservice.exception.InvalidUserCredentialsException;
import com.bank.authservice.repository.RoleRepository;
import com.bank.authservice.repository.UserRepository;
import com.bank.authservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito for this test class
class AuthServiceImplTest {

    // @Mock creates a fake instance of the dependency
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtUtil jwtUtil;

    // @InjectMocks creates the actual service and injects the @Mocks into it
    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        // This runs before each test to set up common test data
        testRole = Role.builder().id(1L).name("ROLE_USER").build();
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .enabled(true)
                .roles(Set.of(testRole))
                .build();
    }

    @Test
    void register_whenUserIsNew_shouldSucceed() {
        // Arrange: Define the input and how the mocks should behave
        RegisterRequest request = new RegisterRequest("abhishek", "password123");

        when(userRepository.findByUsername("abhishek")).thenReturn(Optional.empty()); // User doesn't exist yet
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(testRole)); // Role exists
        when(encoder.encode("password123")).thenReturn("hashedPassword"); // Fake the encoding
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // Act: Call the method being tested
        assertDoesNotThrow(() -> authService.register(request));

        // Assert: Verify the repository's save method was called exactly once
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_whenUserAlreadyExists_shouldThrowDuplicateUserException() {
        // Arrange
        RegisterRequest request = new RegisterRequest("testuser", "password123");
        // Simulate that the user already exists in the database
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert: Expect our custom exception to be thrown
        assertThrows(DuplicateUserExistsException.class, () -> authService.register(request));

        // Ensure save was NEVER called because it should fail early
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_whenCredentialsValid_shouldReturnToken() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encoder.matches("password123", "encodedPassword")).thenReturn(true); // Passwords match
        when(jwtUtil.generateToken(eq("testuser"), anyList())).thenReturn("mocked-jwt-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.token());
    }

    @Test
    void login_whenPasswordInvalid_shouldThrowInvalidUserCredentialsException() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encoder.matches("wrongpassword", "encodedPassword")).thenReturn(false); // Passwords DO NOT match

        // Act & Assert
        assertThrows(InvalidUserCredentialsException.class, () -> authService.login(request));
    }
}