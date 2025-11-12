# Asset Maintenance Service - API Endpoints

## Base Path
`/api/asset-maintenance`

---

## 1. Asset Management APIs

### 1.1 Get Assets List
**GET** `/api/asset-maintenance/assets`

**Query Parameters:**
- `buildingId` (UUID, optional) - Filter by building
- `unitId` (UUID, optional) - Filter by unit
- `assetType` (AssetType enum, optional) - Filter by asset type
- `status` (AssetStatus enum, optional) - Filter by status
- `isDeleted` (boolean, optional, default: false) - Include deleted assets
- `page` (int, optional, default: 0)
- `size` (int, optional, default: 20)
- `sort` (string, optional, default: "createdAt,desc")

**Response:** `Page<AssetResponse>`

---

### 1.2 Get Asset by ID
**GET** `/api/asset-maintenance/assets/{id}`

**Path Variables:**
- `id` (UUID) - Asset ID

**Response:** `AssetDetailResponse`

---

### 1.3 Create Asset
**POST** `/api/asset-maintenance/assets`

**Request Body:** `CreateAssetRequest`

**Response:** `AssetResponse`

**Permissions:** ADMIN, MANAGER

---

### 1.4 Update Asset
**PUT** `/api/asset-maintenance/assets/{id}`

**Path Variables:**
- `id` (UUID) - Asset ID

**Request Body:** `UpdateAssetRequest`

**Response:** `AssetResponse`

**Permissions:** ADMIN, MANAGER

---

### 1.5 Delete Asset (Soft Delete)
**DELETE** `/api/asset-maintenance/assets/{id}`

**Path Variables:**
- `id` (UUID) - Asset ID

**Response:** `void`

**Permissions:** ADMIN, MANAGER

---

### 1.6 Restore Deleted Asset
**PUT** `/api/asset-maintenance/assets/{id}/restore`

**Path Variables:**
- `id` (UUID) - Asset ID

**Response:** `AssetResponse`

**Permissions:** ADMIN, MANAGER

---

### 1.7 Upload Asset Images
**POST** `/api/asset-maintenance/assets/upload-images`

**Request:** `multipart/form-data` - files array

**Response:** `{ "imageUrls": List<String> }`

---

### 1.8 Get Assets by Building
**GET** `/api/asset-maintenance/assets/by-building/{buildingId}`

**Path Variables:**
- `buildingId` (UUID) - Building ID

**Query Parameters:** Same as Get Assets List

**Response:** `List<AssetResponse>`

---

### 1.9 Get Assets by Unit
**GET** `/api/asset-maintenance/assets/by-unit/{unitId}`

**Path Variables:**
- `unitId` (UUID) - Unit ID

**Query Parameters:** Same as Get Assets List

**Response:** `List<AssetResponse>`

---

## 2. Supplier Management APIs

### 2.1 Get Suppliers List
**GET** `/api/asset-maintenance/suppliers`

**Query Parameters:**
- `type` (string, optional) - Filter by supplier type (SUPPLIER only)
- `isActive` (boolean, optional) - Filter by active status
- `search` (string, optional) - Search by name, phone, email
- `page` (int, optional, default: 0)
- `size` (int, optional, default: 20)
- `sort` (string, optional, default: "name,asc")

**Response:** `Page<SupplierResponse>`

---

### 2.2 Get Supplier by ID
**GET** `/api/asset-maintenance/suppliers/{id}`

**Path Variables:**
- `id` (UUID) - Supplier ID

**Response:** `SupplierResponse`

---

### 2.3 Create Supplier
**POST** `/api/asset-maintenance/suppliers`

**Request Body:** `CreateSupplierRequest`

**Response:** `SupplierResponse`

**Permissions:** ADMIN, MANAGER

**Note:** Supplier chỉ dùng cho nhà cung cấp thiết bị (PURCHASE). Maintenance và warranty do nội bộ.

---

### 2.4 Update Supplier
**PUT** `/api/asset-maintenance/suppliers/{id}`

**Path Variables:**
- `id` (UUID) - Supplier ID

