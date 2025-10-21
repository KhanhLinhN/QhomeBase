package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.EmployeeDto;
import com.QhomeBase.iamservice.security.UserPrincipal;
import com.QhomeBase.iamservice.service.EmployeeManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeManagementController {

    private final EmployeeManagementService employeeManagementService;

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("@authz.canViewTenantEmployees(#tenantId)")
    public ResponseEntity<List<EmployeeDto>> getEmployeesInTenant(@PathVariable UUID tenantId) {
        List<EmployeeDto> employees = employeeManagementService.getEmployeesInTenant(tenantId);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@authz.canViewEmployeeDetails(#tenantId, #userId)")
    public ResponseEntity<EmployeeDto> getEmployeeDetails(
            @PathVariable UUID userId,
            @RequestParam UUID tenantId) {
        EmployeeDto employee = employeeManagementService.getEmployeeDetails(userId, tenantId);
        return ResponseEntity.ok(employee);
    }

    @PostMapping("/{userId}/assign")
    @PreAuthorize("@authz.canManageDepartment(#tenantId, null)")
    public ResponseEntity<EmployeeDto> assignEmployeeToTenant(
            @PathVariable UUID userId,
            @RequestParam UUID tenantId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        EmployeeDto employee = employeeManagementService.assignEmployeeToTenant(userId, tenantId, principal.username());
        return ResponseEntity.ok(employee);
    }

    @PostMapping("/{userId}/remove")
    @PreAuthorize("@authz.canManageDepartment(#tenantId, null)")
    public ResponseEntity<String> removeEmployeeFromTenant(
            @PathVariable UUID userId,
            @RequestParam UUID tenantId) {
        employeeManagementService.removeEmployeeFromTenant(userId, tenantId);
        return ResponseEntity.ok("Employee removed from tenant successfully");
    }

    @GetMapping("/available")
    @PreAuthorize("@authz.canManageDepartment(null, null)")
    public ResponseEntity<List<EmployeeDto>> getAvailableEmployees() {
        List<EmployeeDto> employees = employeeManagementService.getAvailableEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/tenant/{tenantId}/count")
    @PreAuthorize("@authz.canViewTenantEmployees(#tenantId)")
    public ResponseEntity<Long> countEmployeesInTenant(@PathVariable UUID tenantId) {
        long count = employeeManagementService.countEmployeesInTenant(tenantId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/tenant/{tenantId}/active")
    @PreAuthorize("@authz.canViewTenantEmployees(#tenantId)")
    public ResponseEntity<List<EmployeeDto>> getActiveEmployees(@PathVariable UUID tenantId) {
        List<EmployeeDto> employees = employeeManagementService.getEmployeesInTenant(tenantId)
                .stream()
                .filter(EmployeeDto::isActive)
                .toList();
        return ResponseEntity.ok(employees);
    }
}
