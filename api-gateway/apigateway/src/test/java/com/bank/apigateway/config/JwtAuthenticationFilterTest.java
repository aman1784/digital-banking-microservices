package com.bank.apigateway.config;

import com.bank.apigateway.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        // By default, if the filter chain is called, just return an empty Mono (success)
        lenient().when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void shouldBypassAuthenticationForAuthEndpoints() {
        // Arrange: Create a mock request targeting the auth service
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert: Verify it completed successfully and called the chain without checking tokens
        StepVerifier.create(result).verifyComplete();
        verify(filterChain, times(1)).filter(exchange);
        verify(jwtUtil, never()).extractUsername(anyString());
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderIsMissing() {
        // Arrange: Request targeting a protected endpoint but missing the header
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts/me").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert: StepVerifier is used to evaluate Reactive Mono streams
        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any()); // Should be blocked, chain never called
    }

    @Test
    void shouldMutateRequestAndProceedWhenTokenIsValid() {
        // Arrange
        String validToken = "valid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts/me")
                .header("Authorization", "Bearer " + validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.extractUsername(validToken)).thenReturn("aman");
        when(jwtUtil.extractRoles(validToken)).thenReturn(List.of("ROLE_USER"));

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result).verifyComplete();

        // Verify the chain was called once with the newly mutated exchange
        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldReturn401WithSpecificMessageWhenTokenIsExpired() {
        // Arrange
        String expiredToken = "expired.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/transactions/deposit")
                .header("Authorization", "Bearer " + expiredToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Simulate the JwtUtil throwing the specific expiration exception
        when(jwtUtil.extractUsername(expiredToken)).thenThrow(new ExpiredJwtException(null, null, "Token Expired"));

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }
}