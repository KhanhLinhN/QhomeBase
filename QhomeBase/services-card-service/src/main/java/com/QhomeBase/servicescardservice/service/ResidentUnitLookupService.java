package com.QhomeBase.servicescardservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResidentUnitLookupService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<AddressInfo> resolveByResident(UUID residentId, UUID unitId) {
        if (residentId == null && unitId == null) {
            return Optional.empty();
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("residentId", residentId)
                .addValue("unitId", unitId);

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet("""
                SELECT u.code AS apartment_number,
                       COALESCE(b.name, b.code) AS building_name,
                       hm.resident_id,
                       r.full_name
                FROM data.household_members hm
                JOIN data.households h ON h.id = hm.household_id
                JOIN data.units u ON u.id = h.unit_id
                JOIN data.buildings b ON b.id = u.building_id
                JOIN data.residents r ON r.id = hm.resident_id
                WHERE (:residentId IS NULL OR hm.resident_id = :residentId)
                  AND (:unitId IS NULL OR h.unit_id = :unitId)
                  AND (hm.left_at IS NULL)
                ORDER BY hm.is_primary DESC NULLS LAST, hm.created_at DESC NULLS LAST
                LIMIT 1
                """, params);

        if (rowSet.next()) {
            return Optional.of(mapRow(rowSet));
        }

        if (unitId != null) {
            return resolveByUnit(unitId);
        }

        return Optional.empty();
    }

    public Optional<AddressInfo> resolveByUser(UUID userId, UUID unitId) {
        if (userId == null && unitId == null) {
            return Optional.empty();
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("unitId", unitId);

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet("""
                SELECT u.code AS apartment_number,
                       COALESCE(b.name, b.code) AS building_name,
                       hm.resident_id,
                       r.full_name
                FROM data.residents r
                LEFT JOIN data.household_members hm ON hm.resident_id = r.id AND hm.left_at IS NULL
                LEFT JOIN data.households h ON h.id = hm.household_id
                LEFT JOIN data.units u ON u.id = h.unit_id
                LEFT JOIN data.buildings b ON b.id = u.building_id
                WHERE (:userId IS NULL OR r.user_id = :userId)
                  AND (:unitId IS NULL OR h.unit_id = :unitId)
                ORDER BY hm.is_primary DESC NULLS LAST, hm.created_at DESC NULLS LAST
                LIMIT 1
                """, params);

        if (rowSet.next()) {
            AddressInfo info = mapRow(rowSet);
            if (info.apartmentNumber() != null || info.buildingName() != null) {
                return Optional.of(info);
            }
        }

        if (unitId != null) {
            return resolveByUnit(unitId);
        }

        return Optional.empty();
    }

    private Optional<AddressInfo> resolveByUnit(UUID unitId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("unitId", unitId);
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet("""
                SELECT u.code AS apartment_number,
                       COALESCE(b.name, b.code) AS building_name
                FROM data.units u
                LEFT JOIN data.buildings b ON b.id = u.building_id
                WHERE u.id = :unitId
                LIMIT 1
                """, params);

        if (rowSet.next()) {
            return Optional.of(new AddressInfo(
                    normalize(rowSet.getString("apartment_number")),
                    normalize(rowSet.getString("building_name")),
                    null,
                    null
            ));
        }
        return Optional.empty();
    }

    private AddressInfo mapRow(SqlRowSet rowSet) {
        UUID residentId = null;
        if (hasColumn(rowSet, "resident_id")) {
            Object value = rowSet.getObject("resident_id");
            if (value instanceof UUID uuid) {
                residentId = uuid;
            } else if (value instanceof String str && StringUtils.hasText(str)) {
                try {
                    residentId = UUID.fromString(str.trim());
                } catch (IllegalArgumentException ignored) {
                    log.debug("Cannot parse resident_id value {}", str);
                }
            }
        }
        return new AddressInfo(
                normalize(getString(rowSet, "apartment_number")),
                normalize(getString(rowSet, "building_name")),
                residentId,
                normalize(getString(rowSet, "full_name"))
        );
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean hasColumn(SqlRowSet rowSet, String column) {
        try {
            rowSet.findColumn(column);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getString(SqlRowSet rowSet, String column) {
        return hasColumn(rowSet, column) ? rowSet.getString(column) : null;
    }

    public record AddressInfo(String apartmentNumber, String buildingName, UUID residentId, String residentFullName) {
        public boolean hasAddress() {
            return StringUtils.hasText(apartmentNumber) || StringUtils.hasText(buildingName);
        }
    }
}