**Request Body:** `UpdateSupplierRequest`

**Response:** `SupplierResponse`

**Permissions:** ADMIN, MANAGER

---

### 2.5 Delete Supplier
**DELETE** `/api/asset-maintenance/suppliers/{id}`

**Path Variables:**
- `id` (UUID) - Supplier ID

**Response:** `void`

**Permissions:** ADMIN, MANAGER

---

### 2.6 Activate/Deactivate Supplier
**PUT** `/api/asset-maintenance/suppliers/{id}/toggle-active`

**Path Variables:**
- `id` (UUID) - Supplier ID

**Request Body:** `{ "isActive": boolean }`

**Response:** `SupplierResponse`

**Permissions:** ADMIN, MANAGER

---

### 2.7 Get Suppliers by Asset
**GET** `/api/asset-maintenance/suppliers/by-asset/{assetId}`

**Path Variables:**
- `assetId` (UUID) - Asset ID

**Response:** `List<SupplierResponse>`

---

## 3. Asset-Supplier Relationship APIs

### 3.1 Link Asset to Supplier
**POST** `/api/asset-maintenance/assets/{assetId}/suppliers/{supplierId}`

**Path Variables:**
- `assetId` (UUID) - Asset ID
- `supplierId` (UUID) - Supplier ID

**Request Body:**
```json
{
  "purchaseDate": "2024-01-15",
  "purchasePrice": 500000000,
  "warrantyStartDate": "2024-01-15",
  "warrantyEndDate": "2026-01-15",
  "warrantyProvider": "Supplier Name",
  "warrantyContact": "contact info",
  "notes": "notes"
}
```

**Response:** `AssetSupplierResponse`

**Permissions:** ADMIN, MANAGER

**Note:** relationshipType = "PURCHASE" (fixed)

---

### 3.2 Unlink Asset from Supplier
**DELETE** `/api/asset-maintenance/assets/{assetId}/suppliers/{supplierId}`

**Path Variables:**
- `assetId` (UUID) - Asset ID
- `supplierId` (UUID) - Supplier ID

**Response:** `void`

**Permissions:** ADMIN, MANAGER

---

## 4. Maintenance Schedule Management APIs

### 4.1 Get Maintenance Schedules
**GET** `/api/asset-maintenance/maintenance/schedules`

**Query Parameters:**
- `assetId` (UUID, optional) - Filter by asset
- `maintenanceType` (string, optional) - Filter by maintenance type
- `isActive` (boolean, optional) - Filter by active status
- `assignedTo` (UUID, optional) - Filter by assigned technician
- `nextMaintenanceDateFrom` (LocalDate, optional) - Filter by next maintenance date from
- `nextMaintenanceDateTo` (LocalDate, optional) - Filter by next maintenance date to
- `page` (int, optional, default: 0)
- `size` (int, optional, default: 20)
- `sort` (string, optional, default: "nextMaintenanceDate,asc")

**Response:** `Page<MaintenanceScheduleResponse>`

---

### 4.2 Get Maintenance Schedule by ID
**GET** `/api/asset-maintenance/maintenance/schedules/{id}`

**Path Variables:**
- `id` (UUID) - Schedule ID

**Response:** `MaintenanceScheduleResponse`

---

### 4.3 Create Maintenance Schedule
**POST** `/api/asset-maintenance/maintenance/schedules`

**Request Body:** `CreateMaintenanceScheduleRequest`

**Response:** `MaintenanceScheduleResponse`

**Permissions:** ADMIN, MANAGER

---

### 4.4 Update Maintenance Schedule
**PUT** `/api/asset-maintenance/maintenance/schedules/{id}`

**Path Variables:**
- `id` (UUID) - Schedule ID

**Request Body:** `UpdateMaintenanceScheduleRequest`

**Response:** `MaintenanceScheduleResponse`

**Permissions:** ADMIN, MANAGER

---

### 4.5 Delete Maintenance Schedule
**DELETE** `/api/asset-maintenance/maintenance/schedules/{id}`

