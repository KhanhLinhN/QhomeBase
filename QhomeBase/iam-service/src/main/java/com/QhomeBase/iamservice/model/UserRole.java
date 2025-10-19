package com.QhomeBase.iamservice.model;

public enum UserRole {
    ACCOUNT("Account"),
    ADMIN("Administrator"),
    TENANT_OWNER("Tenant Owner"),
    TECHNICIAN("Technician"),
    SUPPORTER("Supporter");
    
    private final String description;
    
    UserRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    public boolean isTenantOwner() {
        return this == TENANT_OWNER;
    }
    
    public boolean isTechnician() {
        return this == TECHNICIAN;
    }
    
    public boolean isSupporter() {
        return this == SUPPORTER;
    }
    
    public boolean isAccount() {
        return this == ACCOUNT;
    }
}