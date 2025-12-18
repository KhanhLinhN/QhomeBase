-- Check meters cho các căn hộ chưa assign
SELECT 
    u.code as unit_code,
    u.building_id,
    b.code as building_code,
    m.id as meter_id,
    m.meter_code,
    m.active,
    m.service_id,
    s.name as service_name
FROM data.units u
LEFT JOIN data.buildings b ON u.building_id = b.id
LEFT JOIN data.meters m ON m.unit_id = u.id
LEFT JOIN data.services s ON m.service_id = s.id
WHERE u.code IN ('B---03', 'C---01', 'C---02', 'C---03', 'D---01', 'E---01', 'F---01', 'G---01', 'J---02', 'J---03')
ORDER BY u.code;

-- Count active vs inactive meters
SELECT 
    CASE WHEN m.active THEN 'Active' ELSE 'Inactive' END as meter_status,
    COUNT(*) as count
FROM data.meters m
JOIN data.units u ON m.unit_id = u.id
WHERE u.code IN ('B---03', 'C---01', 'C---02', 'C---03', 'D---01', 'E---01', 'F---01', 'G---01', 'J---02', 'J---03')
GROUP BY m.active;

-- Check if meters have correct service_id (assuming electric service)
SELECT 
    s.id as service_id,
    s.name as service_name,
    s.service_type,
    COUNT(m.id) as meter_count
FROM data.services s
LEFT JOIN data.meters m ON m.service_id = s.id
WHERE s.service_type = 'ELECTRIC'
GROUP BY s.id, s.name, s.service_type;

