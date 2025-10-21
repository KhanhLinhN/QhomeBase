package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.AvailablePermissionDto;
import com.QhomeBase.iamservice.dto.AvailableRoleDto;
import com.QhomeBase.iamservice.dto.EmployeeRoleDto;
import com.QhomeBase.iamservice.dto.RoleRemovalRequest;
import com.QhomeBase.iamservice.security.UserPrincipal;
import com.QhomeBase.iamservice.service.EmployeeRoleManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employee-roles")
@RequiredArgsConstructor
public class EmployeeRoleManagementController {

    private final EmployeeRoleManagementService employeeRoleManagementService;




    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("@authz.canViewTenantEmployees(#tenantId)")
    public ResponseEntity<List<EmployeeRoleDto>> getEmployeesInTenant(@PathVariable UUID tenantId) {
        List<EmployeeRoleDto> employees = employeeRoleManagementService.getEmployeesInTenant(tenantId);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employee/{userId}")
    @PreAuthorize("@authz.canViewEmployeeDetails(#tenantId, #userId)")
    public ResponseEntity<EmployeeRoleDto> getEmployeeDetails(
            @PathVariable UUID userId,
            @RequestParam UUID tenantId) {
        EmployeeRoleDto employee = employeeRoleManagementService.getEmployeeDetails(userId, tenantId);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/available-roles/{tenantId}")
    @PreAuthorize("@authz.canViewTenantEmployees(#tenantId)")
    public ResponseEntity<List<AvailableRoleDto>> getAvailableRoles(
            @PathVariable UUID tenantId) {
        List<AvailableRoleDto> roles = 
                employeeRoleManagementService.getAvailableRolesForTenant(tenantId);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/available-permissions")
    @PreAuthorize("hasAnyRole('admin', 'tenant_owner')")
    public ResponseEntity<List<AvailablePermissionDto>> getAvailablePermissionsGroupedByService() {
        List<AvailablePermissionDto> permissions = employeeRoleManagementService.getAvailablePermissionsGroupedByService();
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/assign")
    @PreAuthorize("@authz.canAssignEmployeeRole(#tenantId, #userId)")
    public ResponseEntity<String> assignRolesToEmployee(
            @RequestParam UUID userId,
            @RequestParam UUID tenantId,
            @RequestBody List<String> roleNames,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        employeeRoleManagementService.assignRolesToEmployee(userId, tenantId, roleNames, principal.username());
        return ResponseEntity.ok("Roles assigned successfully");
    }

    @PostMapping("/remove")
    @PreAuthorize("@authz.canRemoveEmployeeRole(#request.tenantId, #request.userId)")
    public ResponseEntity<String> removeRolesFromEmployee(
            @RequestBody RoleRemovalRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        employeeRoleManagementService.removeRolesFromEmployee(request, principal.username());
        return ResponseEntity.ok("Roles removed successfully");
    }

    @GetMapping("/employee/{userId}/permissions")
    @PreAuthorize("@authz.canViewEmployeeDetails(#tenantId, #userId)")
    public ResponseEntity<List<String>> getEmployeePermissions(
            @PathVariable UUID userId,
            @RequestParam UUID tenantId) {
        List<String> permissions = employeeRoleManagementService.getEmployeePermissions(userId, tenantId);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/tenant/{tenantId}/department/{department}")
    @PreAuthorize("@authz.canViewTenantEmployees(#tenantId)")
    public ResponseEntity<List<EmployeeRoleDto>> getEmployeesByDepartment(
            @PathVariable UUID tenantId,
            @PathVariable String department) {
        List<EmployeeRoleDto> employees = employeeRoleManagementService.getEmployeesInTenant(tenantId)
                .stream()
                .filter(emp -> department.equals(emp.getDepartment()))
                .toList();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/tenant/{tenantId}/role/{roleName}")
    @PreAuthorize("@authz.canViewTenantEmployees(#tenantId)")
    public ResponseEntity<List<EmployeeRoleDto>> getEmployeesByRole(
            @PathVariable UUID tenantId,
            @PathVariable String roleName) {
        List<EmployeeRoleDto> employees = employeeRoleManagementService.getEmployeesInTenant(tenantId)
                .stream()
                .filter(emp -> emp.getAssignedRoles().stream()
                        .anyMatch(role -> roleName.equals(role.getRoleName())))
                .toList();
        return ResponseEntity.ok(employees);
    }
}
