# 📋 Contract Complete Flow - Từ Đầu Đến Cuối

## 🎯 TỔNG QUAN

Luồng hoàn chỉnh để quản lý hợp đồng (thuê/mua) và upload files từ khi tạo contract đến khi view/download files.

---

## 📊 LUỒNG HOÀN CHỈNH

### 1️⃣ **TẠO CONTRACT**

#### **RENTAL Contract Request:**
```http
POST /api/contracts
Content-Type: application/json

{
  "unitId": "550e8400-e29b-41d4-a716-446655440011",
  "contractNumber": "HD-2024-001",
  "contractType": "RENTAL",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "monthlyRent": 5000000,
  "notes": "Hợp đồng thuê căn hộ (đã thanh toán đầy đủ)"
}
```

#### **PURCHASE Contract Request:**
```http
POST /api/contracts
Content-Type: application/json

{
  "unitId": "550e8400-e29b-41d4-a716-446655440011",
  "contractNumber": "HD-2024-002",
  "contractType": "PURCHASE",
  "startDate": "2024-01-01",
  "purchasePrice": 5000000000,
  "purchaseDate": "2024-01-01",
  "notes": "Hợp đồng mua căn hộ (đã thanh toán đầy đủ)"
}
```

#### **Response (RENTAL):**
```json
{
  "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "unitId": "550e8400-e29b-41d4-a716-446655440011",
  "contractNumber": "HD-2024-001",
  "contractType": "RENTAL",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "monthlyRent": 5000000,
  "notes": "Hợp đồng thuê căn hộ (đã thanh toán đầy đủ)",
  "status": "ACTIVE",
  "createdBy": "...",
  "createdAt": "2024-11-01T15:00:00Z",
  "files": []
}
```

#### **Response (PURCHASE):**
```json
{
  "id": "b2c3d4e5-f6g7-h8i9-j0k1-l2m3n4o5p6q7",
  "unitId": "550e8400-e29b-41d4-a716-446655440011",
  "contractNumber": "HD-2024-002",
  "contractType": "PURCHASE",
  "startDate": "2024-01-01",
  "purchasePrice": 5000000000,
  "purchaseDate": "2024-01-01",
  "notes": "Hợp đồng mua căn hộ (đã thanh toán đầy đủ)",
  "status": "ACTIVE",
  "createdBy": "...",
  "createdAt": "2024-11-01T15:00:00Z",
  "files": []
}
```

#### **Backend Process:**
```
1. ContractController.createContract()
   ↓
2. ContractService.createContract()
   - Validate contract number (unique check)
   - Validate contract type (RENTAL/PURCHASE)
   - For RENTAL:
     * Validate: monthlyRent is REQUIRED
     * Validate: startDate <= endDate (if endDate exists)
   - For PURCHASE:
     * Validate: purchasePrice is REQUIRED
     * Validate: purchaseDate is REQUIRED
     * Validate: endDate cannot exist
     * Validate: paymentMethod and paymentTerms cannot exist (fully paid)
   - Build Contract entity
   ↓
3. ContractRepository.save()
   - Save to database: files.contracts table
   ↓
4. Return ContractDto
```

---

### 2️⃣ **UPLOAD CONTRACT FILE (PDF/Image)**

#### **Request:**
```http
POST /api/contracts/{contractId}/files
Content-Type: multipart/form-data

file: [PDF file hoặc Image]
isPrimary: true (optional, default: false)
uploadedBy: {userId} (optional)
```

#### **Response:**
```json
{
  "id": "f1f2f3f4-f5f6-f7f8-f9f0-f1f2f3f4f5f6",
  "contractId": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "fileName": "a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf",
  "originalFileName": "hop-dong-thue-nha.pdf",
  "fileUrl": "http://localhost:8082/api/contracts/.../a3f5b2c1-...pdf",
  "contentType": "application/pdf",
  "fileSize": 1024000,
  "isPrimary": true,
  "displayOrder": 0,
  "uploadedBy": "...",
  "uploadedAt": "2024-11-01T15:05:00Z"
}
```

