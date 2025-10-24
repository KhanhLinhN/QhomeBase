package com.QhomeBase.customerinteractionservice.security;

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
        boolean okRole = p.roles() != null && (p.roles().contains("tenant_manager") || p.roles().contains("tenant_owner") || p.roles().contains("admin"));
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.tenant.delete.request"));
        return (sameTenant && okRole) || okPerm || hasAnyRole(Set.of("admin"));
    }

    public boolean canRequestDeleteTenant(UUID tenantId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.tenant.delete.request");
        return okRole || okPerm;
    }

    public boolean canApproveTicket(UUID ticketTenantId) {
        var p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean sameTenant = p.tenant() != null && p.tenant().equals(ticketTenantId);
        boolean okRole = p.roles() != null && (p.roles().contains("tenant_manager") || p.roles().contains("tenant_owner") || p.roles().contains("admin"));
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.tenant.delete.approve"));
        return (sameTenant && okRole) || okPerm || hasAnyRole(Set.of("admin"));
    }
    public boolean canCreateBuilding() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.create");
        return okRole || okPerm;
    }
    
    public boolean canUpdateBuilding() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.update");
        return okRole || okPerm;
    }
    public boolean canRequestDeleteBuilding(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.request");
        return (st && okRole) || okPerm || hasAnyRole(Set.of("admin"));
    }

    public boolean canApproveDeleteBuilding(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return (st && okRole) || okPerm || hasAnyRole(Set.of("admin"));
    }

    public boolean canDeleteBuilding() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete");
        return okRole || okPerm;
    }

    public boolean canCompleteBuildingDeletion() {
        boolean okRole = hasAnyRole(Set.of("admin", "tenant_owner", "tenant_manager"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

    public boolean canRejectDeleteBuilding(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return (st && okRole) || okPerm || hasAnyRole(Set.of("admin"));
    }

    public boolean canViewDeleteBuilding(UUID id) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.request") || hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

    public boolean canViewAllDeleteBuildings() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }
    public boolean canCreateUnit(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.unit.create");
        return (st && okRole) || okPerm || hasAnyRole(Set.of("admin"));
    }

    public boolean canUpdateUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.unit.update");
        return okRole || okPerm;
    }

    public boolean canViewUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "admin"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm;
    }

    public boolean canViewUnits() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm;
    }

    public boolean canDeleteUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.unit.delete");
        return okRole || okPerm;
    }

    public boolean canManageUnitStatus(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.unit.status.manage");
        return okRole || okPerm;
    }

    public boolean canViewUnitsByTenant(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.unit.view");
        return (st && okRole) || okPerm || hasAnyRole(Set.of("admin"));
    }

    public boolean canViewUnitsByBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm;
    }


    public boolean canCreateVehicle(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("admin"));
        return st || okRole;
    }

    public boolean canUpdateVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident", "admin"));
        boolean okPerm = hasPerm("base.vehicle.update");
        return okRole || okPerm;
    }

    public boolean canViewVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident", "admin"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicles() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canDeleteVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.delete");
        return okRole || okPerm;
    }

    public boolean canManageVehicleStatus(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.status.manage");
        return okRole || okPerm;
    }

    public boolean canViewVehiclesByTenant(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return (st && okRole) || okPerm || hasAnyRole(Set.of("admin"));
    }

    public boolean canViewVehiclesByResident(UUID residentId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident", "admin"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canViewVehiclesByUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm;
    }

    public boolean canCreateVehicleRegistration(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("admin"));
        return st || okRole;
    }

    public boolean canApproveVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.approve");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistrations() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistrationsByTenant(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return (st && okRole) || okPerm || hasAnyRole(Set.of("admin"));
    }

    public boolean canViewVehicleRegistrationsByResident(UUID residentId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewVehicleRegistrationsByUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canViewAllVehicleRegistrations() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm;
    }

    public boolean canCancelVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "unit_owner", "resident", "admin"));
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

    public boolean canCreateNews() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("content.news.create");
        return okRole || okPerm;
    }

    public boolean canUpdateNews() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("content.news.update");
        return okRole || okPerm;
    }

    public boolean canDeleteNews() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("content.news.delete");
        return okRole || okPerm;
    }

    public boolean canViewNews() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("content.news.view");
        return okRole || okPerm;
    }

    public boolean canPublishNews() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("content.news.publish");
        return okRole || okPerm;
    }

    public boolean canViewAllNews() {
        boolean okRole = hasAnyRole(Set.of("admin", "tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("content.news.view");
        return okRole || okPerm;
    }

    public boolean canReadNews(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("resident", "unit_owner"));
        return st && okRole;
    }

    public boolean canUploadNewsImage() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("content.news.image.upload");
        return okRole || okPerm;
    }

    public boolean canDeleteNewsImage() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("content.news.image.delete");
        return okRole || okPerm;
    }

    public boolean canUpdateNewsImage() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("content.news.image.update");
        return okRole || okPerm;
    }

}
