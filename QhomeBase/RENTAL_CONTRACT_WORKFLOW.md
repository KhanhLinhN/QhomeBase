# 📋 Luồng Hợp Đồng Cho Thuê (Rental Contract Workflow)

## 🎯 TỔNG QUAN

Luồng hoàn chỉnh từ khi căn hộ sẵn sàng cho thuê đến khi hoàn thành hợp đồng và thanh toán.

---

## 📊 LUỒNG CHI TIẾT

### 1️⃣ **TRẠNG THÁI CĂN HỘ (Unit Status)**

#### **Các trạng thái:**
- **AVAILABLE** - Có thể cho thuê (sẵn sàng tạo hợp đồng)
- **ACTIVE** - Đang hoạt động (đã có hợp đồng đang hiệu lực)
- **VACANT** - Trống (không có hợp đồng)
- **MAINTENANCE** - Đang bảo trì
- **INACTIVE** - Không hoạt động

---

### 2️⃣ **LUỒNG ĐẦY ĐỦ**

#### **Bước 1: Căn hộ sẵn sàng cho thuê (AVAILABLE)**
```
Unit Status: AVAILABLE
↓
- Căn hộ đã sẵn sàng
- Không có hợp đồng đang hiệu lực
- Có thể tạo hợp đồng mới ngay
```

#### **Bước 2: Tạo hợp đồng cho thuê**
```
POST /api/contracts
{
  "unitId": "...",
  "contractType": "RENTAL",
  "contractNumber": "HD-2024-001",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "monthlyRent": 5000000,
  "status": "ACTIVE" (hoặc "INACTIVE" nếu startDate > today)
}
↓
Contract được tạo:
- Contract Status: ACTIVE (nếu startDate <= today) hoặc INACTIVE
- Unit Status: Giữ nguyên AVAILABLE (chưa tự động update)
```

**Lưu ý:** Hiện tại không tự động update Unit Status khi tạo contract. Cần:
- **Option 1:** Tự động update Unit Status từ AVAILABLE → ACTIVE khi tạo contract thành công
- **Option 2:** Admin update thủ công Unit Status
- **Option 3:** Dựa vào Contract Status để tính toán Unit Status

#### **Bước 3: Upload file hợp đồng (optional)**
```
POST /api/contracts/{contractId}/files
- Upload PDF/Image hợp đồng đã ký
```

#### **Bước 4: Kích hoạt hợp đồng (nếu tạo với status INACTIVE)**
```
Scheduled Job (ContractScheduler):
- Tự động activate contracts có startDate = today
- Contract Status: INACTIVE → ACTIVE
↓
Unit Status: Nên update từ AVAILABLE → ACTIVE
```

#### **Bước 5: Tạo hóa đơn tiền thuê (Invoice)**
```
Hiện tại: Chưa tự động tạo invoice khi tạo contract

Đề xuất luồng:
1. Khi contract được activate → Tạo invoice tiền thuê
2. Invoice Type: RENTAL
3. Invoice Status: PUBLISHED (chưa thanh toán)
4. Invoice Amount: monthlyRent × số tháng
5. Due Date: Theo payment terms
```

#### **Bước 6: Thanh toán tiền thuê**
```
POST /api/invoices/{invoiceId}/payment/vnpay
hoặc
POST /api/invoices/{invoiceId}/payment/manual
↓
Invoice Status: PUBLISHED → PAID
↓
Contract vẫn giữ nguyên ACTIVE
```

#### **Bước 7: Hợp đồng đang hiệu lực**
```
Contract Status: ACTIVE
Unit Status: ACTIVE
Invoice Status: PAID (đã thanh toán)

Trong thời gian hợp đồng:
- Có thể tạo invoice hàng tháng cho tiền thuê
- Theo dõi thanh toán
- Quản lý hợp đồng
```

#### **Bước 8: Checkout (Dọn ra)**
```
PUT /api/contracts/{contractId}/checkout?checkoutDate=2024-12-15
↓
Contract:
- checkoutDate: 2024-12-15
- status: ACTIVE → CANCELLED
↓
Unit Status: Nên update từ ACTIVE → AVAILABLE (hoặc VACANT)
```

#### **Bước 9: Hợp đồng hết hạn**
```
Khi endDate < today:
- Contract Status: ACTIVE → EXPIRED (tự động hoặc thủ công)
↓
Unit Status: Nên update từ ACTIVE → AVAILABLE
```

---

## 🔄 MỐI QUAN HỆ GIỮA UNIT STATUS VÀ CONTRACT

### **Quy tắc đề xuất:**

1. **Unit AVAILABLE** → Có thể tạo contract mới
2. **Contract ACTIVE** → Unit nên là ACTIVE (không thể cho thuê)
3. **Contract CANCELLED** → Unit về AVAILABLE
4. **Contract EXPIRED** → Unit về AVAILABLE
5. **Unit MAINTENANCE** → Không thể tạo contract
6. **Unit INACTIVE** → Không thể tạo contract

### **Logic tự động đề xuất:**