**Path Variables:**
- `id` (UUID) - Schedule ID

**Response:** `void`

**Permissions:** ADMIN, MANAGER

---

### 4.6 Activate/Deactivate Schedule
**PUT** `/api/asset-maintenance/maintenance/schedules/{id}/toggle-active`

**Path Variables:**
- `id` (UUID) - Schedule ID

**Request Body:** `{ "isActive": boolean }`

**Response:** `MaintenanceScheduleResponse`

**Permissions:** ADMIN, MANAGER

---

### 4.7 Get Schedules by Asset
**GET** `/api/asset-maintenance/maintenance/schedules/by-asset/{assetId}`

**Path Variables:**
- `assetId` (UUID) - Asset ID

**Response:** `List<MaintenanceScheduleResponse>`

---

### 4.8 Get Upcoming Maintenance Schedules
**GET** `/api/asset-maintenance/maintenance/schedules/upcoming`

**Query Parameters:**
- `days` (int, optional, default: 30) - Number of days ahead to look
- `assetId` (UUID, optional) - Filter by asset
- `assignedTo` (UUID, optional) - Filter by assigned technician

**Response:** `List<MaintenanceScheduleResponse>`

---

## 5. Technician Workload APIs

### 5.1 Get Technician Workload
**GET** `/api/asset-maintenance/technicians/{technicianId}/workload`

**Path Variables:**
- `technicianId` (UUID) - Technician user ID

**Response:** `TechnicianWorkloadDto`

---

### 5.2 Get All Technicians Workload
**GET** `/api/asset-maintenance/technicians/workload`

**Query Parameters:**
- `onlyAvailable` (boolean, optional) - Only technicians with < 8 pending tasks
- `page` (int, optional, default: 0)
- `size` (int, optional, default: 20)

**Response:** `Page<TechnicianWorkloadDto>`

**Permissions:** ADMIN, MANAGER

---

### 5.3 Get Available Technicians for Assignment
**GET** `/api/asset-maintenance/technicians/available`

**Query Parameters:**
- `excludeTechnicianId` (UUID, optional) - Exclude specific technician
- `maxPendingTasks` (int, optional, default: 7) - Maximum pending tasks threshold

**Response:** `List<TechnicianWorkloadDto>`

**Permissions:** ADMIN, MANAGER

**Note:** Use this API before assigning maintenance records to check technician availability.

---

## 6. Maintenance Record Management APIs

### 6.1 Get Maintenance Records
**GET** `/api/asset-maintenance/maintenance/records`

**Query Parameters:**
- `assetId` (UUID, optional) - Filter by asset
- `maintenanceType` (string, optional) - Filter by maintenance type
- `status` (MaintenanceRecordStatus enum, optional) - Filter by status
- `assignedTo` (UUID, optional) - Filter by assigned technician
- `maintenanceDateFrom` (LocalDate, optional) - Filter by maintenance date from
- `maintenanceDateTo` (LocalDate, optional) - Filter by maintenance date to
- `scheduleId` (UUID, optional) - Filter by maintenance schedule
- `page` (int, optional, default: 0)
- `size` (int, optional, default: 20)
- `sort` (string, optional, default: "maintenanceDate,desc")

**Response:** `Page<MaintenanceRecordResponse>`

---

### 6.2 Get Maintenance Record by ID
**GET** `/api/asset-maintenance/maintenance/records/{id}`

**Path Variables:**
- `id` (UUID) - Record ID

**Response:** `MaintenanceRecordResponse`

---

### 6.3 Create Maintenance Record
**POST** `/api/asset-maintenance/maintenance/records`

**Request Body:** `CreateMaintenanceRecordRequest`

**Response:** `MaintenanceRecordResponse`

**Permissions:** ADMIN, MANAGER

---

### 6.4 Update Maintenance Record
**PUT** `/api/asset-maintenance/maintenance/records/{id}`

**Path Variables:**
- `id` (UUID) - Record ID

**Request Body:** `UpdateMaintenanceRecordRequest`

