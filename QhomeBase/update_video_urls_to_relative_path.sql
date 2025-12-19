-- Script to update video URLs to relative paths
-- This allows Flutter app to prepend base URL from app_config.dart
-- Run this script to fix existing videos with hardcoded IP addresses

-- Update video_storage table: convert full URLs to relative paths
UPDATE files.video_storage 
SET file_url = '/api/videos/stream/' || id::text
WHERE file_url IS NOT NULL 
  AND file_url != ''
  AND (
    -- URLs with hardcoded IP addresses
    file_url LIKE '%192.168.%' 
    OR file_url LIKE '%10.%'
    OR file_url LIKE '%172.1%'
    OR file_url LIKE '%172.2%'
    OR file_url LIKE '%172.3%'
    OR file_url LIKE '%localhost%'
    OR file_url LIKE '%127.0.0.1%'
    -- Or any full URL (starts with http:// or https://)
    OR file_url LIKE 'http://%'
    OR file_url LIKE 'https://%'
  )
  AND file_url NOT LIKE '/api/videos/stream/%'; -- Don't update if already relative

-- Verify the update
SELECT 
    id,
    file_name,
    file_url,
    category,
    created_at
FROM files.video_storage
WHERE is_deleted = false
ORDER BY created_at DESC
LIMIT 10;

-- Count how many videos were updated
SELECT 
    COUNT(*) as total_videos,
    COUNT(CASE WHEN file_url LIKE '/api/videos/stream/%' THEN 1 END) as relative_path_count,
    COUNT(CASE WHEN file_url LIKE 'http%' THEN 1 END) as full_url_count
FROM files.video_storage
WHERE is_deleted = false;


