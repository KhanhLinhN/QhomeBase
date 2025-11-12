# 📁 File Storage - Cách Lưu File Chi Tiết

## 🎯 TỔNG QUAN

Tài liệu này mô tả chi tiết cách lưu file contract (PDF, images) từ khi upload đến khi lưu vào database và disk.

---

## 🔄 LUỒNG LƯU FILE

### **Step 1: User Upload File**
```
User → Frontend: Chọn file PDF hoặc Image
  ↓
Frontend: Validate file (size, type) - Client side
  ↓
POST /api/contracts/{contractId}/files
Content-Type: multipart/form-data
{
  file: [MultipartFile],
  isPrimary: true (optional),
  uploadedBy: UUID (optional)
}
```

---

### **Step 2: Controller Receive Request**
```java
ContractController.uploadContractFile()
  ↓
POST /api/contracts/{contractId}/files
  - contractId: UUID của contract
  - file: MultipartFile (PDF/Image)
  - isPrimary: Boolean (optional)
  - uploadedBy: UUID (optional)
```

---

### **Step 3: ContractService Process**
```java
ContractService.uploadContractFile()
  1. Find contract by ID → throw error if not found
  2. Call FileStorageService.uploadContractFile()
     - Pass: file, contractId, uploadedBy
  3. Get FileUploadResponse from FileStorageService
  4. Handle primary file logic:
     - If isPrimary = true → set existing primary to false
     - If no files exist → auto-set as primary
  5. Calculate displayOrder (số files hiện tại)
  6. Create ContractFile entity:
     - fileName: UUID filename (from FileStorageService)
     - originalFileName: Tên file gốc
     - filePath: "contracts/{contractId}/{fileName}"
     - fileUrl: Full URL (from FileStorageService)
     - contentType, fileSize, isPrimary, displayOrder
  7. Save ContractFile to database
  8. Return ContractFileDto
```

---

### **Step 4: FileStorageService Save to Disk**
```java
FileStorageService.uploadContractFile()
  1. Validate file:
     - Size: <= 20MB
     - Type: PDF, JPEG, PNG, HEIC, HEIF
  2. Clean original filename:
     - StringUtils.cleanPath(file.getOriginalFilename())
     - Remove "..", "/", "\" for security
  3. Extract file extension:
     - getFileExtension(originalFileName)
  4. Generate UUID filename:
     - UUID.randomUUID().toString() + "." + extension
     - Example: "a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf"
  5. Build file path:
     - Base path: ./uploads (from application.properties)
     - Full path: ./uploads/contracts/{contractId}/{UUID}.{ext}
     - Example: ./uploads/contracts/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6/a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf
  6. Create directories if not exist:
     - Files.createDirectories(targetLocation.getParent())
  7. Save file to disk:
     - Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING)
  8. Build file URL:
     - Base URL: http://localhost:8082/api (from application.properties)
     - Full URL: http://localhost:8082/api/contracts/{contractId}/{fileName}
  9. Return FileUploadResponse:
     - id: UUID (random, not used in database)
     - fileName: UUID filename
     - originalFileName: Tên file gốc
     - fileUrl: Full URL
     - contentType: MIME type
     - fileSize: Size in bytes
     - uploadedBy: UUID
```

---

## 📂 CẤU TRÚC THƯ MỤC

### **Disk Structure:**
```
./uploads/                          ← Base location (configurable)
└── contracts/
    └── {contractId}/               ← Folder per contract
        ├── {UUID1}.pdf              ← File 1
        ├── {UUID2}.jpg              ← File 2
        └── {UUID3}.png              ← File 3
```

### **Example:**
```
./uploads/
└── contracts/
    └── a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6/
        ├── a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf
        ├── b4g6c3d2-e5f6-g7h8-i9j0-k1l2m3n4o5p6.jpg
        └── c5h7d4e3-f6g7-h8i9-j0k1-l2m3n4o5p6q7.png
```

---

## 🗄️ DATABASE STRUCTURE

