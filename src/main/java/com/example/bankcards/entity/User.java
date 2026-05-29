package com.example.bankcards.entity;

import com.example.bankcards.dto.CreateUserDto;

import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.util.HmacUtil;
import com.example.bankcards.util.converter.CryptoConverter;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "users")
public class User {

    public User() {
    }

    public User(CreateUserDto dto) {
        setFirstName(dto.firstName());
        setLastName(dto.lastName());
        setEmail(dto.email());           // ← теперь вызовет вычисление emailHash
        setPhone(dto.phone());
        setIdentityDocumentNumber(dto.identityDocumentNumber());
        setRole(dto.userRole());
        setUserStatus(dto.userStatus());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    @Convert(converter = CryptoConverter.class)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "email_hash", unique = true)
    private String emailHash;

    @Convert(converter = CryptoConverter.class)
    private String phone;

    @Convert(converter = CryptoConverter.class)
    private String identityDocumentNumber;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @JsonManagedReference
    @OneToMany(mappedBy = "user")
    private List<Account> account;

    @JsonManagedReference
    @OneToMany(mappedBy = "user")
    private List<Card> card;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        if (email != null && !email.isBlank()) {
            this.emailHash = HmacUtil.hmac(email);
        } else {
            this.emailHash = null;
        }
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdentityDocumentNumber() {
        return identityDocumentNumber;
    }

    public void setIdentityDocumentNumber(String identityDocumentNumber) {
        this.identityDocumentNumber = identityDocumentNumber;
    }

    public UserRole getRole() {
        return userRole;
    }

    public void setRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public List<Account> getAccount() {
        return account;
    }

    public void setAccount(List<Account> account) {
        this.account = account;
    }

    public List<Card> getCard() {
        return card;
    }

    public void setCard(List<Card> card) {
        this.card = card;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public void setEmailHash(String emailHash) {
        this.emailHash = emailHash;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }
}