```java
// Khi tạo contract thành công (RENTAL):
if (contract.getStatus() == "ACTIVE") {
    updateUnitStatus(unitId, UnitStatus.ACTIVE);
}

// Khi contract checkout (CANCELLED):
updateUnitStatus(unitId, UnitStatus.AVAILABLE);

// Khi contract expired:
updateUnitStatus(unitId, UnitStatus.AVAILABLE);

// Khi tạo contract, validate:
if (unit.getStatus() != UnitStatus.AVAILABLE && unit.getStatus() != UnitStatus.VACANT) {
    throw new IllegalArgumentException("Unit must be AVAILABLE or VACANT to create contract");
}
```

---

## 💰 LUỒNG THANH TOÁN

### **Hiện tại:**
- Contract được tạo với `monthlyRent` (giả định đã thanh toán)
- Không có invoice tự động tạo

### **Đề xuất luồng mới:**

#### **Option A: Thanh toán trước (Pre-paid)**
```
1. Tạo contract với monthlyRent (đã nhận tiền)
2. Không tạo invoice
3. Contract status = ACTIVE ngay
```

#### **Option B: Thanh toán sau (Post-paid)**
```
1. Tạo contract với monthlyRent (số tiền cần thu)
2. Tự động tạo invoice cho tháng đầu
3. Invoice status = PUBLISHED (chưa thanh toán)
4. Contract status = ACTIVE
5. Khi thanh toán → Invoice status = PAID
```

#### **Option C: Tính toán tổng tiền theo ngày**
```
1. Tạo contract với monthlyRent
2. Tính tổng tiền:
   - Nếu startDate <= 15: tính cả tháng
   - Nếu startDate > 15: tính nửa tháng
3. Tạo invoice với tổng tiền đã tính
4. Invoice status = PUBLISHED
```

---

## 📋 CHECKLIST LUỒNG

### **Khi tạo Contract (RENTAL):**
- [ ] Validate Unit Status = AVAILABLE hoặc VACANT
- [ ] Tạo Contract với status ACTIVE hoặc INACTIVE
- [ ] Update Unit Status → ACTIVE (nếu contract ACTIVE)
- [ ] (Optional) Tạo Invoice tiền thuê
- [ ] (Optional) Upload file hợp đồng

### **Khi Contract được activate:**
- [ ] Contract Status → ACTIVE
- [ ] Update Unit Status → ACTIVE (nếu chưa)
- [ ] (Optional) Tạo Invoice tiền thuê đầu tiên

### **Khi thanh toán Invoice:**
- [ ] Invoice Status → PAID
- [ ] Contract vẫn giữ ACTIVE
- [ ] Gửi thông báo thanh toán thành công

### **Khi Checkout:**
- [ ] Set checkoutDate
- [ ] Contract Status → CANCELLED
- [ ] Update Unit Status → AVAILABLE
- [ ] (Optional) Tạo invoice cuối cùng nếu còn nợ

### **Khi Contract hết hạn:**
- [ ] Contract Status → EXPIRED
- [ ] Update Unit Status → AVAILABLE
- [ ] (Optional) Tạo invoice thanh lý nếu cần

---

## 🔧 CẦN IMPLEMENT

### **1. Tự động update Unit Status:**
- [ ] Khi tạo contract → Unit AVAILABLE/VACANT → ACTIVE
- [ ] Khi checkout → Unit ACTIVE → AVAILABLE
- [ ] Khi contract expired → Unit ACTIVE → AVAILABLE

### **2. Validation khi tạo contract:**
- [ ] Chỉ cho phép tạo contract nếu Unit = AVAILABLE hoặc VACANT
- [ ] Validate không có contract ACTIVE khác cho unit đó

### **3. Tự động tạo Invoice (optional):**
- [ ] Tạo invoice khi contract được activate
- [ ] Tính tổng tiền theo logic (ngày <= 15: cả tháng, > 15: nửa tháng)
- [ ] Invoice type = RENTAL

### **4. Scheduled Jobs:**
- [ ] Auto-activate contracts (đã có)
- [ ] Auto-expire contracts khi endDate < today
- [ ] Auto-update Unit Status khi contract expired

---

## 📊 STATE DIAGRAM

```
Unit: AVAILABLE
  ↓ [Tạo Contract]
Contract: ACTIVE
  ↓
Unit: ACTIVE
  ↓
Invoice: PUBLISHED → [Thanh toán] → PAID
  ↓
Contract: ACTIVE (tiếp tục)
  ↓ [Checkout hoặc Expired]
Contract: CANCELLED/EXPIRED
  ↓
Unit: AVAILABLE
```

---

## ✅ KẾT LUẬN

**Luồng hiện tại:**
- ✅ Tạo contract được
- ✅ Checkout được
- ❌ Chưa tự động update Unit Status
- ❌ Chưa tự động tạo Invoice
- ❌ Chưa validate Unit Status khi tạo contract

**Cần cải thiện:**
1. Thêm logic tự động update Unit Status
2. Thêm validation Unit Status khi tạo contract
3. (Optional) Tự động tạo Invoice khi activate contract

