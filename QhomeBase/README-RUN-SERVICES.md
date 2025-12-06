# Hướng dẫn chạy tất cả services

## Cách 1: Chạy bằng PowerShell (KHUYÊN DÙNG)

### Từ bất kỳ đâu, chạy:
```powershell
cd D:\Capstone\QhomeBase\QhomeBase
.\run-all-services.ps1
```

**Lưu ý:** Trong PowerShell, không dùng `/d` như CMD. Chỉ cần:
```powershell
cd D:\Capstone\QhomeBase\QhomeBase
```

### Hoặc chạy trực tiếp với đường dẫn đầy đủ:
```powershell
& "D:\Capstone\QhomeBase\QhomeBase\run-all-services.ps1"
```

## Cách 2: Chạy bằng CMD/Batch

### Từ bất kỳ đâu, chạy:
```cmd
cd /d D:\Capstone\QhomeBase\QhomeBase
run-all-services.bat
```

### Hoặc double-click file `run-all-services.bat` trong Windows Explorer

## Cách 3: Chạy trực tiếp từ Windows Explorer

1. Mở Windows Explorer
2. Navigate đến `D:\Capstone\QhomeBase\QhomeBase`
3. Double-click file `run-all-services.bat` hoặc `run-all-services.ps1`

## Services sẽ được chạy:

1. **IAM Service** (port 8088) - Magenta
2. **Base Service** (port 8081) - Blue
3. **Customer Interaction Service** (port 8086) - DarkCyan
4. **Data Docs Service** (port 8082) - Cyan
5. **Services Card Service** (port 8083) - Green
6. **Asset Maintenance Service** (port 8084) - Yellow
7. **Finance Billing Service** (port 8085) - Red
8. **API Gateway** (port 8989) - DarkMagenta

## Service URLs:

- API Gateway: http://localhost:8989
- IAM Service: http://localhost:8088
- Base Service: http://localhost:8081
- Customer Interaction: http://localhost:8086
- Data Docs: http://localhost:8082
- Services Card: http://localhost:8083
- Asset Maintenance: http://localhost:8084
- Finance Billing: http://localhost:8085

## Để dừng services:

- Đóng các cửa sổ PowerShell/CMD đang chạy service
- Hoặc nhấn `Ctrl+C` trong từng cửa sổ

