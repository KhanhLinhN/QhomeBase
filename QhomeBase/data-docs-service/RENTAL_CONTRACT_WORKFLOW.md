# Luồng Hoạt Động Cho Hợp Đồng Thuê (RENTAL Contract Workflow)

## 📋 Tổng Quan

Tài liệu này mô tả đầy đủ luồng hoạt động của hợp đồng thuê (RENTAL contracts) từ khi tạo mới đến khi kết thúc, bao gồm các trạng thái, scheduled tasks, và các thao tác manual.

---

## 1. TẠO HỢP ĐỒNG MỚI (CREATE CONTRACT)

### 1.1. Endpoint
```
POST /api/contracts
Body: CreateContractRequest
```

### 1.2. Điều kiện
- **contractType**: `"RENTAL"` (mặc định) hoặc truyền vào request
- **monthlyRent**: Bắt buộc cho RENTAL contracts
- **startDate**: Bắt buộc
- **endDate**: Tùy chọn (có thể có hoặc không)
- **contractNumber**: Phải unique

### 1.3. Trạng thái ban đầu
```java
Contract Status:
  - Mặc định: "ACTIVE" (nếu không truyền status)
  - Có thể set: "INACTIVE" (nếu startDate > TODAY)

Renewal Status:
  - Mặc định: "PENDING"
  - renewalReminderSentAt: NULL
  - renewalDeclinedAt: NULL
```

### 1.4. Validation
- ✅ `startDate <= endDate` (nếu có endDate)
- ✅ `monthlyRent` phải > 0
- ✅ `contractNumber` không được trùng

---

## 2. TRẠNG THÁI HỢP ĐỒNG (CONTRACT STATUS)

### 2.1. Các trạng thái

| Status | Mô tả | Khi nào | Có thể làm gì |
|--------|-------|---------|---------------|
| **INACTIVE** | Chưa bắt đầu hiệu lực | `startDate > TODAY` | ❌ Không gửi reminder<br>❌ Không extend<br>❌ Không checkout |
| **ACTIVE** | Đang có hiệu lực | `startDate <= TODAY` | ✅ Gửi reminder<br>✅ Extend<br>✅ Checkout<br>✅ Cancel |
| **EXPIRED** | Đã hết hạn | `endDate < TODAY` (tự động, ngay cả 1 ngày sau endDate) | ❌ Không gửi reminder<br>❌ Không extend |
| **CANCELLED** | Đã hủy | Manual (checkout hoặc update status) | ❌ Không làm gì |

### 2.2. Chuyển đổi trạng thái

```
┌──────────┐
│ INACTIVE │
└────┬─────┘
     │ Scheduled task 00:00
     │ Khi startDate = TODAY
     ▼
┌──────────┐
│  ACTIVE  │◄──────────────┐
└────┬─────┘               │
     │                     │
     ├─────────────────────┤
     │ Scheduled task 01:00│
     │ Khi endDate < TODAY │
     │                     │
     │ Manual:             │
     │ - checkout          │
     │ - update status     │
     ▼                     │
┌──────────┐      ┌────────┴────┐
│ EXPIRED  │      │  CANCELLED  │
└──────────┘      └─────────────┘
```

---

## 3. TRẠNG THÁI GIA HẠN (RENEWAL STATUS)

**⚠️ LƯU Ý: Renewal Status CHỈ hoạt động khi Contract Status = ACTIVE**

### 3.1. Các trạng thái

| Renewal Status | Mô tả | Khi nào |
|----------------|-------|---------|
| **PENDING** | Chưa gửi reminder | Mới tạo hoặc sau khi extend |
| **REMINDED** | Đã gửi ít nhất 1 reminder | Sau reminder lần 1, 2, hoặc 3 |
| **DECLINED** | Đã từ chối gia hạn | Sau 20+ ngày từ reminder đầu tiên |

### 3.2. Chuyển đổi

