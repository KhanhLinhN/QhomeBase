package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.CreateUserForResidentDto;
import com.QhomeBase.iamservice.dto.UserAccountDto;
import com.QhomeBase.iamservice.dto.UserInfoDto;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import com.QhomeBase.iamservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
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
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserService userService;

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

    @PostMapping("/create-for-resident")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT') or hasAuthority('PERM_iam.user.create') or hasAuthority('PERM_base.resident.approve')")
    public ResponseEntity<UserAccountDto> createUserForResident(
            @Valid @RequestBody CreateUserForResidentDto request) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.QhomeBase.iamservice.security.UserPrincipal principal) {
                log.info("Creating user for resident: roles={}, perms={}", principal.roles(), principal.perms());
            } else {
                log.warn("No authentication found when creating user for resident");
            }
            User user;
            
            if (request.autoGenerate()) {
                if (request.username() == null || request.username().isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                if (request.email() == null || request.email().isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                user = userService.createUserWithAutoGeneratedPassword(
                        request.username(),
                        request.email(),
                        request.residentId()
                );
            } else {
                if (request.username() == null || request.username().isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                if (request.email() == null || request.email().isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                if (request.password() == null || request.password().isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                user = userService.createUserForResident(
                        request.username(),
                        request.email(),
                        request.password(),
                        request.residentId()
                );
            }
            
            UserAccountDto accountDto = new UserAccountDto(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRoles().stream()
                            .map(UserRole::getRoleName)
                            .collect(Collectors.toList()),
                    user.isActive()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(accountDto);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create user for resident: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating user for resident", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{userId}/account-info")
    @PreAuthorize("@authz.canViewUser(#userId)")
    @Transactional(readOnly = true)
    public ResponseEntity<UserAccountDto> getUserAccountInfo(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    // Force initialization of roles collection within transaction
                    List<String> roleNames = user.getRoles().stream()
                            .map(UserRole::getRoleName)
                            .collect(Collectors.toList());
                    
                    UserAccountDto accountDto = new UserAccountDto(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            roleNames,
                            user.isActive()
                    );
                    return ResponseEntity.ok(accountDto);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT') or hasAuthority('PERM_iam.user.read') or hasAuthority('PERM_base.resident.approve')")
    public ResponseEntity<UserInfoDto> getUserByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
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
    
    @GetMapping("/by-email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT') or hasAuthority('PERM_iam.user.read') or hasAuthority('PERM_base.resident.approve')")
    public ResponseEntity<UserInfoDto> getUserByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email)
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

    public record UserStatusResponse(
            boolean active,
            int failedLoginAttempts,
            boolean accountLocked,
            java.time.LocalDateTime lastLogin
    ) {}
}
