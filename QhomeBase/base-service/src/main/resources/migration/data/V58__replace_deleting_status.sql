- V58: Align existing building statuses with new MAINTENANCE flow
- Convert any legacy DELETING status to MAINTENANCE so the enum lookup succeeds

UPDATE data.buildings
SET status = 'MAINTENANCE'
WHERE status = 'DELETING';