```
PENDING ──[Reminder 1]──> REMINDED
                              │
                              ├──[Reminder 2 (7 ngày)]──> REMINDED (giữ nguyên)
                              │
                              ├──[Reminder 3 (20 ngày)]──> REMINDED (giữ nguyên)
                              │
                              └──[>20 ngày]──> DECLINED

DECLINED ──[Extend Contract]──> PENDING (reset chu kỳ mới)
```

---

## 4. SCHEDULED TASKS (Tác vụ tự động)

### 4.1. Activate Inactive Contracts ⏰ 00:00 hàng ngày

**Cron:** `0 0 0 * * ?`

**Logic:**
```java
Tìm contracts: status = 'INACTIVE' AND startDate = TODAY
→ Set status = 'ACTIVE'
```

**Kết quả:**
- Hợp đồng tự động kích hoạt khi đến ngày bắt đầu
- Renewal status `PENDING` bắt đầu có ý nghĩa

---

### 4.2. Mark Expired Contracts ⏰ 01:00 hàng ngày

**Cron:** `0 0 1 * * ?`

**Logic:**
```java
Tìm contracts: status = 'ACTIVE' 
              AND endDate IS NOT NULL 
              AND endDate < TODAY
→ Set status = 'EXPIRED'
```

**Lưu ý quan trọng:**
- EXPIRED được set **ngay khi endDate < TODAY** (ngay cả 1 ngày sau endDate)
- **KHÔNG phải** là quá 20 ngày
- 20 ngày là thời gian để mark **DECLINED** (từ reminder đầu tiên), không liên quan đến EXPIRED

**Kết quả:**
- Hợp đồng tự động đánh dấu hết hạn
- Không còn gửi renewal reminders

---

### 4.3. Send Renewal Reminders ⏰ 08:00 hàng ngày

**Cron:** `0 0 8 * * ?`

**Logic:**

#### Reminder 1 (Lần đầu - 30 ngày trước hết hạn)
```java
Tìm contracts: status = 'ACTIVE'
              AND contractType = 'RENTAL'
              AND renewalStatus = 'PENDING'
              AND endDate >= TODAY
              AND endDate <= TODAY + 30 days
              AND renewalReminderSentAt IS NULL

→ Set renewalReminderSentAt = NOW()
→ Set renewalStatus = 'REMINDED'
```

#### Reminder 2 (7 ngày sau reminder 1)
```java
Tìm contracts: status = 'ACTIVE'
              AND contractType = 'RENTAL'
              AND renewalStatus = 'REMINDED'
              AND renewalReminderSentAt <= 7 days ago
              AND renewalReminderSentAt > 20 days ago

→ Gửi reminder (không update renewalReminderSentAt)
→ Giữ nguyên renewalStatus = 'REMINDED'
```

#### Reminder 3 (20 ngày sau reminder 1 - DEADLINE)
```java
Tìm contracts: status = 'ACTIVE'
              AND contractType = 'RENTAL'
              AND renewalStatus = 'REMINDED'
              AND renewalReminderSentAt <= 20 days ago

→ Gửi reminder (không update renewalReminderSentAt)
→ Giữ nguyên renewalStatus = 'REMINDED'
```

**Lưu ý quan trọng:**
- `renewalReminderSentAt` CHỈ được set 1 lần (khi gửi reminder đầu tiên)
- Các reminder sau không update timestamp này
- Điều này đảm bảo tính toán "20 ngày" luôn từ reminder đầu tiên

---

### 4.4. Mark Renewal Declined ⏰ 09:00 hàng ngày

**Cron:** `0 0 9 * * ?`

**Logic:**
```java
Tìm contracts: status = 'ACTIVE'
              AND contractType = 'RENTAL'
              AND renewalStatus = 'REMINDED'
              AND renewalReminderSentAt <= 20 days ago

→ Set renewalDeclinedAt = NOW()
→ Set renewalStatus = 'DECLINED'
```

**Kết quả:**
- Đánh dấu hợp đồng đã từ chối gia hạn
- Không gửi reminder nữa
- Vẫn có thể extend thủ công

---

## 5. CÁC THAO TÁC MANUAL (Manual Operations)

