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

    private boolean sameTenant(UUID tenantId) {
        var p = principal();
        return p.tenant() != null && p.tenant().equals(tenantId);
    }

    public boolean canManageTenant(UUID tenantId) {
        var p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean sameTenant = p.tenant() != null && p.tenant().equals(tenantId);
        boolean okRole = p.roles() != null && (p.roles().contains("tenant_manager") || p.roles().contains("tenant_owner"));
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.tenant.delete.request"));
        return sameTenant && (okRole || okPerm);
    }

    public boolean canRequestDeleteTenant(UUID tenantId) {
        var p = principal();
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.tenant.delete.request");
        
        System.out.println("=== canRequestDeleteTenant Debug ===");
        System.out.println("Tenant ID: " + tenantId);
        System.out.println("User Principal: " + p);
        System.out.println("User Roles: " + p.roles());
        System.out.println("User Perms: " + p.perms());
        System.out.println("okRole: " + okRole);
        System.out.println("okPerm: " + okPerm);
        System.out.println("Final Result: " + (okRole || okPerm));
        System.out.println("=====================================");
        
        return okRole || okPerm;
    }

    public boolean canApproveTicket(UUID ticketTenantId) {
        var p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean sameTenant = p.tenant() != null && p.tenant().equals(ticketTenantId);
        boolean okRole = p.roles() != null && (p.roles().contains("tenant_manager") || p.roles().contains("tenant_owner"));
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.tenant.delete.approve"));
        return sameTenant && (okRole || okPerm);
    }
    public boolean canCreateBuilding() {
        var p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.building.create"));
        
        return okPerm;
    }
    public boolean canUpdateBuilding() {
        var p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.building.update"));



        return okPerm;
    }
    public boolean canRequestDeleteBuilding(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.building.delete.request");
        boolean result = st && (okRole || okPerm);
        return result;
    }

    public boolean canApproveDeleteBuilding(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        boolean result = st && (okRole || okPerm);
        return result;
    }

    public boolean canDeleteBuilding() {
        boolean okPerm = hasPerm("base.building.delete");
        return okPerm;
    }

    public boolean canApproveBuildingDeletion() {
        boolean okRole = hasAnyRole(Set.of("admin", "tenant_owner", "tenant_manager"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

    public boolean canCompleteBuildingDeletion() {
        boolean okRole = hasAnyRole(Set.of("admin", "tenant_owner", "tenant_manager"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

    public boolean canRejectDeleteBuilding(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        boolean result = st && (okRole || okPerm);
        return result;
    }

    public boolean canViewDeleteBuilding(UUID id) {

        boolean okPerm = hasPerm("base.building.delete.request") || hasPerm("base.building.delete.approve");
        return okPerm;
    }

    public boolean canViewAllDeleteBuildings() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }
    public boolean canCreateUnit(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.unit.create");
        return st && (okRole || okPerm);
    }

    public boolean canUpdateUnit(UUID unitId) {
        
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.unit.update");
        return okRole || okPerm;
    }

    public boolean canViewUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm;
    }

    public boolean canViewUnits() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm;
    }

    public boolean canDeleteUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.unit.delete");
        return okRole || okPerm;
    }

    public boolean canManageUnitStatus(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.unit.status.manage");
        return okRole || okPerm;
    }

    public boolean canViewUnitsByTenant(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.unit.view");
        return st && (okRole || okPerm);
    }

    public boolean canViewUnitsByBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm;
    }


    public boolean canCreateVehicle(UUID tenantId) {
        boolean st = sameTenant(tenantId);

        return st;
    }

    public boolean canUpdateVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.update");
        return okRole || okPerm;
    }

    public boolean canViewVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicles() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canDeleteVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.vehicle.delete");
        return okRole || okPerm;
    }

    public boolean canManageVehicleStatus(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.vehicle.status.manage");
        return okRole || okPerm;
    }

    public boolean canViewVehiclesByTenant(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return st && (okRole || okPerm);
    }

    public boolean canViewVehiclesByResident(UUID residentId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canViewVehiclesByUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canCreateVehicleRegistration(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        return st;
    }

    public boolean canApproveVehicleRegistration(UUID requestId) {

        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.vehicle.registration.approve");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistrations() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistrationsByTenant(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return st && (okRole || okPerm);
    }

    public boolean canViewVehicleRegistrationsByResident(UUID residentId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistrationsByUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewAllVehicleRegistrations() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canCancelVehicleRegistration(UUID requestId) {

        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident"));
        boolean okPerm = hasPerm("base.vehicle.registration.cancel");
        return okRole || okPerm;
    }

    public boolean canViewAllTenantDeletionRequests() {
        boolean okRole = hasAnyRole(Set.of("admin", "tenant_owner"));
        boolean okPerm = hasPerm("base.tenant.delete.approve");
        return okRole || okPerm;
    }

    public boolean canViewTenantDeletionRequest(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("admin", "tenant_owner"));
        boolean okPerm = hasPerm("base.tenant.delete.approve");
        return okRole || okPerm;
    }

}
