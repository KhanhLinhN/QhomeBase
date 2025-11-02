package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.PermissionRepository;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public List<UserRole> getAllRoles() {
        return List.of(UserRole.values());
    }

    public List<Permission> getPermissionsByRole(UserRole role) {
        String roleCode = role.name().toLowerCase();
        return rolePermissionRepository.findPermissionObjectsByRole(roleCode);
    }

    public List<Permission> getAccountantPermissions() {
        return getPermissionsByRole(UserRole.ACCOUNTANT);
    }

    public List<Permission> getAdminPermissions() {
        return getPermissionsByRole(UserRole.ADMIN);
    }

    public List<Permission> getTechnicianPermissions() {
        return getPermissionsByRole(UserRole.TECHNICIAN);
    }

    public List<Permission> getSupporterPermissions() {
        return getPermissionsByRole(UserRole.SUPPORTER);
    }

    public boolean hasPermission(UserRole role, String permissionCode) {
        List<Permission> rolePermissions = getPermissionsByRole(role);
        return rolePermissions.stream()
                .anyMatch(permission -> permission.getCode().equals(permissionCode));
    }
}

import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.PermissionRepository;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public List<UserRole> getAllRoles() {
        return List.of(UserRole.values());
    }

    public List<Permission> getPermissionsByRole(UserRole role) {
        String roleCode = role.name().toLowerCase();
        return rolePermissionRepository.findPermissionObjectsByRole(roleCode);
    }

    public List<Permission> getAccountantPermissions() {
        return getPermissionsByRole(UserRole.ACCOUNTANT);
    }

    public List<Permission> getAdminPermissions() {
        return getPermissionsByRole(UserRole.ADMIN);
    }

    public List<Permission> getTechnicianPermissions() {
        return getPermissionsByRole(UserRole.TECHNICIAN);
    }

    public List<Permission> getSupporterPermissions() {
        return getPermissionsByRole(UserRole.SUPPORTER);
    }

    public boolean hasPermission(UserRole role, String permissionCode) {
        List<Permission> rolePermissions = getPermissionsByRole(role);
        return rolePermissions.stream()
                .anyMatch(permission -> permission.getCode().equals(permissionCode));
    }
}
