package com.example.bankcards.controller;

import com.example.bankcards.dto.UserCreateAccountDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final AdminService adminService;

    public UserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public UserResponseDto userCreateDto(
            @RequestBody UserCreateAccountDto dto
    ) {
        return adminService.createUser(dto);
    }

    @GetMapping
    public List<UserResponseDto> getUsers() {
        return adminService.getAllUsers();
    }

}
