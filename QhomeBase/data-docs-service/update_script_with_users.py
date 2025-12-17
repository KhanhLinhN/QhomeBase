import re

# Read the file
with open('create_residents_for_cde_g_buildings.sql', 'r', encoding='utf-8') as f:
    content = f.read()

# Pattern to match resident creation blocks that don't have user_data yet
# We'll update all blocks that have "resident_data AS" but don't have "user_data AS" before it
pattern = r'(    -- ([CDEG])---(\d{2}): Primary resident\s+WITH unit_data AS[^,]+),\s+resident_data AS \(\s+INSERT INTO data\.residents \(\s+id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at\s+\)\s+SELECT \s+gen_random_uuid\(\),\s+\'([^\']+)\',\s+\'(\d+)\',\s+\'([^\']+)\',\s+\'([^\']+)\',\s+\'([^\']+)\',\s+\'ACTIVE\',\s+NULL,'

def generate_username(full_name, unit_code):
    """Generate username from full name and unit code"""
    # Remove Vietnamese diacritics and spaces, convert to lowercase
    # Example: "Trần Văn C01" -> "tranvanc01"
    name_parts = full_name.lower().split()
    # Take last 2 parts (usually first name + last name) and unit code
    if len(name_parts) >= 2:
        username = name_parts[-2] + name_parts[-1] + unit_code.lower().replace('---', '')
    else:
        username = ''.join(name_parts) + unit_code.lower().replace('---', '')
    # Remove Vietnamese diacritics (simplified)
    username = username.replace('ă', 'a').replace('â', 'a').replace('á', 'a').replace('à', 'a').replace('ạ', 'a').replace('ả', 'a').replace('ã', 'a')
    username = username.replace('ê', 'e').replace('é', 'e').replace('è', 'e').replace('ẹ', 'e').replace('ẻ', 'e').replace('ẽ', 'e')
    username = username.replace('ô', 'o').replace('ơ', 'o').replace('ó', 'o').replace('ò', 'o').replace('ọ', 'o').replace('ỏ', 'o').replace('õ', 'o')
    username = username.replace('ư', 'u').replace('ú', 'u').replace('ù', 'u').replace('ụ', 'u').replace('ủ', 'u').replace('ũ', 'u')
    username = username.replace('í', 'i').replace('ì', 'i').replace('ị', 'i').replace('ỉ', 'i').replace('ĩ', 'i')
    username = username.replace('ý', 'y').replace('ỳ', 'y').replace('ỵ', 'y').replace('ỷ', 'y').replace('ỹ', 'y')
    username = username.replace('đ', 'd')
    return username

def replace_resident_block(match):
    building = match.group(2)
    unit_num = match.group(3)
    full_name = match.group(4)
    phone = match.group(5)
    email = match.group(6)
    national_id = match.group(7)
    dob = match.group(8)
    unit_code = f"{building}---{unit_num}"
    username = generate_username(full_name, unit_code)
    
    replacement = f"""{match.group(1)},
    user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            '{username}',
            '{email}',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (id) DO UPDATE SET
            username = EXCLUDED.username,
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            ud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM user_data ud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            '{full_name}',
            '{phone}',
            '{email}',
            '{national_id}',
            '{dob}',
            'ACTIVE',
            ud.user_id,"""
    
    return replacement

# Find and replace all resident blocks
new_content = re.sub(pattern, replace_resident_block, content, flags=re.MULTILINE | re.DOTALL)

# Also need to update the RETURNING clause to include user_id
new_content = re.sub(
    r'(\s+FROM unit_data\s+RETURNING id as resident_id)',
    r'\1, user_id',
    new_content
)

# Write back
with open('create_residents_for_cde_g_buildings.sql', 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Script updated successfully!")

