# Kế hoạch Refactor Notification System

## Phương án 1: Tách bảng (Recommended - giống News pattern)

### Cấu trúc:
```
Notification (1) ──< NotificationTarget (N)
Notification (1) ──< NotificationRead (N)
```

### Ưu điểm:
✅ Track được ai đã đọc
✅ Có thể gửi cho nhiều target
✅ Scalable, dễ mở rộng
✅ Consistent với News pattern

### Nhược điểm:
❌ Phức tạp hơn
❌ Cần query join nhiều bảng

---

## Phương án 2: Giữ nguyên + NotificationRead

### Cấu trúc:
```
Notification (1) ──< NotificationRead (N)
```

### Thay đổi:
- Xóa `status`, `read_at` khỏi Notification
- Thêm NotificationRead table để track

### Ưu điểm:
✅ Đơn giản hơn
✅ Track được ai đã đọc
✅ Vẫn có thể gửi cho ALL

### Nhược điểm:
❌ Vẫn không thể gửi cho building/unit
❌ Mỗi notification chỉ có 1 recipient

---

## Phương án 3: Giữ nguyên + thêm NotificationRecipient

### Cấu trúc:
```
Notification (1) ──< NotificationRecipient (N)
NotificationRecipient (1) ──< NotificationRead (N)
```

### Ưu điểm:
✅ Linh hoạt nhất
✅ Có thể gửi cho nhiều người
✅ Track được ai đã đọc

### Nhược điểm:
❌ Phức tạp nhất
❌ Cần tạo nhiều NotificationRecipient khi gửi ALL

---

## Recommendation: Phương án 1 (Tách bảng)

Lý do:
1. Giống News pattern - dễ maintain
2. Có thể gửi cho building/unit cụ thể
3. Track được ai đã đọc
4. Scalable cho tương lai

