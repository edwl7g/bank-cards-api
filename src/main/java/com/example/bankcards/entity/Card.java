package com.example.bankcards.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Card {
    Long id;
    String numCardWithMask;
    User user;
    LocalDateTime validityPeriod;
    CardStatus cardStatus;
    BigDecimal balance;
}