#### **Backend Process:**
```
1. ContractController.uploadContractFile()
   ↓
2. ContractService.uploadContractFile()
   - Find contract by ID
   ↓
3. FileStorageService.uploadContractFile()
   - Validate file (size <= 20MB, type: PDF/JPEG/PNG/HEIC)
   - Generate UUID filename: {UUID}.{extension}
   - Save file to disk: ./uploads/contracts/{contractId}/{UUID}.pdf
   - Return FileUploadResponse
   ↓
4. ContractService.uploadContractFile() (continue)
   - Check if first file → set as primary
   - Calculate displayOrder
   - Create ContractFile entity
   ↓
5. ContractFileRepository.save()
   - Save metadata to database: files.contract_files table
   ↓
6. Return ContractFileDto
```

#### **File Storage:**
```
./uploads/
└── contracts/
    └── {contractId}/
        └── {UUID}.pdf
```

**Example:**
```
./uploads/contracts/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6/a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf
```

---

### 3️⃣ **GET CONTRACT VỚI FILES**

#### **Request:**
```http
GET /api/contracts/{contractId}
```

#### **Response:**
```json
{
  "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "unitId": "550e8400-e29b-41d4-a716-446655440011",
  "contractNumber": "HD-2024-001",
  "contractType": "RENTAL",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "monthlyRent": 5000000,
  "files": [
    {
      "id": "f1f2f3f4-f5f6-f7f8-f9f0-f1f2f3f4f5f6",
      "fileName": "a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf",
      "originalFileName": "hop-dong-thue-nha.pdf",
      "fileUrl": "http://localhost:8082/api/contracts/.../a3f5b2c1-...pdf",
      "contentType": "application/pdf",
      "fileSize": 1024000,
      "isPrimary": true,
      "displayOrder": 0
    }
  ]
}
```

#### **Backend Process:**
```
1. ContractController.getContract()
   ↓
2. ContractService.getContractById()
   - Find contract with files (LEFT JOIN FETCH)
   ↓
3. ContractRepository.findByIdWithFiles()
   - Query: SELECT c FROM Contract c LEFT JOIN FETCH c.files WHERE c.id = :id
   ↓
4. Convert to DTO (filter deleted files)
   ↓
5. Return ContractDto
```

---

### 4️⃣ **VIEW CONTRACT FILE (Inline trong Browser)**

#### **Request:**
```http
GET /api/contracts/{contractId}/files/{fileId}/view
```

#### **Response:**
```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: inline; filename="a3f5b2c1-...pdf"

[PDF File Content]
```

#### **Backend Process:**
```
1. ContractController.viewContractFile()
   ↓
2. ContractService.viewContractFile()
   - Find ContractFile by fileId
   - Verify file belongs to contract
   ↓
3. FileStorageService.loadContractFileAsResource()
   - Load file from disk: ./uploads/contracts/{contractId}/{fileName}
   - Security check: validate path
   - Return Resource
   ↓
4. ContractController.viewContractFile() (continue)
   - Detect content type
   - Set Content-Disposition: inline
   - Return ResponseEntity<Resource>
```

---

### 5️⃣ **DOWNLOAD CONTRACT FILE**

#### **Request:**
```http
GET /api/contracts/{contractId}/files/{fileId}/download
```

#### **Response:**
```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="hop-dong-thue-nha.pdf"

[PDF File Content]
```

#### **Backend Process:**
```
1. ContractController.downloadContractFile()
   ↓
2. ContractService.downloadContractFile()
   - Same as viewContractFile()
   ↓
3. ContractController.downloadContractFile() (continue)
   - Set Content-Disposition: attachment (download)
   - Return ResponseEntity<Resource>
```

---

### 6️⃣ **DIRECT FILE ACCESS (Alternative)**

