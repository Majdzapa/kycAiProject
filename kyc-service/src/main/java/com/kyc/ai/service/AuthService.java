package com.kyc.ai.service;

import com.kyc.ai.dto.*;
import com.kyc.ai.entity.User;
import com.kyc.ai.exception.BadRequestException;
import com.kyc.ai.exception.UnauthorizedException;
import com.kyc.ai.repository.UserRepository;
import com.kyc.ai.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            // Get user from database
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

            // Check if account is locked
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new UnauthorizedException("Account is temporarily locked. Please try again later.");
            }

            // Check if account is enabled
            if (!user.getEnabled()) {
                throw new UnauthorizedException("Account is disabled");
            }

            // Reset failed login attempts
            user.setFailedLoginAttempts(0);
            user.setLastLoginAt(LocalDateTime.now());

            // Ensure customerId is present for existing users
            if (user.getCustomerId() == null) {
                user.setCustomerId("CUST-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }

            userRepository.save(user);

            // Generate tokens using your JwtTokenProvider
            String accessToken = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            // Build response
            return LoginResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .user(convertToDTO(user))
                    .build();

        } catch (BadCredentialsException e) {
            handleFailedLogin(request.getUsername());
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    @Transactional
    public UserDTO register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .customerId("CUST-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .roles(Set.of(User.Role.CUSTOMER)) // Default role
                .enabled(true)
                .emailVerified(false)
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        return convertToDTO(user);
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        // Check if it's actually a refresh token
        if (!jwtTokenProvider.isRefreshToken(requestRefreshToken)) {
            throw new UnauthorizedException("Token is not a refresh token");
        }

        // Extract username from token
        String username = jwtTokenProvider.getUsernameFromToken(requestRefreshToken);

        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Check if user is still enabled
        if (!user.getEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        // Create new authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                user.getRoles().stream()
                        .map(role -> (GrantedAuthority) () -> "ROLE_" + role.name())
                        .collect(Collectors.toList()));

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(convertToDTO(user))
                .build();
    }

    private void handleFailedLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            // Lock account after 5 failed attempts for 15 minutes
            if (attempts >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                log.warn("Account locked due to too many failed login attempts: {}", username);
            }

            userRepository.save(user);
        });
    }

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .customerId(user.getCustomerId())
                .roles(user.getRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()))
                .enabled(user.getEnabled())
                .emailVerified(user.getEmailVerified())
                .mfaEnabled(user.getMfaEnabled())
                .build();
    }
}