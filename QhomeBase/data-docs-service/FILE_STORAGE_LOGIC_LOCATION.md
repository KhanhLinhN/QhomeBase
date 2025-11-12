# 📂 Logic Lưu File - Vị Trí Code

## 🎯 CÁC FILE CHỨA LOGIC LƯU FILE

### **1. Controller - Nhận Request Upload**
📁 **File:** `ContractController.java`  
📍 **Path:** `data-docs-service/src/main/java/com/QhomeBase/datadocsservice/controller/ContractController.java`

```java
@PostMapping("/{contractId}/files")
public ResponseEntity<ContractFileDto> uploadContractFile(
        @PathVariable UUID contractId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary,
        @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy) {
    
    ContractFileDto fileDto = contractService.uploadContractFile(contractId, file, uploadedBy, isPrimary);
    return ResponseEntity.status(HttpStatus.CREATED).body(fileDto);
}
```

**Chức năng:**
- Nhận HTTP request upload file
- Extract parameters (contractId, file, isPrimary, uploadedBy)
- Gọi `ContractService.uploadContractFile()`
- Return response

---

### **2. Service - Business Logic**
📁 **File:** `ContractService.java`  
📍 **Path:** `data-docs-service/src/main/java/com/QhomeBase/datadocsservice/service/ContractService.java`

**Method:** `uploadContractFile()` - Line 232

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
    
    // 3. Handle primary file logic
    // 4. Create ContractFile entity
    // 5. Save to database
    // 6. Return DTO
}
```

**Chức năng:**
- Validate contract tồn tại
- Gọi `FileStorageService` để lưu file vào disk
- Xử lý primary file logic
- Lưu metadata vào database
- Return DTO

---

### **3. File Storage Service - Lưu File Vào Disk**
📁 **File:** `FileStorageService.java`  
📍 **Path:** `data-docs-service/src/main/java/com/QhomeBase/datadocsservice/service/FileStorageService.java`

**Method:** `uploadContractFile()` - Line 185

```java
public FileUploadResponse uploadContractFile(
        MultipartFile file,
        UUID contractId,
        UUID uploadedBy) {
    
    // 1. Validate file
    validateContractFile(file);
    
    // 2. Generate UUID filename
    String fileName = UUID.randomUUID().toString() + "." + fileExtension;
    
    // 3. Build file path
    Path targetLocation = this.fileStorageLocation
            .resolve("contracts")
            .resolve(contractId.toString())
            .resolve(fileName);
    
    // 4. Create directories if not exist
    Files.createDirectories(targetLocation.getParent());
    
    // 5. Save file to disk
    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
    
    // 6. Build file URL
    String fileUrl = String.format("%s/contracts/%s/%s", ...);
    
    // 7. Return response
    return FileUploadResponse.success(...);
}
```

**Chức năng:**
- Validate file (size, type)
- Generate UUID filename
- Tạo path: `./uploads/contracts/{contractId}/{UUID}.{ext}`
- Tạo thư mục nếu chưa có
- **Lưu file vào disk** ← **Logic chính ở đây**
- Build file URL
- Return response

---

## 🔄 LUỒNG HOÀN CHỈNH

```
1. ContractController.uploadContractFile()          ← Nhận request
   ↓
2. ContractService.uploadContractFile()            ← Business logic
   ↓
3. FileStorageService.uploadContractFile()          ← Lưu file vào disk
   ↓
   Files.copy() → Lưu vào: ./uploads/contracts/{contractId}/{UUID}.{ext}
   ↓
4. ContractService.uploadContractFile() (continue)  ← Lưu metadata vào DB
   ↓
   ContractFileRepository.save() → Lưu vào: files.contract_files
   ↓
5. Return ContractFileDto
```

---

## 📋 CÁC METHOD LIÊN QUAN

### **FileStorageService.java:**

1. **`uploadContractFile()`** - Line 185
   - Lưu file contract vào disk
   - Method chính để lưu file

2. **`validateContractFile()`** - Line 267
   - Validate file size (<= 20MB)
   - Validate file type (PDF, JPEG, PNG, HEIC, HEIF)

3. **`loadContractFileAsResource()`** - Line 230
   - Load file từ disk để view/download

4. **`init()`** - Line 61 (@PostConstruct)
   - Khởi tạo thư mục uploads khi application start

### **ContractService.java:**

1. **`uploadContractFile()`** - Line 232
   - Xử lý business logic
   - Lưu metadata vào database

2. **`viewContractFile()`** - Line 265
   - Load file để view

3. **`downloadContractFile()`** - Line 278
   - Load file để download

### **ContractController.java:**

1. **`uploadContractFile()`** - Line 96
   - Endpoint: `POST /api/contracts/{contractId}/files`

2. **`uploadContractFiles()`** - Line 112
   - Endpoint: `POST /api/contracts/{contractId}/files/multiple`

3. **`viewContractFile()`** - Line 141
   - Endpoint: `GET /api/contracts/{contractId}/files/{fileId}/view`

4. **`downloadContractFile()`** - Line 167
   - Endpoint: `GET /api/contracts/{contractId}/files/{fileId}/download`

---

## 🎯 LOGIC CHÍNH LƯU FILE

### **File:** `FileStorageService.java`
### **Method:** `uploadContractFile()` - Line 185-228
### **Code chính:**

```java
// Build path
Path targetLocation = this.fileStorageLocation
        .resolve("contracts")
        .resolve(contractId.toString())
        .resolve(fileName);

// Create directories
Files.createDirectories(targetLocation.getParent());

// Lưu file vào disk ← ĐÂY LÀ LOGIC CHÍNH
Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
```

---

## 📁 CẤU TRÚC FILE

```
data-docs-service/
└── src/
    └── main/
        └── java/
            └── com/
                └── QhomeBase/
                    └── datadocsservice/
                        ├── controller/
                        │   └── ContractController.java        ← Nhận request
                        │
                        ├── service/
                        │   ├── ContractService.java          ← Business logic
                        │   └── FileStorageService.java       ← Lưu file vào disk (CHÍNH)
                        │
                        ├── repository/
                        │   ├── ContractRepository.java
                        │   └── ContractFileRepository.java
                        │
                        ├── model/
                        │   ├── Contract.java
                        │   └── ContractFile.java
                        │
                        └── dto/
                            ├── ContractDto.java
                            └── ContractFileDto.java
```

---

## ✅ TÓM TẮT

### **Logic lưu file nằm ở:**

1. **ContractController.java** (Line 96)
   - Nhận HTTP request upload file

2. **ContractService.java** (Line 232)
   - Xử lý business logic
   - Gọi FileStorageService để lưu file

3. **FileStorageService.java** (Line 185) ← **CHÍNH**
   - **Logic lưu file vào disk**
   - Method: `uploadContractFile()`
   - Code lưu: `Files.copy()` - Line 205

### **File quan trọng nhất:**
📁 `FileStorageService.java` - Method `uploadContractFile()` - Line 185-228

---

## 🔍 TÌM CODE LƯU FILE

### **Tìm method lưu file:**
```java
// FileStorageService.java
Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
```

### **Path đầy đủ:**
```
data-docs-service/src/main/java/com/QhomeBase/datadocsservice/service/FileStorageService.java
Method: uploadContractFile()
Line: 205
```


