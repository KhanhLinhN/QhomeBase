package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public Optional<Permission> getPermissionByCode(String code) {
        return permissionRepository.findByCode(code);
    }

    public List<Permission> getPermissionsByService(String servicePrefix) {
        return permissionRepository.findByCodePrefix(servicePrefix);
    }

    public List<Permission> getBaseServicePermissions() {
        return permissionRepository.findBaseServicePermissions();
    }

    public List<Permission> getIamServicePermissions() {
        return permissionRepository.findIamServicePermissions();
    }

    public List<Permission> getMaintenanceServicePermissions() {
        return permissionRepository.findMaintenanceServicePermissions();
    }

    public List<Permission> getFinanceServicePermissions() {
        return permissionRepository.findFinanceServicePermissions();
    }

    public List<Permission> getDocumentServicePermissions() {
        return permissionRepository.findDocumentServicePermissions();
    }

    public List<Permission> getReportServicePermissions() {
        return permissionRepository.findReportServicePermissions();
    }

    public List<Permission> getSystemServicePermissions() {
        return permissionRepository.findSystemServicePermissions();
    }

    public Permission createPermission(String code, String description) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setDescription(description);
        return permissionRepository.save(permission);
    }

    public Permission updatePermission(String code, String description) {
        Optional<Permission> existingPermission = permissionRepository.findByCode(code);
        if (existingPermission.isPresent()) {
            Permission permission = existingPermission.get();
            permission.setDescription(description);
            return permissionRepository.save(permission);
        }
        throw new IllegalArgumentException("Permission with code " + code + " not found");
    }

    public void deletePermission(String code) {
        permissionRepository.deleteById(code);
    }

    public boolean permissionExists(String code) {
        return permissionRepository.existsById(code);
    }
}
