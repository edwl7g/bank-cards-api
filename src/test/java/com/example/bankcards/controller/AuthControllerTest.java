package com.example.bankcards.controller;

import com.example.bankcards.dto.AuthRequestDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.util.JwtFilter;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtFilter jwtFilter;

    private static String asJsonString(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void login_shouldReturnToken_whenValidCredentials() throws Exception {
        AuthRequestDto request = new AuthRequestDto("user@example.com", "password");

        // Создаём пользователя с email и ролью
        User user = new User();
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);  // ← добавляем роль
        user.setUserStatus(UserStatus.ACTIVE); // тоже желательно, но может не понадобиться

        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtUtil.generateToken("user@example.com", "ROLE_USER")).thenReturn("test.jwt.token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test.jwt.token"));
    }

    @Test
    void login_shouldReturnUnauthorized_whenInvalidCredentials() throws Exception {
        AuthRequestDto request = new AuthRequestDto("wrong@example.com", "wrong");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }
}