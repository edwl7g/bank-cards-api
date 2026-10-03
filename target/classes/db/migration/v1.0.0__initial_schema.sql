-- v1.0.0__initial_schema.sql

-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email TEXT NOT NULL,
    email_hash VARCHAR(64) UNIQUE NOT NULL,
    phone TEXT NOT NULL,
    identity_document_number TEXT NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    user_status VARCHAR(20) NOT NULL
);

-- Создание таблицы счетов
CREATE TABLE IF NOT EXISTS bank_account (
    id BIGSERIAL PRIMARY KEY,
    balance NUMERIC(19,2) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- Создание таблицы карт
CREATE TABLE IF NOT EXISTS card (
    id BIGSERIAL PRIMARY KEY,
    num_card TEXT NOT NULL,
    num_card_last_four_hash VARCHAR(64),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    validity_period TIMESTAMP NOT NULL,
    card_status VARCHAR(20) NOT NULL,
    account_id BIGINT NOT NULL REFERENCES bank_account(id) ON DELETE CASCADE
);

-- Индексы
CREATE INDEX idx_user_email_hash ON users(email_hash);
CREATE INDEX idx_card_user_id ON card(user_id);
CREATE INDEX idx_card_account_id ON card(account_id);
CREATE INDEX idx_bank_account_user_id ON bank_account(user_id);
CREATE INDEX idx_card_last_four_hash ON card(num_card_last_four_hash);