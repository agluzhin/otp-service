-- OTP Service Database Schema
-- Run once on a fresh PostgreSQL 17 database

CREATE TABLE IF NOT EXISTS users (
                                     id       BIGSERIAL    PRIMARY KEY,
                                     login    VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(10)  NOT NULL CHECK (role IN ('ADMIN', 'USER'))
    );

-- Exactly one row at all times (enforced by CHECK + seed INSERT)
CREATE TABLE IF NOT EXISTS otp_config (
                                          id          INTEGER PRIMARY KEY DEFAULT 1,
                                          code_length INTEGER NOT NULL DEFAULT 6,
                                          ttl_seconds INTEGER NOT NULL DEFAULT 300,
                                          CHECK (id = 1)
    );
INSERT INTO otp_config (id, code_length, ttl_seconds)
VALUES (1, 6, 300)
    ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS otp_codes (
                                         id           BIGSERIAL    PRIMARY KEY,
                                         user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id VARCHAR(100) NOT NULL,
    code         VARCHAR(20)  NOT NULL,
    status       VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ  NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_otp_codes_user_id   ON otp_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_otp_codes_status     ON otp_codes(status);
CREATE INDEX IF NOT EXISTS idx_otp_codes_expires_at ON otp_codes(expires_at);