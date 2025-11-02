package com.QhomeBase.assetmaintenanceservice.model;

public enum AssetType {
    ELEVATOR("Thang máy"),
    PARKING("Bãi đỗ xe"),
    GYM("Phòng gym"),
    POOL("Hồ bơi"),
    SECURITY_SYSTEM("Hệ thống an ninh"),
    CCTV("Camera giám sát"),
    FIRE_SAFETY("Hệ thống phòng cháy chữa cháy"),
    WATER_SUPPLY("Hệ thống cấp nước"),
    ELECTRICAL("Hệ thống điện"),
    AIR_CONDITIONING("Hệ thống điều hòa"),
    LIGHTING("Hệ thống chiếu sáng"),
    LIFT("Thang bộ"),
    GATE("Cổng vào"),
    GUARDHOUSE("Nhà bảo vệ"),
    GARDEN("Vườn cây"),
    WASTE_MANAGEMENT("Hệ thống quản lý rác"),
    INTERNET("Hệ thống internet"),
    OTHER("Khác");

    private final String description;

    AssetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