**Response:** `MaintenanceRecordResponse`

**Permissions:** ADMIN, MANAGER (limited fields), TECHNICIAN (their own records)

---

### 6.5 Delete Maintenance Record
**DELETE** `/api/asset-maintenance/maintenance/records/{id}`

**Path Variables:**
- `id` (UUID) - Record ID

**Response:** `void`

**Permissions:** ADMIN, MANAGER

---

### 6.6 Get My Tasks (TECHNICIAN)
**GET** `/api/asset-maintenance/maintenance/records/my-tasks`

**Query Parameters:**
- `status` (MaintenanceRecordStatus enum, optional) - Filter by status (ASSIGNED, IN_PROGRESS, COMPLETED)
- `maintenanceDateFrom` (LocalDate, optional)
- `maintenanceDateTo` (LocalDate, optional)
- `page` (int, optional, default: 0)
- `size` (int, optional, default: 20)
- `sort` (string, optional, default: "maintenanceDate,asc")

**Response:** `Page<MaintenanceRecordResponse>`

**Permissions:** TECHNICIAN (only their own tasks)

---

### 6.7 Start Maintenance (TECHNICIAN)
**PUT** `/api/asset-maintenance/maintenance/records/{id}/start`

**Path Variables:**
- `id` (UUID) - Record ID

**Request Body:** `{ "notes": string (optional) }`

**Response:** `MaintenanceRecordResponse`

**Permissions:** TECHNICIAN (only their assigned tasks)

---

### 6.8 Complete Maintenance (TECHNICIAN)
**PUT** `/api/asset-maintenance/maintenance/records/{id}/complete`

**Path Variables:**
- `id` (UUID) - Record ID

**Request Body:** `CompleteMaintenanceRequest`

**Response:** `MaintenanceRecordResponse`

**Permissions:** TECHNICIAN (only their assigned tasks)

---

### 6.9 Assign Maintenance Task
**PUT** `/api/asset-maintenance/maintenance/records/{id}/assign`

**Path Variables:**
- `id` (UUID) - Record ID

**Request Body:** `AssignMaintenanceRequest`

**Response:** `MaintenanceRecordResponse`

**Permissions:** ADMIN, MANAGER

**Business Rules:**
- Cannot assign if technician has 8 pending tasks (ASSIGNED + IN_PROGRESS)
- Warning shown if technician has 4-7 tasks
- Admin override option available (requires justification)

---

### 6.10 Bulk Assign Maintenance Tasks
**PUT** `/api/asset-maintenance/maintenance/records/assign-bulk`

**Request Body:** `BulkAssignMaintenanceRequest`

**Response:** `List<MaintenanceRecordResponse>`

**Permissions:** ADMIN, MANAGER

**Business Rules:**
- Assignment mode: SINGLE (all to one technician) or DISTRIBUTE (distribute evenly)
- Distribution methods: ROUND_ROBIN, LOAD_BALANCE, MANUAL
- Respects 8 task limit per technician

---

### 6.11 Reassign Maintenance Task
**PUT** `/api/asset-maintenance/maintenance/records/{id}/reassign`

**Path Variables:**
- `id` (UUID) - Record ID

**Request Body:** `AssignMaintenanceRequest`

**Response:** `MaintenanceRecordResponse`

**Permissions:** ADMIN, MANAGER

---

### 6.12 Cancel Maintenance Record
**PUT** `/api/asset-maintenance/maintenance/records/{id}/cancel`

**Path Variables:**
- `id` (UUID) - Record ID

**Request Body:** `{ "reason": string (optional) }`

**Response:** `MaintenanceRecordResponse`

**Permissions:** ADMIN, MANAGER

---

### 6.13 Get Maintenance Records by Asset
**GET** `/api/asset-maintenance/maintenance/records/by-asset/{assetId}`

**Path Variables:**
- `assetId` (UUID) - Asset ID

**Query Parameters:**
- `status` (optional)
- `maintenanceDateFrom` (optional)
- `maintenanceDateTo` (optional)
- `page` (optional)
- `size` (optional)
- `sort` (optional)