#### **Request:**
```http
GET /api/files/contracts/{contractId}/{fileName}
```

#### **Response:**
```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: inline; filename="{fileName}"

[PDF File Content]
```

#### **Backend Process:**
```
1. FileUploadController.getContractFile()
   ↓
2. FileStorageService.loadContractFileAsResource()
   - Load file directly from disk
   - No database check (faster but less secure)
   ↓
3. Return ResponseEntity<Resource>
```

---

## 🔄 LUỒNG ĐẦY ĐỦ (Từng bước)

### **Step 1: Tạo Contract**
```
User → Frontend
  ↓
POST /api/contracts
  ↓
Backend: Validate → Save to DB → Return ContractDto
  ↓
Frontend: Display contract info
```

### **Step 2: Upload File**
```
User → Frontend: Chọn file
  ↓
Frontend: Validate (client-side)
  ↓
POST /api/contracts/{contractId}/files (multipart/form-data)
  ↓
Backend:
  1. Validate file (size, type)
  2. Save to disk: ./uploads/contracts/{contractId}/{UUID}.pdf
  3. Save metadata to DB: contract_files table
  ↓
Frontend: Display file in list
```

### **Step 3: View File**
```
User → Frontend: Click "View"
  ↓
Frontend: GET /api/contracts/{contractId}/files/{fileId}/view
  ↓
Backend:
  1. Find ContractFile in DB
  2. Load file from disk
  3. Return file content
  ↓
Frontend: Display in <iframe> (PDF) or image viewer
```

---

## 📁 DATABASE STRUCTURE

### **files.contracts Table:**
```sql
- id (UUID, PK)
- unit_id (UUID, NOT NULL)
- contract_number (VARCHAR, UNIQUE)
- contract_type (VARCHAR) -- 'RENTAL' or 'PURCHASE'
- start_date (DATE, NOT NULL)
- end_date (DATE) -- NULL for PURCHASE, optional for RENTAL
- monthly_rent (NUMERIC) -- REQUIRED for RENTAL, NULL for PURCHASE
- purchase_price (NUMERIC) -- REQUIRED for PURCHASE, NULL for RENTAL
- payment_method (VARCHAR) -- NULL (not used, fully paid)
- payment_terms (TEXT) -- NULL (not used, fully paid)
- purchase_date (DATE) -- REQUIRED for PURCHASE, NULL for RENTAL
- status (VARCHAR) -- 'ACTIVE', 'EXPIRED', 'TERMINATED'
- notes (TEXT)
- created_by (UUID, NOT NULL)
- created_at (TIMESTAMPTZ, NOT NULL)
- updated_by (UUID)
- updated_at (TIMESTAMPTZ, NOT NULL)
```

### **files.contract_files Table:**
```sql
- id (UUID, PK)
- contract_id (UUID, FK → contracts.id)
- file_name (VARCHAR) -- UUID filename
- original_file_name (VARCHAR) -- Original filename
- file_path (TEXT) -- Relative path
- file_url (TEXT) -- Full URL
- content_type (VARCHAR) -- MIME type
- file_size (BIGINT)
- is_primary (BOOLEAN) -- Primary file
- display_order (INTEGER)
- uploaded_by, uploaded_at
- is_deleted, deleted_at
```

---

## 🗂️ FILE STORAGE STRUCTURE

```
./uploads/
└── contracts/
    ├── {contractId1}/
    │   ├── {UUID1}.pdf
    │   ├── {UUID2}.jpg
    │   └── {UUID3}.png
    ├── {contractId2}/
    │   └── {UUID1}.pdf
    └── ...
```

---

## 📝 API ENDPOINTS SUMMARY

### **Contract Management:**
- `POST /api/contracts` - Create contract
- `PUT /api/contracts/{contractId}` - Update contract
- `GET /api/contracts/{contractId}` - Get contract by ID
- `GET /api/contracts/unit/{unitId}` - Get contracts by unit
- `GET /api/contracts/active` - Get active contracts
- `GET /api/contracts/unit/{unitId}/active` - Get active contracts by unit
- `DELETE /api/contracts/{contractId}` - Delete contract