### **files.contract_files Table:**
```sql
CREATE TABLE files.contract_files (
    id UUID PRIMARY KEY,
    contract_id UUID NOT NULL,          -- FK → contracts.id
    file_name VARCHAR(255) NOT NULL,    -- UUID filename: "a3f5b2c1-...pdf"
    original_file_name VARCHAR(500),    -- Original: "hop-dong-thue-nha.pdf"
    file_path TEXT NOT NULL,            -- Relative: "contracts/{contractId}/{fileName}"
    file_url TEXT NOT NULL,             -- Full URL: "http://localhost:8082/api/contracts/{contractId}/{fileName}"
    content_type VARCHAR(100),           -- MIME: "application/pdf"
    file_size BIGINT,                   -- Size in bytes
    is_primary BOOLEAN,                 -- Primary file flag
    display_order INTEGER,              -- Display order
    uploaded_by UUID,                   -- User ID
    uploaded_at TIMESTAMPTZ,           -- Upload timestamp
    is_deleted BOOLEAN,                 -- Soft delete flag
    deleted_at TIMESTAMPTZ             -- Delete timestamp
);
```

### **Example Data:**
```sql
INSERT INTO files.contract_files VALUES (
    'f1f2f3f4-f5f6-f7f8-f9f0-f1f2f3f4f5f6',  -- id
    'a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6',  -- contract_id
    'a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf',  -- file_name (UUID)
    'hop-dong-thue-nha.pdf',              -- original_file_name
    'contracts/a1b2c3d4-.../a3f5b2c1-...pdf',  -- file_path (relative)
    'http://localhost:8082/api/contracts/a1b2c3d4-.../a3f5b2c1-...pdf',  -- file_url
    'application/pdf',                    -- content_type
    1024000,                              -- file_size (bytes)
    true,                                 -- is_primary
    0,                                    -- display_order
    'u1u2u3u4-u5u6-u7u8-u9u0-u1u2u3u4u5u6',  -- uploaded_by
    '2024-11-01 15:05:00+07',            -- uploaded_at
    false,                                -- is_deleted
    NULL                                  -- deleted_at
);
```

---

## 📋 CHI TIẾT CODE

### **1. FileStorageService.uploadContractFile()**

```java
public FileUploadResponse uploadContractFile(
        MultipartFile file,
        UUID contractId,
        UUID uploadedBy) {
    
    // 1. Validate file
    validateContractFile(file);  // Size <= 20MB, Type: PDF/JPEG/PNG/HEIC
    
    // 2. Clean original filename
    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
    
    // 3. Extract extension
    String fileExtension = getFileExtension(originalFileName);  // ".pdf"
    
    // 4. Generate UUID filename
    String fileName = UUID.randomUUID().toString() + "." + fileExtension;
    // Example: "a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf"
    
    // 5. Build file path
    Path targetLocation = this.fileStorageLocation
            .resolve("contracts")                    // ./uploads/contracts
            .resolve(contractId.toString())         // ./uploads/contracts/{contractId}
            .resolve(fileName);                      // ./uploads/contracts/{contractId}/{UUID}.pdf
    
    try {
        // 6. Create directories if not exist
        Files.createDirectories(targetLocation.getParent());
        
        // 7. Save file to disk
        Files.copy(
            file.getInputStream(), 
            targetLocation, 
            StandardCopyOption.REPLACE_EXISTING
        );
        
        // 8. Build file URL
        String fileUrl = String.format(
            "%s/contracts/%s/%s",
            fileStorageProperties.getBaseUrl(),  // http://localhost:8082/api
            contractId,
            fileName
        );
        // Result: "http://localhost:8082/api/contracts/{contractId}/{fileName}"
        
        // 9. Return response
        return FileUploadResponse.success(
            UUID.randomUUID(),           // id (not used)
            fileName,                    // UUID filename
            originalFileName,            // Original filename
            fileUrl,                     // Full URL
            file.getContentType(),       // MIME type
            file.getSize(),              // Size in bytes
            uploadedBy                   // User ID
        );
        
    } catch (IOException ex) {
        throw new FileStorageException("Could not store file " + fileName, ex);
    }
}
```

---

### **2. ContractService.uploadContractFile()**

