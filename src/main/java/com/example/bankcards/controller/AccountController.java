package com.example.bankcards.controller;

import com.example.bankcards.dto.CreateBankAccountDto;
import com.example.bankcards.dto.ResponseCreateBankAccountDto;
import com.example.bankcards.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AdminService adminService;

    public AccountController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/create")
    public ResponseCreateBankAccountDto createAccount(
            @RequestBody @Valid CreateBankAccountDto dto
    ) {
        return adminService.createAccount(dto);
    }
}
