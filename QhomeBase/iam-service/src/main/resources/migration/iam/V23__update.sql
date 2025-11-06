-- Bước 1: Xóa bỏ ràng buộc cũ đang bắt buộc chữ thường trên bảng 'roles'.
ALTER TABLE iam.roles
DROP CONSTRAINT ck_roles_code_lower;

Alter table role_permissions
drop constraint role_permissions_role_fkey;

alter table user_roles
drop constraint user_roles_role_fkey;
-- (Ghi chú: Nếu bảng 'role_permissions' cũng có một ràng buộc tương tự, bạn cũng phải xóa nó)
-- ALTER TABLE iam.role_permissions DROP CONSTRAINT <tên_ràng_buộc_chữ_thường_khác>;


-- Bước 2: Bây giờ mới chạy các lệnh UPDATE của bạn (giữ nguyên).
UPDATE iam.role_permissions
SET role = UPPER(role)
WHERE role != UPPER(role);

UPDATE iam.roles
SET role = UPPER(role)
WHERE role != UPPER(role);


-- Bước 3: (Khuyến khích) Thêm ràng buộc MỚI để BẮT BUỘC CHỮ HOA
-- Điều này để đảm bảo dữ liệu luôn nhất quán theo quy tắc mới của bạn.
ALTER TABLE iam.roles
ADD CONSTRAINT ck_roles_role_upper CHECK (role = UPPER(role));

ALTER TABLE iam.role_permissions
ADD CONSTRAINT ck_role_permissions_role_upper CHECK (role = UPPER(role));


-- Bước 4: Cập nhật comments (giữ nguyên).
COMMENT ON COLUMN iam.role_permissions.role IS 'Role name in UPPERCASE format (matches UserRole enum name: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)';
COMMENT ON COLUMN iam.user_roles.role IS 'Role name in UPPERCASE format (matches UserRole enum name: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)';