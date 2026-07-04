package com.example.bankcards.config;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.HmacUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // конструктор
    @Override
    public void run(String... args) {
        if (userRepository.findByEmailHash(HmacUtil.hmac("admin@example.com")).isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setFirstName("Admin");
            admin.setLastName("Admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.ADMIN);
            admin.setUserStatus(UserStatus.ACTIVE);
            // Добавляем недостающие поля:
            admin.setPhone("+70000000000");                // ← добавить
            admin.setIdentityDocumentNumber("0000000000"); // ← добавить
            userRepository.save(admin);
        }
    }
}