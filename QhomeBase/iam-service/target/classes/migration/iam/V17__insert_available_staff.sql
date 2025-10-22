
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at) VALUES

('11111111-1111-1111-1111-111111111111', 
 'john_tech', 
 'john.tech@qhomebase.com', 
 'password',
 true, 
 null, 
 0, 
 null,
 now(),
 now()),


('22222222-2222-2222-2222-222222222222', 
 'jane_support', 
 'jane.support@qhomebase.com', 
 'password',
 true, 
 null, 
 0, 
 null,
 now(),
 now()),


('33333333-3333-3333-3333-333333333333', 
 'mike_maintenance', 
 'mike.maintenance@qhomebase.com', 
 'password',
 true, 
 null, 
 0, 
 null,
 now(),
 now()),


('44444444-4444-4444-4444-444444444444', 
 'sarah_billing', 
 'sarah.billing@qhomebase.com', 
 'password',
 true, 
 null, 
 0, 
 null,
 now(),
 now()),


('55555555-5555-5555-5555-555555555555', 
 'david_security', 
 'david.security@qhomebase.com', 
 'password',
 true, 
 null, 
 0, 
 null,
 now(),
 now()),

('66666666-6666-6666-6666-666666666666', 
 'lisa_cleaning', 
 'lisa.cleaning@qhomebase.com', 
 'password',
 true, 
 null, 
 0, 
 null,
 now(),
 now()),


('77777777-7777-7777-7777-777777777777', 
 'tom_electrician', 
 'tom.electrician@qhomebase.com', 
 'password',
 true, 
 null, 
 0, 
 null,
 now(),
 now()),


('88888888-8888-8888-8888-888888888888', 
 'emma_plumber', 
 'emma.plumber@qhomebase.com', 
 'password',
 true, 
 null, 
 0, 
 null,
 now(),
 now());


INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by) VALUES
('11111111-1111-1111-1111-111111111111', 'technician', now(), 'system'),
('22222222-2222-2222-2222-222222222222', 'supporter', now(), 'system'),
('33333333-3333-3333-3333-333333333333', 'technician', now(), 'system'),
('44444444-4444-4444-4444-444444444444', 'account', now(), 'system'),
('55555555-5555-5555-5555-555555555555', 'supporter', now(), 'system'),
('66666666-6666-6666-6666-666666666666', 'technician', now(), 'system'),
('77777777-7777-7777-7777-777777777777', 'account', now(), 'system'),
('88888888-8888-8888-8888-888888888888', 'supporter', now(), 'system');


