package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.util.HmacUtil;
import com.example.bankcards.util.converter.CryptoConverter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Entity
@Table(name = "card")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "num_card", nullable = false)
    private String numCard;

    @Column(name = "num_card_last_four_hash", length = 64)
    private String numCardLastFourHash;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "validity_period")
    private LocalDateTime validityPeriod;

    @Column(name = "card_status")
    @Enumerated(EnumType.STRING)
    private CardStatus cardStatus;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumCard() {
        return numCard;
    }

    public void setNumCard(String numCard) {
        this.numCard = numCard;
        if (numCard != null && numCard.length() >= 4) {
            String digits = numCard.replaceAll("\\D", "");
            String lastFour = digits.substring(digits.length() - 4);
            this.numCardLastFourHash = HmacUtil.hmac(lastFour); // метод ниже
        } else {
            throw new IllegalArgumentException("Card number is too short to extract last 4 digits");
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(LocalDateTime validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public CardStatus getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(CardStatus cardStatus) {
        this.cardStatus = cardStatus;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

}