### 5.1. Extend Contract (Gia hạn hợp đồng)

**Endpoint:**
```
PUT /api/contracts/{contractId}/extend?newEndDate=2026-12-31
```

**Điều kiện:**
- ✅ Contract Status = `ACTIVE`
- ✅ Contract Type = `RENTAL`
- ✅ Phải có `endDate` hiện tại
- ✅ `newEndDate > currentEndDate`

**Logic:**
```java
contract.setEndDate(newEndDate);
contract.setRenewalStatus("PENDING");        // Reset về PENDING
contract.setRenewalReminderSentAt(null);     // Reset timestamp
contract.setRenewalDeclinedAt(null);         // Reset declined date
// Contract Status: ACTIVE (giữ nguyên)
```

**Kết quả:**
- ✅ Hợp đồng được gia hạn
- ✅ Bắt đầu chu kỳ reminder mới
- ✅ Có thể nhận reminder lại sau 30 ngày trước ngày hết hạn mới

---

### 5.2. Checkout Contract (Dọn ra)

**Endpoint:**
```
PUT /api/contracts/{contractId}/checkout?checkoutDate=2025-12-15
```

**Điều kiện:**
- ✅ Contract Type = `RENTAL`
- ✅ `checkoutDate >= startDate`
- ✅ `checkoutDate <= endDate` (nếu có)

**Logic:**
```java
contract.setCheckoutDate(checkoutDate);
contract.setStatus("CANCELLED");
```

**Kết quả:**
- ✅ Hợp đồng bị hủy
- ✅ Không còn gửi reminder
- ✅ Không thể extend

---

### 5.3. Cancel Contract (Hủy hợp đồng)

**⚠️ LƯU Ý:** Hiện tại **KHÔNG có endpoint riêng** để cancel contract (ví dụ: `PUT /api/contracts/{contractId}/cancel`).

**Cách hủy hợp đồng:**

#### Cách 1: Checkout Contract (Khuyến nghị cho RENTAL)
```
PUT /api/contracts/{contractId}/checkout?checkoutDate=2025-12-15
```
- Set `checkoutDate` và tự động set `status = "CANCELLED"`
- Chỉ áp dụng cho RENTAL contracts
- Xem chi tiết ở mục 5.2

#### Cách 2: Update Contract Status (Thủ công)
```
PUT /api/contracts/{contractId}
Body: {
  "status": "CANCELLED"
}
```
- Có thể set `status = "CANCELLED"` thông qua update endpoint
- Áp dụng cho cả RENTAL và PURCHASE contracts

**Logic khi cancel:**
```java
contract.setStatus("CANCELLED");
```

**Kết quả:**
- ✅ Hợp đồng bị hủy
- ✅ Không còn gửi reminder
- ✅ Không thể extend
- ✅ Không thể activate lại

---

### 5.4. Update Contract (Cập nhật hợp đồng)

**Endpoint:**
```
PUT /api/contracts/{contractId}
Body: UpdateContractRequest
```

**Có thể cập nhật:**
- `startDate`, `endDate`
- `monthlyRent`
- `status` (manual)
- Các thông tin khác

**Lưu ý:**
- Nếu đổi `endDate`, renewal status không tự động reset
- Cần extend contract để reset renewal status

---

## 6. WORKFLOW ĐẦY ĐỦ (Complete Workflow)

### 6.1. Scenario 1: Hợp đồng hoạt động bình thường