**Response:** `Page<MaintenanceRecordResponse>`

---

### 6.14 Get Upcoming Maintenance Records
**GET** `/api/asset-maintenance/maintenance/records/upcoming`

**Query Parameters:**
- `days` (int, optional, default: 30) - Number of days ahead
- `assetId` (UUID, optional)
- `assignedTo` (UUID, optional)
- `status` (optional, default: SCHEDULED, ASSIGNED)

**Response:** `List<MaintenanceRecordResponse>`

---

### 6.15 Create Maintenance Records from Schedule
**POST** `/api/asset-maintenance/maintenance/records/from-schedule/{scheduleId}`

**Path Variables:**
- `scheduleId` (UUID) - Schedule ID

**Request Body:** `{ "maintenanceDate": LocalDate (optional, default: schedule.nextMaintenanceDate) }`

**Response:** `MaintenanceRecordResponse`

**Permissions:** ADMIN, MANAGER (or auto-created by system)

---

### 6.16 Upload Maintenance Completion Images
**POST** `/api/asset-maintenance/maintenance/records/{id}/upload-images`

**Path Variables:**
- `id` (UUID) - Record ID

**Request:** `multipart/form-data` - files array

**Response:** `{ "imageUrls": List<String> }`

**Permissions:** TECHNICIAN (only their assigned tasks)

---

## 7. Reports & Analytics APIs

### 7.1 Get Maintenance History Report
**GET** `/api/asset-maintenance/reports/maintenance-history`

**Query Parameters:**
- `assetId` (UUID, optional)
- `technicianId` (UUID, optional)
- `maintenanceType` (string, optional)
- `status` (MaintenanceRecordStatus, optional)
- `dateFrom` (LocalDate, required)
- `dateTo` (LocalDate, required)
- `format` (string, optional, default: "json", options: "json", "excel", "pdf")

**Response:** 
- JSON: `MaintenanceHistoryReportDto`
- Excel/PDF: File download

**Permissions:** ADMIN, MANAGER

---

### 7.2 Get Asset Status Report
**GET** `/api/asset-maintenance/reports/asset-status`

**Query Parameters:**
- `buildingId` (UUID, optional)
- `assetType` (AssetType, optional)
- `status` (AssetStatus, optional)

**Response:** `AssetStatusReportDto`

**Permissions:** ADMIN, MANAGER, SUPPORTER

---

### 7.3 Get Maintenance Cost Report
**GET** `/api/asset-maintenance/reports/maintenance-cost`

**Query Parameters:**
- `assetId` (UUID, optional)
- `maintenanceType` (string, optional)
- `dateFrom` (LocalDate, required)
- `dateTo` (LocalDate, required)
- `groupBy` (string, optional, default: "month", options: "day", "week", "month", "year", "asset", "technician")

**Response:** `MaintenanceCostReportDto`

**Permissions:** ADMIN, MANAGER

---

### 7.4 Get Upcoming Maintenance Report
**GET** `/api/asset-maintenance/reports/upcoming-maintenance`

**Query Parameters:**
- `days` (int, optional, default: 30)
- `assetId` (UUID, optional)
- `assetType` (AssetType, optional)
- `buildingId` (UUID, optional)

**Response:** `UpcomingMaintenanceReportDto`

**Permissions:** ADMIN, MANAGER, SUPPORTER

---

### 7.5 Get Technician Performance Report
**GET** `/api/asset-maintenance/reports/technician-performance`

**Query Parameters:**
- `technicianId` (UUID, optional)
- `dateFrom` (LocalDate, required)
- `dateTo` (LocalDate, required)

**Response:** `TechnicianPerformanceReportDto`

**Permissions:** ADMIN, MANAGER

---

### 7.6 Get Asset Maintenance Summary
**GET** `/api/asset-maintenance/reports/asset/{assetId}/summary`

**Path Variables:**
- `assetId` (UUID) - Asset ID

**Response:** `AssetMaintenanceSummaryDto`

