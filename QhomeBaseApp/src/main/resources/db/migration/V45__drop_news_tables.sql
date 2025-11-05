-- V45__drop_news_tables.sql
-- Drop all News-related tables and their dependencies

-- Drop tables in correct order (respecting foreign key constraints)
DROP TABLE IF EXISTS qhomebaseapp.news_read CASCADE;
DROP TABLE IF EXISTS qhomebaseapp.news_attachment CASCADE;
DROP TABLE IF EXISTS qhomebaseapp.news_image CASCADE;
DROP TABLE IF EXISTS qhomebaseapp.news CASCADE;
DROP TABLE IF EXISTS qhomebaseapp.news_category CASCADE;