```
📅 Ngày 01/12/2025:
├─ Tạo hợp đồng mới
│  ├─ startDate: 01/12/2025
│  ├─ endDate: 31/12/2025
│  ├─ status: ACTIVE
│  └─ renewalStatus: PENDING
│
📅 Ngày 01/12/2025 (08:00):
├─ Scheduled task chạy
│  └─ Không có reminder (chưa đến 30 ngày)
│
📅 Ngày 06/12/2025 (08:00):
├─ Scheduled task chạy
│  ├─ Tìm thấy hợp đồng: endDate = 31/12/2025 (25 ngày sau)
│  ├─ Gửi REMINDER 1
│  ├─ renewalReminderSentAt = 06/12/2025 08:00
│  └─ renewalStatus: PENDING → REMINDED
│
📅 Ngày 13/12/2025 (08:00):
├─ Scheduled task chạy
│  ├─ Tìm thấy hợp đồng: 7 ngày từ reminder 1
│  ├─ Gửi REMINDER 2
│  └─ renewalStatus: REMINDED (giữ nguyên)
│
📅 Ngày 26/12/2025 (08:00):
├─ Scheduled task chạy
│  ├─ Tìm thấy hợp đồng: 20 ngày từ reminder 1
│  ├─ Gửi REMINDER 3 (DEADLINE)
│  └─ renewalStatus: REMINDED (giữ nguyên)
│
📅 Ngày 27/12/2025 (09:00):
├─ Scheduled task mark declined chạy
│  ├─ Tìm thấy hợp đồng: 21 ngày từ reminder 1
│  ├─ renewalDeclinedAt = 27/12/2025 09:00
│  └─ renewalStatus: REMINDED → DECLINED
│
📅 Ngày 01/01/2026 (01:00):
├─ Scheduled task mark expired chạy
│  ├─ Tìm thấy hợp đồng: endDate (31/12/2025) < TODAY (01/01/2026)
│  ├─ Lưu ý: Chỉ cần 1 ngày sau endDate là đã EXPIRED
│  └─ status: ACTIVE → EXPIRED
```

---

### 6.2. Scenario 2: Gia hạn hợp đồng

```
📅 Ngày 15/12/2025:
├─ Hợp đồng đang ở:
│  ├─ status: ACTIVE
│  ├─ renewalStatus: REMINDED
│  ├─ endDate: 31/12/2025
│  └─ renewalReminderSentAt: 06/12/2025
│
├─ User gọi extendContract(newEndDate: 31/12/2026)
│  ├─ endDate: 31/12/2025 → 31/12/2026
│  ├─ renewalStatus: REMINDED → PENDING (reset)
│  ├─ renewalReminderSentAt: null (reset)
│  ├─ renewalDeclinedAt: null (reset)
│  └─ status: ACTIVE (giữ nguyên)
│
📅 Ngày 06/12/2026 (08:00):
├─ Scheduled task chạy
│  ├─ Tìm thấy hợp đồng: endDate = 31/12/2026 (25 ngày sau)
│  ├─ Gửi REMINDER 1 (chu kỳ mới)
│  └─ renewalStatus: PENDING → REMINDED
```

---

### 6.3. Scenario 3: Hợp đồng INACTIVE

```
📅 Ngày 01/12/2025:
├─ Tạo hợp đồng mới
│  ├─ startDate: 15/12/2025
│  ├─ endDate: 31/12/2025
│  ├─ status: INACTIVE
│  └─ renewalStatus: PENDING
│
📅 Ngày 01/12/2025 - 14/12/2025:
├─ Scheduled tasks chạy
│  └─ Không có reminder (status = INACTIVE)
│
📅 Ngày 15/12/2025 (00:00):
├─ Scheduled task activate chạy
│  ├─ Tìm thấy hợp đồng: status = INACTIVE, startDate = 15/12/2025
│  └─ status: INACTIVE → ACTIVE
│
📅 Ngày 15/12/2025 (08:00):
├─ Scheduled task reminder chạy
│  ├─ Tìm thấy hợp đồng: ACTIVE, endDate trong 30 ngày
│  ├─ Gửi REMINDER 1
│  └─ renewalStatus: PENDING → REMINDED
```

---

### 6.4. Scenario 4: Checkout sớm (Set CANCELLED)

