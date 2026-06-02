package com.example.bankcards.controller;

import com.example.bankcards.dto.CreateBankAccountDto;
import com.example.bankcards.dto.ResponseCreateBankAccountDto;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.util.JwtFilter;
import com.example.bankcards.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtFilter jwtFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAccount_shouldReturnOk() throws Exception {
        ResponseCreateBankAccountDto response = new ResponseCreateBankAccountDto(10L, BigDecimal.valueOf(1000));
        when(adminService.createAccount(any(CreateBankAccountDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/account/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"initialBalance\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.initialBalance").value(1000));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAccount_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/account/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"initialBalance\":1000}"))
                .andExpect(status().isForbidden());
    }
}