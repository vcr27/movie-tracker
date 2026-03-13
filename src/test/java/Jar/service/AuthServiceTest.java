package Jar.service;

import Jar.dto.LoginRequest;
import Jar.dto.RegisterRequest;
import Jar.entity.User;
import Jar.exception.BadRequestException;
import Jar.repository.UserRepository;
import Jar.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldSaveHashedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("plain-pass");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-pass")).thenReturn("hashed-pass");

        String result = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("user@example.com", saved.getEmail());
        assertEquals("hashed-pass", saved.getPassword());
        assertEquals("USER", saved.getRole());
        assertEquals("User registered successfully", result);
    }

    @Test
    void login_shouldThrowBadRequestForInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong-pass");

        User existing = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .role("USER")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong-pass", "encoded")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(anyString());
    }
}