### **File Management:**
- `POST /api/contracts/{contractId}/files` - Upload single file
- `POST /api/contracts/{contractId}/files/multiple` - Upload multiple files
- `GET /api/contracts/{contractId}/files` - Get contract files list
- `GET /api/contracts/{contractId}/files/{fileId}/view` - View file inline
- `GET /api/contracts/{contractId}/files/{fileId}/download` - Download file
- `DELETE /api/contracts/{contractId}/files/{fileId}` - Delete file
- `PUT /api/contracts/{contractId}/files/{fileId}/primary` - Set primary file

### **Direct File Access:**
- `GET /api/files/contracts/{contractId}/{fileName}` - Direct file access

---

## 🔒 VALIDATION RULES

### **RENTAL Contracts:**
- ✅ **Must have:** `startDate`, `monthlyRent` (tiền thuê đã thanh toán)
- ✅ **Optional:** `endDate`
- ❌ **Cannot have:** `purchasePrice`, `paymentMethod`, `paymentTerms`, `purchaseDate`
- ✅ **Validation:** If `endDate` exists: `startDate <= endDate`
- 💡 **Note:** Contract chỉ được tạo sau khi đã thanh toán đầy đủ

### **PURCHASE Contracts:**
- ✅ **Must have:** `purchasePrice` (giá mua đã thanh toán), `purchaseDate`, `startDate`
- ❌ **Cannot have:** `monthlyRent`, `endDate`, `paymentMethod`, `paymentTerms` (fully paid)
- 💡 **Note:** Contract chỉ được tạo sau khi đã thanh toán đầy đủ

### **File Upload:**
- ✅ Max size: 20MB
- ✅ Allowed types: PDF, JPEG, PNG, HEIC, HEIF
- ✅ File name: Auto-generate UUID

---

## 🎯 USE CASES

### **Use Case 1: Tạo hợp đồng thuê + upload file**
```
1. POST /api/contracts
   {
     "contractType": "RENTAL",
     "monthlyRent": 5000000,  // Required - đã thanh toán
     "startDate": "2024-01-01",
     "endDate": "2024-12-31"
   }
2. POST /api/contracts/{contractId}/files (upload PDF)
3. GET /api/contracts/{contractId} (view contract + files)
4. GET /api/contracts/{contractId}/files/{fileId}/view (view PDF)
```

### **Use Case 2: Tạo hợp đồng mua + upload files**
```
1. POST /api/contracts
   {
     "contractType": "PURCHASE",
     "purchasePrice": 5000000000,  // Required - đã thanh toán
     "purchaseDate": "2024-01-01"  // Required
   }
2. POST /api/contracts/{contractId}/files/multiple (upload multiple files)
3. GET /api/contracts/{contractId}/files (list all files)
4. PUT /api/contracts/{contractId}/files/{fileId}/primary (set primary)
```

---

## 💡 QUAN TRỌNG

### **Thanh toán trước khi tạo Contract:**
- Tất cả contracts (RENTAL và PURCHASE) **phải đã thanh toán đầy đủ** trước khi tạo contract
- RENTAL: `monthlyRent` là số tiền đã thanh toán (required)
- PURCHASE: `purchasePrice` là số tiền đã thanh toán (required)
- Không có trường hợp "chưa thanh toán" hoặc "thanh toán dần"

### **Fields đã loại bỏ:**
- ❌ `deposit` - Không còn dùng
- ❌ `paymentMethod` và `paymentTerms` cho PURCHASE - Không cần vì đã thanh toán đầy đủ

---

## ✅ HOÀN CHỈNH!

Luồng đã sẵn sàng từ tạo contract (đã thanh toán) → upload files → view/download files! 🚀