```java
@Transactional
public ContractFileDto uploadContractFile(
        UUID contractId, 
        MultipartFile file, 
        UUID uploadedBy, 
        Boolean isPrimary) {
    
    // 1. Find contract
    Contract contract = contractRepository.findByIdWithFiles(contractId)
            .orElseThrow(() -> new IllegalArgumentException("Contract not found"));
    
    // 2. Upload file to disk (FileStorageService)
    FileUploadResponse uploadResponse = fileStorageService.uploadContractFile(
        file, contractId, uploadedBy
    );
    
    // 3. Handle primary file
    if (Boolean.TRUE.equals(isPrimary)) {
        // Set existing primary to false
        contractFileRepository.findPrimaryFileByContractId(contractId)
                .ifPresent(primaryFile -> {
                    primaryFile.setIsPrimary(false);
                    contractFileRepository.save(primaryFile);
                });
    } else {
        // Auto-set as primary if no files exist
        List<ContractFile> existingFiles = contractFileRepository.findByContractId(contractId);
        if (existingFiles.isEmpty()) {
            isPrimary = true;
        }
    }
    
    // 4. Calculate display order
    Integer displayOrder = contractFileRepository.findByContractId(contractId).size();
    
    // 5. Create ContractFile entity
    ContractFile contractFile = ContractFile.builder()
            .contract(contract)
            .fileName(uploadResponse.getFileName())              // UUID filename
            .originalFileName(uploadResponse.getOriginalFileName())  // Original name
            .filePath("contracts/" + contractId + "/" + uploadResponse.getFileName())  // Relative path
            .fileUrl(uploadResponse.getFileUrl())                 // Full URL
            .contentType(uploadResponse.getContentType())         // MIME type
            .fileSize(uploadResponse.getFileSize())               // Size
            .isPrimary(Boolean.TRUE.equals(isPrimary))           // Primary flag
            .displayOrder(displayOrder)                           // Display order
            .uploadedBy(uploadedBy)                               // User ID
            .build();
    
    // 6. Save to database
    contractFile = contractFileRepository.save(contractFile);
    
    // 7. Return DTO
    return toFileDto(contractFile);
}
```

---

## ⚙️ CONFIGURATION

### **application.properties:**
```properties
# File Storage Location
file.storage.location=${FILE_STORAGE_LOCATION:./uploads}

# File Storage Base URL
file.storage.base-url=${FILE_STORAGE_BASE_URL:http://localhost:8082/api}

# File Upload Size
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB
```

---

## 🔍 VALIDATION

### **File Validation:**
```java
private void validateContractFile(MultipartFile file) {
    // 1. Check file size
    if (file.getSize() > MAX_CONTRACT_FILE_SIZE) {  // 20MB
        throw new FileStorageException("File size exceeds 20MB limit");
    }
    
    // 2. Check content type
    String contentType = file.getContentType();
    if (!ALLOWED_CONTRACT_TYPES.contains(contentType)) {
        throw new FileStorageException("File type not allowed. Allowed: PDF, JPEG, PNG, HEIC");
    }
    
    // 3. Check if file is empty
    if (file.isEmpty()) {
        throw new FileStorageException("File is empty");
    }
}
```

### **Allowed Types:**
- `application/pdf` - PDF files
- `image/jpeg` - JPEG images
- `image/png` - PNG images
- `image/heic` - HEIC images
- `image/heif` - HEIF images

---

## 🔐 SECURITY

### **Filename Sanitization:**
```java
StringUtils.cleanPath(filename)
```
- Removes: `..`, `/`, `\` (path traversal attacks)
- Normalizes: Path separators
- Example:
  - Input: `../../etc/passwd.pdf`
  - Output: `etc/passwd.pdf`

### **Path Validation:**
```java
Path targetLocation = fileStorageLocation.resolve("contracts")
    .resolve(contractId.toString())
    .resolve(fileName);
    
// Security check: Ensure path is within allowed directory
Path normalizedPath = targetLocation.normalize();
if (!normalizedPath.startsWith(fileStorageLocation.normalize())) {
    throw new FileStorageException("Invalid file path");
}
```

---

## 📊 SUMMARY

### **Luồng hoàn chỉnh:**
```
1. User upload file → POST /api/contracts/{contractId}/files
   ↓
2. ContractController → ContractService.uploadContractFile()
   ↓
3. FileStorageService.uploadContractFile():
   - Validate file (size, type)
   - Generate UUID filename
   - Save to disk: ./uploads/contracts/{contractId}/{UUID}.{ext}
   - Return FileUploadResponse
   ↓
4. ContractService.uploadContractFile() (continue):
   - Handle primary file logic
   - Create ContractFile entity
   - Save to database: files.contract_files table
   ↓
5. Return ContractFileDto
```

### **Điểm quan trọng:**
- ✅ File được lưu với UUID filename (không dùng tên gốc để tránh conflicts)
- ✅ File được tổ chức theo contractId (mỗi contract có folder riêng)
- ✅ Metadata được lưu trong database (file_contracts table)
- ✅ File path và URL được tách riêng (relative path và full URL)
- ✅ Primary file được tự động set nếu là file đầu tiên
- ✅ Display order được tự động tính (số files hiện tại)

---

## ✅ HOÀN CHỈNH!

Cách lưu file đã được mô tả chi tiết! 🚀

