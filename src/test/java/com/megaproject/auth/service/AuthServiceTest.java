package com.megaproject.auth.service;

import com.megaproject.auth.dto.LoginRequest;
import com.megaproject.auth.dto.RegisterRequest;
import com.megaproject.auth.exception.AuthException;
import com.megaproject.auth.exception.EmailAlreadyInUseException;
import com.megaproject.auth.model.OtpPurpose;
import com.megaproject.auth.model.RefreshToken;
import com.megaproject.auth.model.Role;
import com.megaproject.auth.model.User;
import com.megaproject.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("u1");
        user.setEmail("test@test.com");
        user.setPassword("encoded");
        user.setRole(Role.USER);
    }

    @Test
    void testRegister_success() {
        RegisterRequest req = new RegisterRequest("test@test.com", "pass");
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);

        String id = authService.register(req);

        assertEquals("u1", id);
        verify(otpService).generateAndSend("u1", "test@test.com", OtpPurpose.VERIFICATION);
    }

    @Test
    void testRegister_duplicateEmail() {
        RegisterRequest req = new RegisterRequest("test@test.com", "pass");
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLogin_success() {
        LoginRequest req = new LoginRequest("test@test.com", "pass");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access_token");
        
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh_token");
        when(refreshTokenService.create("u1")).thenReturn(rt);

        var response = authService.login(req);

        assertEquals("access_token", response.accessToken());
        assertEquals("refresh_token", response.refreshToken());
    }

    @Test
    void testLogin_wrongPassword() {
        LoginRequest req = new LoginRequest("test@test.com", "wrong");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.login(req));
    }

    @Test
    void testUpdateUserRole() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        authService.updateUserRole("u1", Role.ALUMNI);

        assertEquals(Role.ALUMNI, user.getRole());
        verify(userRepository).save(user);
    }
}
