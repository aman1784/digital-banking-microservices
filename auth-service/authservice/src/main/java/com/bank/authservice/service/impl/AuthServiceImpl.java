package com.bank.authservice.service.impl;

import com.bank.authservice.dto.AuthResponse;
import com.bank.authservice.dto.LoginRequest;
import com.bank.authservice.dto.RegisterRequest;
import com.bank.authservice.entity.RefreshToken;
import com.bank.authservice.entity.Role;
import com.bank.authservice.entity.User;
import com.bank.authservice.exception.DuplicateUserExistsException;
import com.bank.authservice.exception.InvalidUserCredentialsException;
import com.bank.authservice.exception.RefreshTokenExpiredException;
import com.bank.authservice.exception.RefreshTokenNotFoundException;
import com.bank.authservice.repository.RefreshTokenRepository;
import com.bank.authservice.repository.RoleRepository;
import com.bank.authservice.repository.UserRepository;
import com.bank.authservice.service.AuthService;
import com.bank.authservice.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder encoder, JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void register(RegisterRequest request) {

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DuplicateUserExistsException("User already exists");
        }

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder().name("ROLE_USER").build()
                ));

        User user = User.builder()
                .username(request.username())
                .password(encoder.encode(request.password()))
                .enabled(true)
                .roles(Set.of(role))
                .build();

        userRepository.save(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidUserCredentialsException("Invalid credentials"));

        if (!encoder.matches(request.password(), user.getPassword())) {
            throw new InvalidUserCredentialsException("Invalid credentials");
        }

        List<String> roleNames = user.getRoles()
                .stream()
                .map(Role::getName)  // ROLE_ADMIN, ROLE_USER
                .toList();

        String accessToken = jwtUtil.generateToken(user.getUsername(), roleNames);

        // Generate and save Refresh Token
        String refreshTokenString = jwtUtil.generateRefreshToken(user.getUsername());

        RefreshToken refreshToken = refreshTokenRepository.findByUser_Id(user.getId())
                .orElse(new RefreshToken());

        // Update the entity with the new data
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenString);
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtUtil.getRefreshExpiration()));

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshTokenString);
    }

    @Transactional
    @Override
    public AuthResponse refreshToken(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token is not in database!"));

        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenExpiredException("Refresh token was expired. Please make a new SignIn request");
        }

        User user = refreshToken.getUser();
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        String accessToken = jwtUtil.generateToken(user.getUsername(), roleNames);

        return new AuthResponse(accessToken, requestRefreshToken);
    }
}
