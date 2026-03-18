package com.bank.authservice.service;

import com.bank.authservice.dto.AuthResponse;
import com.bank.authservice.dto.LoginRequest;
import com.bank.authservice.dto.RegisterRequest;
import com.bank.authservice.entity.RefreshToken;
import com.bank.authservice.entity.Role;
import com.bank.authservice.entity.User;
import com.bank.authservice.exception.DuplicateUserExistsException;
import com.bank.authservice.exception.InvalidUserCredentialsException;
import com.bank.authservice.repository.RefreshTokenRepository;
import com.bank.authservice.repository.RoleRepository;
import com.bank.authservice.repository.UserRepository;
import com.bank.authservice.security.JwtUtil;
import com.bank.authservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

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
    void shouldRegisterUserWhenUserIsNew() {

        RegisterRequest request = new RegisterRequest("abhishek", "password123");

        when(userRepository.findByUsername("abhishek")).thenReturn(Optional.empty()); // User doesn't exist yet
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(testRole)); // Role exists
        when(encoder.encode("password123")).thenReturn("hashedPassword"); // Fake the encoding
        when(userRepository.save(any(User.class))).thenReturn(new User());

        assertDoesNotThrow(() -> authService.register(request));


        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldThrowDuplicateUserExceptionWhenUserAlreadyExists() {
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
    void shouldReturnTokensWhenLoginCredentialsValid() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(eq("testuser"), anyList())).thenReturn("mocked-access-token");
        when(jwtUtil.generateRefreshToken("testuser")).thenReturn("mocked-refresh-token");
        when(jwtUtil.getRefreshExpiration()).thenReturn(86400000L); // 24 hours

        // When finding the existing token (returning empty means it's a new login)
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mocked-access-token", response.accessToken());
        assertEquals("mocked-refresh-token", response.refreshToken());

        // Verify the repository's save method was called exactly once
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void shouldThrowInvalidUserCredentialsExceptionWhenLoginPasswordInvalid() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encoder.matches("wrongpassword", "encodedPassword")).thenReturn(false); // Passwords DO NOT match

        // Act & Assert
        assertThrows(InvalidUserCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void shouldReturnNewAccessTokenWhenRefreshTokenIsValid() {
        // Arrange
        String validTokenString = "valid-refresh-token";
        // Create a fake token that expires 1 day from now
        RefreshToken mockTokenEntity = RefreshToken.builder()
                .id(1L)
                .token(validTokenString)
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .user(testUser)
                .build();

        when(refreshTokenRepository.findByToken(validTokenString)).thenReturn(Optional.of(mockTokenEntity));
        when(jwtUtil.generateToken(eq("testuser"), anyList())).thenReturn("new-access-token");

        // Act
        AuthResponse response = authService.refreshToken(validTokenString);

        // Assert
        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals(validTokenString, response.refreshToken()); // original refresh token is returned
    }

    @Test
    void shouldThrowRefreshTokenNotFoundExceptionWhenRefreshTokenNotFound() {
        // Arrange
        String invalidToken = "fake-token";
        when(refreshTokenRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(com.bank.authservice.exception.RefreshTokenNotFoundException.class,
                () -> authService.refreshToken(invalidToken));
    }

    @Test
    void shouldThrowRefreshTokenExpiredExceptionAndDeleteTokenWhenRefreshTokenIsExpired() {
        // Arrange
        String expiredTokenString = "expired-refresh-token";
        // Create a fake token that expired 1 day ago
        RefreshToken expiredTokenEntity = RefreshToken.builder()
                .id(1L)
                .token(expiredTokenString)
                .expiryDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .user(testUser)
                .build();

        when(refreshTokenRepository.findByToken(expiredTokenString)).thenReturn(Optional.of(expiredTokenEntity));

        // Act & Assert
        assertThrows(com.bank.authservice.exception.RefreshTokenExpiredException.class,
                () -> authService.refreshToken(expiredTokenString));

        // Crucial check: verify that our code deleted the expired token from the DB!
        verify(refreshTokenRepository, times(1)).delete(expiredTokenEntity);
    }
}