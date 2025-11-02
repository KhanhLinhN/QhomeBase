-- Drop news_views and notification_views tables
-- These tables were used to track read status per user/resident
-- Removing read status tracking feature

-- Drop notification_views table and related indexes
DROP INDEX IF EXISTS content.ix_notification_views_read_at;
DROP INDEX IF EXISTS content.ix_notification_views_user;
DROP INDEX IF EXISTS content.ix_notification_views_resident;
DROP INDEX IF EXISTS content.ix_notification_views_notification;
DROP INDEX IF EXISTS content.uq_notification_views_user;
DROP INDEX IF EXISTS content.uq_notification_views_resident;
DROP TABLE IF EXISTS content.notification_views CASCADE;

-- Drop news_views table and related indexes
DROP INDEX IF EXISTS content.ix_news_views_user;
DROP INDEX IF EXISTS content.ix_news_views_resident;
DROP INDEX IF EXISTS content.ix_news_views_lookup;
DROP INDEX IF EXISTS content.uq_news_views_user;
DROP INDEX IF EXISTS content.uq_news_views_resident;
DROP TABLE IF EXISTS content.news_views CASCADE;