```
📅 Ngày 20/12/2025:
├─ Hợp đồng đang ở:
│  ├─ status: ACTIVE
│  ├─ renewalStatus: REMINDED
│  ├─ endDate: 31/12/2025
│  └─ renewalReminderSentAt: 06/12/2025
│
├─ User gọi checkoutContract(checkoutDate: 20/12/2025)
│  ├─ checkoutDate: 20/12/2025
│  └─ status: ACTIVE → CANCELLED
│
📅 Ngày 21/12/2025 (08:00):
├─ Scheduled task reminder chạy
│  └─ Bỏ qua (status = CANCELLED)
│
📅 Ngày 01/01/2026 (01:00):
├─ Scheduled task mark expired chạy
│  └─ Bỏ qua (status = CANCELLED)
```

**Lưu ý:** Đây là cách chính để set status = CANCELLED. Không có endpoint riêng để cancel contract.

---

## 7. CÁC QUERY QUAN TRỌNG

### 7.1. Query tìm hợp đồng cần reminder lần 1
```sql
SELECT * FROM contracts 
WHERE status = 'ACTIVE'
  AND contract_type = 'RENTAL'
  AND end_date >= CURRENT_DATE
  AND end_date <= CURRENT_DATE + INTERVAL '30 days'
  AND renewal_status = 'PENDING'
```

### 7.2. Query tìm hợp đồng cần reminder lần 2
```sql
SELECT * FROM contracts 
WHERE status = 'ACTIVE'
  AND contract_type = 'RENTAL'
  AND renewal_status = 'REMINDED'
  AND renewal_reminder_sent_at <= CURRENT_TIMESTAMP - INTERVAL '7 days'
  AND renewal_reminder_sent_at > CURRENT_TIMESTAMP - INTERVAL '20 days'
```

### 7.3. Query tìm hợp đồng cần reminder lần 3
```sql
SELECT * FROM contracts 
WHERE status = 'ACTIVE'
  AND contract_type = 'RENTAL'
  AND renewal_status = 'REMINDED'
  AND renewal_reminder_sent_at <= CURRENT_TIMESTAMP - INTERVAL '20 days'
```

### 7.4. Query tìm hợp đồng cần mark declined
```sql
SELECT * FROM contracts 
WHERE status = 'ACTIVE'
  AND contract_type = 'RENTAL'
  AND renewal_status = 'REMINDED'
  AND renewal_reminder_sent_at <= CURRENT_TIMESTAMP - INTERVAL '20 days'
```

---

## 8. MANUAL TRIGGER ENDPOINTS (Testing)

### 8.1. Trigger Renewal Reminders
```
POST /api/contracts/renewal/trigger-reminders
```
**Mô tả:** Gửi reminders thủ công (dùng cho testing)

### 8.2. Trigger Mark Declined
```
POST /api/contracts/renewal/trigger-declined
```
**Mô tả:** Đánh dấu declined thủ công (dùng cho testing)

### 8.3. Trigger Mark Expired
```
POST /api/contracts/status/trigger-expired
```
**Mô tả:** Đánh dấu expired thủ công (dùng cho testing)

### 8.4. Trigger Activate Inactive
```
PUT /api/contracts/activate-inactive
```
**Mô tả:** Activate inactive contracts thủ công (dùng cho testing)

### 8.5. Manual Decline Renewal
```
PUT /api/contracts/{contractId}/renewal/decline
```
**Mô tả:** Đánh dấu declined thủ công cho một hợp đồng cụ thể

---

## 9. BẢNG TÓM TẮT

### 9.1. Điều kiện để gửi Reminder

| Điều kiện | Reminder 1 | Reminder 2 | Reminder 3 |
|-----------|------------|------------|------------|
| Contract Status | ACTIVE | ACTIVE | ACTIVE |
| Contract Type | RENTAL | RENTAL | RENTAL |
| Renewal Status | PENDING | REMINDED | REMINDED |
| End Date | Trong 30 ngày | Bất kỳ | Bất kỳ |
| Renewal Reminder Sent At | NULL | <= 7 ngày trước | <= 20 ngày trước |
| Days Since First Reminder | N/A | 7-19 ngày | >= 20 ngày |

### 9.2. Điều kiện để Extend Contract