**Includes:**
- Total maintenance records
- Last maintenance date
- Next scheduled maintenance
- Total maintenance cost
- Maintenance frequency

---

## 8. Utility APIs

### 8.1 Get Asset Types (Enum values)
**GET** `/api/asset-maintenance/utils/asset-types`

**Response:** `List<EnumDto>` - List of AssetType enum values

---

### 8.2 Get Asset Statuses (Enum values)
**GET** `/api/asset-maintenance/utils/asset-statuses`

**Response:** `List<EnumDto>` - List of AssetStatus enum values

---

### 8.3 Get Maintenance Types (Enum values)
**GET** `/api/asset-maintenance/utils/maintenance-types`

**Response:** `List<EnumDto>` - List of MaintenanceType enum values

---

### 8.4 Get Maintenance Record Statuses (Enum values)
**GET** `/api/asset-maintenance/utils/maintenance-record-statuses`

**Response:** `List<EnumDto>` - List of MaintenanceRecordStatus enum values

---

### 8.5 Search Assets by Code/Name
**GET** `/api/asset-maintenance/assets/search`

**Query Parameters:**
- `query` (string, required) - Search query
- `buildingId` (UUID, optional) - Limit search to building
- `limit` (int, optional, default: 10)

**Response:** `List<AssetResponse>`

---

### 8.6 Generate Asset QR Code
**GET** `/api/asset-maintenance/assets/{id}/qr-code`

**Path Variables:**
- `id` (UUID) - Asset ID

**Response:** QR Code image (PNG)

---

### 8.7 Get Asset by QR Code
**GET** `/api/asset-maintenance/assets/by-qr-code/{qrCode}`

**Path Variables:**
- `qrCode` (string) - QR Code value

**Response:** `AssetResponse`

---

## 9. Batch Operations APIs

### 9.1 Bulk Create Assets
**POST** `/api/asset-maintenance/assets/bulk`

**Request Body:** `List<CreateAssetRequest>`

**Response:** `List<AssetResponse>`

**Permissions:** ADMIN, MANAGER

---

### 9.2 Bulk Create Maintenance Schedules
**POST** `/api/asset-maintenance/maintenance/schedules/bulk`

**Request Body:** `List<CreateMaintenanceScheduleRequest>`

**Response:** `List<MaintenanceScheduleResponse>`

**Permissions:** ADMIN, MANAGER

---

### 9.3 Import Maintenance Schedules (Excel/CSV)
**POST** `/api/asset-maintenance/maintenance/schedules/import`

**Request:** `multipart/form-data` - file (Excel/CSV)

**Response:** `{ "successCount": int, "errorCount": int, "errors": List<String> }`

**Permissions:** ADMIN, MANAGER

---

## Summary

### Total API Endpoints by Category:

1. **Asset Management:** 9 endpoints
2. **Supplier Management:** 7 endpoints
3. **Asset-Supplier Relationship:** 2 endpoints
4. **Maintenance Schedule Management:** 8 endpoints
5. **Technician Workload:** 3 endpoints
6. **Maintenance Record Management:** 16 endpoints
7. **Reports & Analytics:** 6 endpoints
8. **Utility:** 7 endpoints
9. **Batch Operations:** 3 endpoints

**Total: 61 API endpoints**

---

## Authentication & Authorization

All endpoints require JWT authentication.

### Role Permissions:
- **ADMIN:** Full access to all endpoints
- **MANAGER:** Full access except some system-level operations
- **TECHNICIAN:** Read access + can update their own assigned tasks
- **SUPPORTER:** Read-only access to assets and reports

---

## Response Format

All responses follow this structure:

**Success Response:**
```json
{
  "data": { ... },
  "message": "Success",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Error Response:**
```json
{
  "error": "Error message",
  "code": "ERROR_CODE",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## Pagination

Endpoints that return lists support pagination:

**Query Parameters:**
- `page` (int, default: 0)
- `size` (int, default: 20)
- `sort` (string, default: varies by endpoint)

**Response:**
```json
{
  "content": [ ... ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "size": 20
}
```

