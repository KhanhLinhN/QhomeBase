# Test API để kiểm tra inspectionDate

## Backend đã sẵn sàng ✅

Field `inspectionDate` đã được thêm vào `ContractDto` và sẽ được trả về trong tất cả các API endpoints.

## Cách test API:

### 1. Test với Postman hoặc curl:

```bash
# Lấy contract by ID
GET http://localhost:8080/api/contracts/{contractId}
Authorization: Bearer {token}

# Response sẽ có:
{
  "id": "...",
  "contractNumber": "...",
  "inspectionDate": "2025-01-XX",  // ← Field này
  "startDate": "...",
  "endDate": "...",
  ...
}
```

### 2. Kiểm tra trong Browser DevTools:

1. Mở trang rental-review: `http://localhost:3000/base/contract/rental-review`
2. Mở DevTools (F12) → Network tab
3. Tìm request đến `/api/contracts/{contractId}`
4. Xem Response → kiểm tra có field `inspectionDate` không

### 3. Kiểm tra Frontend Code:

Tìm file frontend (có thể ở repository khác):
- `src/app/base/contract/rental-review/page.tsx`
- Hoặc tương tự

Kiểm tra xem code có dùng:
- ✅ `contract.inspectionDate` (đúng)
- ❌ `contract.inspect_date` (sai - thiếu "ion")
- ❌ `contract.inspection_date` (sai - snake_case, nên dùng camelCase)

### 4. Field name trong API:

- **Backend (Java)**: `inspectionDate` (camelCase)
- **API Response (JSON)**: `inspectionDate` (camelCase)
- **Database**: `inspection_date` (snake_case)

### 5. Nếu inspectionDate là null:

- Contract chưa có AssetInspection → `inspectionDate` sẽ là `null`
- Base-service không available → `inspectionDate` sẽ là `null` (không throw error)
- Đây là behavior bình thường

## Checklist để verify:

- [ ] API response có field `inspectionDate`
- [ ] Frontend code dùng `contract.inspectionDate` (không phải `inspect_date`)
- [ ] Frontend hiển thị ngày kiểm tra đúng
- [ ] Xử lý trường hợp `inspectionDate` là `null`


