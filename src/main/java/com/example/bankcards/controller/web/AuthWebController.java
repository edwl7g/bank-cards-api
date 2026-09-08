package com.example.bankcards.controller.web;

import com.example.bankcards.dto.UserRegistrationDto;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web")
public class AuthWebController {

    private final AdminService adminService;

    public AuthWebController(AdminService adminService) {
        this.adminService = adminService;
    }

    // Показать страницу входа
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Показать страницу регистрации
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        return "register";
    }

    // Обработка регистрации
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("userDto") UserRegistrationDto dto,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            adminService.registerUser(dto);
            return "redirect:/web/login?registered";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    // Главная страница после входа
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        return "dashboard";
    }
}