package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.UserInfoDto;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @GetMapping("/{userId}")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<UserInfoDto> getUserInfo(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    List<UserRole> userRoles = user.getRoles();
                    List<String> roleNames = userRoles != null && !userRoles.isEmpty()
                            ? userRoles.stream()
                                .map(UserRole::getRoleName)
                                .collect(Collectors.toList())
                            : List.of();

                    Set<String> permissionsSet = new HashSet<>();
                    if (userRoles != null && !userRoles.isEmpty()) {
                        for (UserRole role : userRoles) {
                            List<String> rolePerms = rolePermissionRepository.findPermissionCodesByRole(role.name());
                            permissionsSet.addAll(rolePerms);
                        }
                    }
                    List<String> permissions = new ArrayList<>(permissionsSet);

                    UserInfoDto userInfo = new UserInfoDto(
                            user.getId().toString(),
                            user.getUsername(),
                            user.getEmail(),
                            roleNames,
                            permissions
                    );
                    return ResponseEntity.ok(userInfo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/status")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<UserStatusResponse> getUserStatus(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    UserStatusResponse status = new UserStatusResponse(
                            user.isActive(),
                            user.getFailedLoginAttempts(),
                            user.isAccountLocked(),
                            user.getLastLogin()
                    );
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available-staff")
    @PreAuthorize("@authz.canViewAllUsers()")
    public ResponseEntity<List<UserInfoDto>> getAvailableStaff() {
        try {
            List<UserInfoDto> availableStaff = userRepository.findAvailableStaff()
                    .stream()
                    .map(user -> {
                        List<UserRole> userRoles = user.getRoles();
                        List<String> roleNames = userRoles != null && !userRoles.isEmpty()
                                ? userRoles.stream()
                                    .map(UserRole::getRoleName)
                                    .collect(Collectors.toList())
                                : List.of();
                        
                        Set<String> permissionsSet = new HashSet<>();
                        if (userRoles != null && !userRoles.isEmpty()) {
                            for (UserRole role : userRoles) {
                                List<String> rolePerms = rolePermissionRepository.findPermissionCodesByRole(role.name());
                                permissionsSet.addAll(rolePerms);
                            }
                        }
                        List<String> permissions = new ArrayList<>(permissionsSet);
                        
                        return new UserInfoDto(
                                user.getId().toString(),
                                user.getUsername(),
                                user.getEmail(),
                                roleNames,
                                permissions
                        );
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(availableStaff);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public record UserStatusResponse(
            boolean active,
            int failedLoginAttempts,
            boolean accountLocked,
            java.time.LocalDateTime lastLogin
    ) {}
}
