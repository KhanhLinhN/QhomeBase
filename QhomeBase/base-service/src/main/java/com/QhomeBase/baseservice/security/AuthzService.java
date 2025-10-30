package com.QhomeBase.baseservice.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service("authz")
public class AuthzService {
    private UserPrincipal principal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    
    private boolean hasPerm(String perm) {
        var p = principal();
        return p.perms() != null && p.perms().contains(perm);
    }

    private boolean hasAnyRole(Set<String> rolesNeed) {
        var p = principal();
        if (p.roles() == null) return false;
        for (String r : rolesNeed) {
            if (p.roles().contains(r)) return true;
        }
        return false;
    }
    
    private boolean isGlobalAdmin() {
        var p = principal();
        return hasAnyRole(Set.of("admin"));
    }

    // ========== Building Permissions ==========
    
    public boolean canCreateBuilding() {
        boolean okPerm = hasPerm("base.building.create");
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        return okPerm || okRole;
    }
    
    public boolean canUpdateBuilding() {
        boolean okPerm = hasPerm("base.building.update");
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        return okPerm || okRole;
    }
    
    public boolean canRequestDeleteBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.request");
        return okRole || okPerm;
    }

    public boolean canApproveDeleteBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

    public boolean canDeleteBuilding() {
        boolean okPerm = hasPerm("base.building.delete");
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        return okPerm || okRole;
    }

    public boolean canApproveBuildingDeletion() {
        boolean okRole = hasAnyRole(Set.of("admin", "owner", "manager"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

    public boolean canCompleteBuildingDeletion() {
        boolean okRole = hasAnyRole(Set.of("admin", "owner", "manager"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

    public boolean canRejectDeleteBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

    public boolean canViewDeleteBuilding(UUID id) {
        boolean okPerm = hasPerm("base.building.delete.request") || hasPerm("base.building.delete.approve");
        return okPerm;
    }

    public boolean canViewAllDeleteBuildings() {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

    // ========== Unit Permissions ==========
    
    public boolean canCreateUnit(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.unit.create");
        return okRole || okPerm;
    }

    public boolean canUpdateUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.unit.update");
        return okRole || okPerm;
    }

    public boolean canViewUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm;
    }

    public boolean canViewUnits() {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm;
    }

    public boolean canDeleteUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.unit.delete");
        return okRole || okPerm;
    }

    public boolean canManageUnitStatus(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.unit.status.manage");
        return okRole || okPerm;
    }

    public boolean canViewUnitsByBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm;
    }

    // ========== Vehicle Permissions ==========

    public boolean canCreateVehicle() {
        return true; // Any authenticated user can create vehicle
    }

    public boolean canUpdateVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.update");
        return okRole || okPerm;
    }

    public boolean canViewVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicles() {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canDeleteVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.delete");
        return okRole || okPerm;
    }

    public boolean canManageVehicleStatus(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.status.manage");
        return okRole || okPerm;
    }

    public boolean canViewVehiclesByResident(UUID residentId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canViewVehiclesByUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "unit_owner"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    // ========== Vehicle Registration Permissions ==========
    
    public boolean canCreateVehicleRegistration() {
        return true; // Any authenticated user can request vehicle registration
    }

    public boolean canApproveVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.approve");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistrations() {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistrationsByResident(UUID residentId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistrationsByUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "unit_owner"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewAllVehicleRegistrations() {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canCancelVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("manager", "owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.registration.cancel");
        return okRole || okPerm;
    }
}
