-- 1. Check electric service ID
SELECT id, name, code, service_type 
FROM data.services 
WHERE service_type = 'ELECTRIC';

-- 2. Check meters for unassigned units - with service info
SELECT 
    u.code as unit_code,
    b.code as building_code,
    m.id as meter_id,
    m.meter_code,
    m.active,
    m.service_id,
    s.name as service_name,
    s.service_type
FROM data.units u
JOIN data.buildings b ON u.building_id = b.id
LEFT JOIN data.meters m ON m.unit_id = u.id
LEFT JOIN data.services s ON m.service_id = s.id
WHERE u.code IN ('B---03', 'C---01', 'C---02', 'J---02', 'J---03')
ORDER BY u.code;

-- 3. Count meters by active status for these units
SELECT 
    COALESCE(m.active, FALSE) as is_active,
    COUNT(*) as meter_count
FROM data.units u
LEFT JOIN data.meters m ON m.unit_id = u.id
WHERE u.code IN ('B---03', 'C---01', 'C---02', 'C---03', 'D---01', 'E---01', 'F---01', 'G---01', 'J---02', 'J---03')
GROUP BY m.active;

-- 4. Check if meters exist but not linked to electric service
SELECT 
    u.code,
    COUNT(m.id) as total_meters,
    SUM(CASE WHEN s.service_type = 'ELECTRIC' THEN 1 ELSE 0 END) as electric_meters,
    SUM(CASE WHEN m.active THEN 1 ELSE 0 END) as active_meters
FROM data.units u
LEFT JOIN data.meters m ON m.unit_id = u.id
LEFT JOIN data.services s ON m.service_id = s.id
WHERE u.code IN ('B---03', 'C---01', 'C---02', 'J---02', 'J---03')
GROUP BY u.code
ORDER BY u.code;

