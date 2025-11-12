# 📁 File Storage Location - Nơi Lưu File

## 🎯 NƠI LƯU FILE

### **Default Location:**
```
./uploads/
```

**Full path:** Relative path từ **thư mục làm việc** (working directory) của application.

---

## 📂 CẤU TRÚC THƯ MỤC

### **Cho Contract Files:**
```
./uploads/
└── contracts/
    └── {contractId}/
        ├── {UUID1}.pdf
        ├── {UUID2}.jpg
        └── {UUID3}.png
```

### **Cho News Images:**
```
./uploads/
└── news/
    └── {ownerId}/
        └── {date}/
            └── {UUID}.{ext}
```

### **Cho Profile Images:**
```
./uploads/
└── profile/
    └── {ownerId}/
        └── {date}/
            └── {UUID}.{ext}
```

---

## ⚙️ CẤU HÌNH

### **application.properties:**
```properties
file.storage.location=${FILE_STORAGE_LOCATION:./uploads}
```

- **Default:** `./uploads` (relative path)
- **Có thể thay đổi:** Set environment variable `FILE_STORAGE_LOCATION`

---

## 🔧 CÁCH THAY ĐỔI NƠI LƯU FILE

### **Option 1: Environment Variable (Recommended)**
```bash
# Windows
set FILE_STORAGE_LOCATION=D:\Capstone\QhomeBase\QhomeBase\data-docs-service\uploads

# Linux/Mac
export FILE_STORAGE_LOCATION=/var/www/uploads
```

### **Option 2: application.properties**
```properties
file.storage.location=D:\\Capstone\\QhomeBase\\QhomeBase\\data-docs-service\\uploads
```

### **Option 3: Absolute Path trong Code**
```java
// FileStorageService.java
this.fileStorageLocation = Paths.get("D:\\Capstone\\QhomeBase\\QhomeBase\\data-docs-service\\uploads")
        .toAbsolutePath().normalize();
```

---

## 📍 VÍ DỤ

### **Windows:**
```
Working Directory: D:\Capstone\QhomeBase\QhomeBase\data-docs-service

Default Location: ./uploads
Full Path: D:\Capstone\QhomeBase\QhomeBase\data-docs-service\uploads

Contract File Example:
D:\Capstone\QhomeBase\QhomeBase\data-docs-service\uploads\contracts\{contractId}\{UUID}.pdf
```

### **Linux/Mac:**
```
Working Directory: /home/user/qhome-base/data-docs-service

Default Location: ./uploads
Full Path: /home/user/qhome-base/data-docs-service/uploads

Contract File Example:
/home/user/qhome-base/data-docs-service/uploads/contracts/{contractId}/{UUID}.pdf
```

---

## 🔍 LÀM THẾ NÀO ĐỂ BIẾT FILE LƯU Ở ĐÂU?

### **1. Check Log khi Application Start:**
```
File storage location initialized at: D:\Capstone\QhomeBase\QhomeBase\data-docs-service\uploads
```

### **2. Check Code:**
```java
// FileStorageService.java
@PostConstruct
public void init() {
    Files.createDirectories(this.fileStorageLocation);
    log.info("File storage location initialized at: {}", this.fileStorageLocation);
}
```

### **3. Check Environment Variable:**
```bash
# Windows
echo %FILE_STORAGE_LOCATION%

# Linux/Mac
echo $FILE_STORAGE_LOCATION
```

---

## 📋 CODE INITIALIZATION

### **FileStorageService Constructor:**
```java
@Autowired
public FileStorageService(FileStorageProperties fileStorageProperties) {
    this.fileStorageProperties = fileStorageProperties;
    this.fileStorageLocation = Paths.get(fileStorageProperties.getLocation())
            .toAbsolutePath().normalize();
}
```

**Giải thích:**
1. Lấy location từ `fileStorageProperties.getLocation()` → `./uploads`
2. `Paths.get()` → Convert string thành Path
3. `.toAbsolutePath()` → Convert relative path thành absolute path
4. `.normalize()` → Clean path (remove `.`, `..`, etc.)

**Kết quả:**
- Input: `./uploads`
- Output: `D:\Capstone\QhomeBase\QhomeBase\data-docs-service\uploads` (absolute path)

---

## 🗂️ LƯU FILE CONTRACT

### **Path Building:**
```java
Path targetLocation = this.fileStorageLocation
        .resolve("contracts")                    // ./uploads/contracts
        .resolve(contractId.toString())         // ./uploads/contracts/{contractId}
        .resolve(fileName);                      // ./uploads/contracts/{contractId}/{UUID}.pdf
```

### **Example:**
```
Base Location: D:\Capstone\QhomeBase\QhomeBase\data-docs-service\uploads
Contract ID: a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6
File Name: a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf

Full Path:
D:\Capstone\QhomeBase\QhomeBase\data-docs-service\uploads\contracts\a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6\a3f5b2c1-d4e5-f6g7-h8i9-j0k1l2m3n4o5.pdf
```

---

## ⚠️ LƯU Ý

### **1. Relative Path:**
- `./uploads` là relative path từ **working directory**
- Working directory là nơi bạn chạy `java -jar app.jar` hoặc từ IDE
- Có thể thay đổi khi deploy

### **2. Absolute Path:**
- Dùng absolute path nếu muốn cố định location
- Example: `D:\Capstone\QhomeBase\QhomeBase\data-docs-service\uploads`

### **3. Production:**
- Nên dùng environment variable để dễ config
- Nên lưu ở thư mục ngoài application (dễ backup, không bị xóa khi redeploy)

### **4. Permissions:**
- Đảm bảo application có quyền **write** vào thư mục
- Nếu không có quyền → lỗi khi khởi động

---

## 🚀 DEPLOYMENT

### **Production Setup:**
```bash
# 1. Tạo thư mục uploads
mkdir -p /var/www/qhome-base/uploads

# 2. Set permissions
chmod 755 /var/www/qhome-base/uploads
chown app-user:app-user /var/www/qhome-base/uploads

# 3. Set environment variable
export FILE_STORAGE_LOCATION=/var/www/qhome-base/uploads

# 4. Run application
java -jar data-docs-service.jar
```

---

## ✅ TÓM TẮT

### **Nơi lưu file:**
- **Default:** `./uploads` (relative path từ working directory)
- **Có thể thay đổi:** Set `FILE_STORAGE_LOCATION` environment variable
- **Full path:** Xem log khi application start

### **Cấu trúc:**
```
./uploads/
├── contracts/
│   └── {contractId}/
│       └── {UUID}.{ext}
├── news/
│   └── {ownerId}/{date}/
│       └── {UUID}.{ext}
└── profile/
    └── {ownerId}/{date}/
        └── {UUID}.{ext}
```

### **Tìm file:**
- Xem log: "File storage location initialized at: ..."
- Hoặc check environment variable `FILE_STORAGE_LOCATION`

