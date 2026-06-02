package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDetailsDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.util.JwtFilter;
import com.example.bankcards.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    // Моки для безопасности (необходимы для поднятия контекста)
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtFilter jwtFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_shouldReturnPage() throws Exception {
        CardResponseDto dto = new CardResponseDto(1L, "**** **** **** 1234", LocalDateTime.now().plusYears(5), CardStatus.ACTIVE, 1L, "John Doe", 1L);
        PageImpl<CardResponseDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        when(adminService.getAllCards(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_shouldReturnCreated() throws Exception {
        com.example.bankcards.entity.Card card = new com.example.bankcards.entity.Card();
        card.setId(100L);
        when(adminService.createCard(any())).thenReturn(card);

        mockMvc.perform(post("/api/v1/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"accountId\":1,\"validityPeriod\":\"2028-12-31T23:59:59\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockCard_shouldReturnNoContent() throws Exception {
        doNothing().when(adminService).blockCard(1L);
        mockMvc.perform(post("/api/v1/card/1/block"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateCard_shouldReturnNoContent() throws Exception {
        doNothing().when(adminService).activateCard(1L);
        mockMvc.perform(post("/api/v1/card/1/activate"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCard_shouldReturnNoContent() throws Exception {
        doNothing().when(adminService).deleteCard(1L);
        mockMvc.perform(delete("/api/v1/card/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCardById_shouldReturnDetails() throws Exception {
        CardDetailsDto dto = new CardDetailsDto(1L, "**** **** **** 1234", "4123456789012345", LocalDateTime.now().plusYears(5), CardStatus.ACTIVE, 1L, "John Doe", "john@mail.com", 1L, BigDecimal.valueOf(1000));
        when(adminService.getCardByIdForAdmin(1L)).thenReturn(dto);
        mockMvc.perform(get("/api/v1/card/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCardByNumber_shouldReturnDetails() throws Exception {
        CardDetailsDto dto = new CardDetailsDto(1L, "**** **** **** 1234", "4123456789012345", LocalDateTime.now().plusYears(5), CardStatus.ACTIVE, 1L, "John Doe", "john@mail.com", 1L, BigDecimal.valueOf(1000));
        when(adminService.getCardByNumberForAdmin("1234")).thenReturn(dto);
        mockMvc.perform(get("/api/v1/card/details?number=1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}