-- V2__insert_users.sql
-- Insert sample data into login.users

INSERT INTO login.users (username, password, email, role) VALUES
('admin', 'admin123', 'admin@example.com', 'ADMIN'),
('user1', 'user123', 'user1@example.com', 'USER'),
('manager', 'manager123', 'manager@example.com', 'MANAGER');
