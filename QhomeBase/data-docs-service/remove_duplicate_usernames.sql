-- Script to remove duplicate usernames from iam.users
-- This script keeps the oldest user (by created_at) and deletes all duplicates
-- It also handles related data in iam.user_roles and data.residents

DO $$
DECLARE
    duplicate_count INTEGER;
BEGIN
    -- First, let's see how many duplicates we have
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT username, COUNT(*) as cnt
        FROM iam.users
        GROUP BY username
        HAVING COUNT(*) > 1
    ) duplicates;
    
    RAISE NOTICE 'Found % duplicate usernames', duplicate_count;
    
    -- Delete user_roles for duplicate users (keep the oldest user's roles)
    DELETE FROM iam.user_roles ur
    WHERE ur.user_id IN (
        SELECT u.id
        FROM iam.users u
        INNER JOIN (
            SELECT username, MIN(created_at) as min_created_at
            FROM iam.users
            GROUP BY username
            HAVING COUNT(*) > 1
        ) duplicates ON u.username = duplicates.username
        WHERE u.created_at > duplicates.min_created_at
    );
    
    RAISE NOTICE 'Deleted user_roles for duplicate users';
    
    -- Update data.residents to point to the oldest user (keep the first user_id)
    UPDATE data.residents r
    SET user_id = (
        SELECT u.id
        FROM iam.users u
        WHERE u.username = (
            SELECT username
            FROM iam.users
            WHERE id = r.user_id
        )
        ORDER BY u.created_at ASC
        LIMIT 1
    )
    WHERE r.user_id IN (
        SELECT u.id
        FROM iam.users u
        INNER JOIN (
            SELECT username, MIN(created_at) as min_created_at
            FROM iam.users
            GROUP BY username
            HAVING COUNT(*) > 1
        ) duplicates ON u.username = duplicates.username
        WHERE u.created_at > duplicates.min_created_at
    );
    
    RAISE NOTICE 'Updated residents to point to oldest users';
    
    -- Finally, delete duplicate users (keep the oldest one)
    DELETE FROM iam.users u
    WHERE u.id IN (
        SELECT u2.id
        FROM iam.users u2
        INNER JOIN (
            SELECT username, MIN(created_at) as min_created_at
            FROM iam.users
            GROUP BY username
            HAVING COUNT(*) > 1
        ) duplicates ON u2.username = duplicates.username
        WHERE u2.created_at > duplicates.min_created_at
    );
    
    RAISE NOTICE 'Deleted duplicate users';
    
    -- Show summary
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT username, COUNT(*) as cnt
        FROM iam.users
        GROUP BY username
        HAVING COUNT(*) > 1
    ) remaining_duplicates;
    
    IF duplicate_count > 0 THEN
        RAISE WARNING 'Still have % duplicate usernames remaining', duplicate_count;
    ELSE
        RAISE NOTICE 'All duplicate usernames have been removed successfully';
    END IF;
END $$;

-- Verification query
SELECT 
    username,
    COUNT(*) as user_count,
    STRING_AGG(id::text, ', ' ORDER BY created_at) as user_ids,
    STRING_AGG(created_at::text, ', ' ORDER BY created_at) as created_dates
FROM iam.users
GROUP BY username
HAVING COUNT(*) > 1
ORDER BY username;