| Điều kiện | Yêu cầu |
|-----------|---------|
| Contract Status | ACTIVE |
| Contract Type | RENTAL |
| Current End Date | Phải có |
| New End Date | > Current End Date |

### 9.3. Điều kiện để Checkout

| Điều kiện | Yêu cầu |
|-----------|---------|
| Contract Type | RENTAL |
| Checkout Date | >= startDate, <= endDate |

---

## 10. LƯU Ý QUAN TRỌNG

1. **Renewal Status CHỈ hoạt động khi Contract Status = ACTIVE**
   - INACTIVE, EXPIRED, CANCELLED → Không gửi reminder

2. **renewalReminderSentAt CHỈ được set 1 lần**
   - Khi gửi reminder đầu tiên
   - Các reminder sau không update timestamp này
   - Đảm bảo tính toán "20 ngày" luôn từ reminder đầu tiên

3. **EXPIRED vs DECLINED - Phân biệt rõ ràng:**
   - **EXPIRED**: Được set khi `endDate < TODAY` (ngay cả 1 ngày sau endDate)
     - Liên quan đến ngày kết thúc hợp đồng
     - Tự động set bởi scheduled task lúc 01:00
   - **DECLINED**: Được set sau 20 ngày từ reminder đầu tiên
     - Liên quan đến quá trình reminder gia hạn
     - Tự động set bởi scheduled task lúc 09:00
   - **KHÔNG có mối quan hệ**: EXPIRED không phụ thuộc vào 20 ngày, và ngược lại

4. **CANCELLED - Khi nào xuất hiện:**
   - **Cách 1:** Gọi `checkoutContract()` - set `checkoutDate` và `status = CANCELLED`
     - Khuyến nghị cho RENTAL contracts
     - Tự động set status = CANCELLED khi checkout
   - **Cách 2:** Update contract và set `status = "CANCELLED"` thủ công
     - Qua endpoint `PUT /api/contracts/{contractId}` với body `{"status": "CANCELLED"}`
     - Áp dụng cho cả RENTAL và PURCHASE contracts
   - **⚠️ KHÔNG có endpoint riêng** để cancel contract (ví dụ: `PUT /api/contracts/{contractId}/cancel`)
   - Khi CANCELLED: Không gửi reminder, không extend, không activate lại

5. **Extend Contract = Reset Renewal Status**
   - Reset về PENDING
   - Xóa các timestamps
   - Bắt đầu chu kỳ mới

6. **Scheduled Tasks chạy theo thứ tự**
   - 00:00: Activate Inactive
   - 01:00: Mark Expired
   - 08:00: Send Reminders
   - 09:00: Mark Declined

7. **Contract Status là trạng thái CHÍNH**
   - Quyết định liệu contract có thể nhận reminders hay không
   - Renewal Status chỉ là trạng thái PHỤ cho quá trình reminder

---

## 11. VÍ DỤ THỰC TẾ

### Example 1: Timeline đầy đủ

```
Hợp đồng: startDate = 01/12/2025, endDate = 31/12/2025

01/12/2025 00:00 - Tạo hợp đồng: ACTIVE, PENDING
06/12/2025 08:00 - Reminder 1: REMINDED (25 ngày trước hết hạn)
13/12/2025 08:00 - Reminder 2: REMINDED (7 ngày sau reminder 1)
26/12/2025 08:00 - Reminder 3: REMINDED (20 ngày sau reminder 1)
27/12/2025 09:00 - Mark Declined: DECLINED (21 ngày sau reminder 1)
01/01/2026 01:00 - Mark Expired: EXPIRED (endDate đã qua)
```

### Example 2: Extend sau khi declined

```
Hợp đồng: endDate = 31/12/2025, renewalStatus = DECLINED

15/12/2025 - User extend: endDate → 31/12/2026
            → renewalStatus: DECLINED → PENDING (reset)
            
06/12/2026 08:00 - Reminder 1: REMINDED (chu kỳ mới bắt đầu)
```

---

**Tài liệu này mô tả đầy đủ luồng hoạt động cho hợp đồng thuê (RENTAL contracts).**

