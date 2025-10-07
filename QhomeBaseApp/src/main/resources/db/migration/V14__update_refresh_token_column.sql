-- V14__update_refresh_token_column.sql

ALTER TABLE qhomebaseapp.refresh_tokens
    ALTER COLUMN token TYPE TEXT;

