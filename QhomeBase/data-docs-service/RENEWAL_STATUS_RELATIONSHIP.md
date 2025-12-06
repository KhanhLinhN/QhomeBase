# Mối Quan Hệ Giữa Contract Status và Renewal Status

## Tóm tắt

**Renewal Status CHỈ có ý nghĩa và hoạt động khi Contract Status = ACTIVE**

## 1. Quy tắc Cơ bản

### Contract Status (Trạng thái Hợp đồng)
- Trạng thái **chính** của hợp đồng
- Áp dụng cho **TẤT CẢ** loại hợp đồng (RENTAL và PURCHASE)
- Các giá trị: `ACTIVE`, `INACTIVE`, `EXPIRED`, `CANCELLED`, `TERMINATED`

### Renewal Status (Trạng thái Gia hạn)
- Trạng thái **phụ** chỉ áp dụng cho **RENTAL contracts**
- **CHỈ hoạt động** khi Contract Status = `ACTIVE`
- Các giá trị: `PENDING`, `REMINDED`, `DECLINED`

## 2. Mối Quan Hệ Chi Tiết

### 2.1. Renewal Status CHỈ hoạt động khi Contract Status = ACTIVE

**Tất cả các query tìm contracts cần renewal reminder đều có điều kiện:**
```java
WHERE c.status = 'ACTIVE'
  AND c.contractType = 'RENTAL'
  AND c.renewalStatus = 'PENDING' (hoặc 'REMINDED')
```

**Validation trong code:**
```java
// ContractService.sendRenewalReminder()
if (!"ACTIVE".equals(contract.getStatus())) {
    throw new IllegalArgumentException("Only ACTIVE contracts can have renewal reminders");
}
```

**Kết luận:**
- ❌ Hợp đồng `INACTIVE` → **KHÔNG** gửi renewal reminder
- ❌ Hợp đồng `EXPIRED` → **KHÔNG** gửi renewal reminder
- ❌ Hợp đồng `CANCELLED` → **KHÔNG** gửi renewal reminder
- ✅ Chỉ hợp đồng `ACTIVE` → **CÓ THỂ** gửi renewal reminder

### 2.2. Khi Contract Status thay đổi, Renewal Status bị ảnh hưởng

#### ACTIVE → EXPIRED
- **Khi nào:** Khi `endDate < TODAY`, scheduled task tự động mark EXPIRED
- **Ảnh hưởng đến Renewal Status:**
  - Contract không còn được check cho renewal reminders
  - Renewal status giữ nguyên (PENDING/REMINDED/DECLINED) nhưng không còn ý nghĩa
  - Không thể gửi reminder nữa

#### ACTIVE → CANCELLED
- **Khi nào:** Khi `cancelContract()` hoặc `checkoutContract()`
- **Ảnh hưởng đến Renewal Status:**
  - Renewal status giữ nguyên
  - Không thể gửi reminder nữa
  - Không thể extend contract

#### INACTIVE → ACTIVE
- **Khi nào:** Khi đến `startDate`, scheduled task tự động activate
- **Ảnh hưởng đến Renewal Status:**
  - Sau khi chuyển sang ACTIVE, renewal status `PENDING` bắt đầu có ý nghĩa
  - Có thể nhận renewal reminders nếu `endDate` trong 30 ngày tới

### 2.3. Khi Renewal Status thay đổi, Contract Status KHÔNG đổi

- `PENDING` → `REMINDED`: Contract Status vẫn `ACTIVE`
- `REMINDED` → `DECLINED`: Contract Status vẫn `ACTIVE`
- Renewal status chỉ phản ánh trạng thái của quá trình nhắc nhở gia hạn, không ảnh hưởng đến trạng thái chính của hợp đồng

### 2.4. Extend Contract - Reset Renewal Status

Khi extend contract (`extendContract()`):
```java
contract.setEndDate(newEndDate);
contract.setRenewalStatus("PENDING"); // Reset về PENDING
contract.setRenewalReminderSentAt(null);
contract.setRenewalDeclinedAt(null);
// Contract Status vẫn giữ nguyên ACTIVE
```

**Lý do:** Gia hạn hợp đồng = bắt đầu một chu kỳ reminder mới

## 3. Workflow Tích Hợp

### 3.1. Lifecycle của RENTAL Contract với Renewal Status

```
1. CREATE CONTRACT
   ├─ Contract Status: INACTIVE (nếu startDate > TODAY) hoặc ACTIVE
   └─ Renewal Status: PENDING

2. ACTIVATE (nếu INACTIVE)
   ├─ Contract Status: INACTIVE → ACTIVE (tự động khi đến startDate)
   └─ Renewal Status: PENDING (bắt đầu có ý nghĩa)

3. RENEWAL REMINDER WORKFLOW (CHỈ khi ACTIVE)
   ├─ Reminder 1 (30 ngày trước endDate)
   │  └─ Renewal Status: PENDING → REMINDED
   ├─ Reminder 2 (7 ngày sau reminder 1)
   │  └─ Renewal Status: REMINDED (không đổi)
   ├─ Reminder 3 (20 ngày sau reminder 1 - deadline)
   │  └─ Renewal Status: REMINDED (không đổi)
   └─ Mark Declined (21+ ngày sau reminder 1)
      └─ Renewal Status: REMINDED → DECLINED

4. EXTEND CONTRACT (nếu cần)
   ├─ Contract Status: ACTIVE (giữ nguyên)
   └─ Renewal Status: DECLINED/REMINDED → PENDING (reset)

5. EXPIRY (tự động)
   ├─ Contract Status: ACTIVE → EXPIRED (khi endDate < TODAY)
   └─ Renewal Status: Giữ nguyên nhưng không còn ý nghĩa

6. CANCELLATION (manual)
   ├─ Contract Status: ACTIVE → CANCELLED
   └─ Renewal Status: Giữ nguyên nhưng không còn ý nghĩa
```

### 3.2. Khi nào Renewal Status được sử dụng?

| Contract Status | Renewal Status | Có gửi Reminder? | Có thể Extend? |
|----------------|----------------|------------------|----------------|
| ACTIVE | PENDING | ✅ Có (nếu endDate trong 30 ngày) | ✅ Có |
| ACTIVE | REMINDED | ✅ Có (reminder lần 2, 3) | ✅ Có |
| ACTIVE | DECLINED | ❌ Không | ✅ Có (reset về PENDING) |
| INACTIVE | PENDING/REMINDED/DECLINED | ❌ Không | ❌ Không |
| EXPIRED | PENDING/REMINDED/DECLINED | ❌ Không | ❌ Không |
| CANCELLED | PENDING/REMINDED/DECLINED | ❌ Không | ❌ Không |

## 4. Code Logic

### 4.1. Tất cả queries cho renewal đều filter ACTIVE

```java
// ContractRepository.findContractsNeedingFirstReminder()
@Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' " +
       "AND c.contractType = 'RENTAL' " +
       "AND c.endDate IS NOT NULL " +
       "AND c.endDate >= :startDate " +
       "AND c.endDate <= :endDate " +
       "AND c.renewalStatus = 'PENDING'")
```

### 4.2. Validation trước khi gửi reminder

```java
// ContractService.sendRenewalReminder()
if (!"RENTAL".equals(contract.getContractType())) {
    throw new IllegalArgumentException("Only RENTAL contracts...");
}

if (!"ACTIVE".equals(contract.getStatus())) {
    throw new IllegalArgumentException("Only ACTIVE contracts...");
}
```

### 4.3. Scheduled Tasks

1. **00:00 - Activate Inactive Contracts**
   - INACTIVE → ACTIVE
   - Sau khi activate, renewal status bắt đầu có ý nghĩa

2. **01:00 - Mark Expired Contracts**
   - ACTIVE → EXPIRED (nếu endDate < TODAY)
   - Sau khi expired, renewal status không còn ý nghĩa

3. **08:00 - Send Renewal Reminders**
   - **CHỈ** xử lý contracts có status = ACTIVE
   - Gửi reminder lần 1, 2, 3

4. **09:00 - Mark Renewal Declined**
   - **CHỈ** xử lý contracts có status = ACTIVE
   - REMINDED → DECLINED

## 5. Best Practices

### 5.1. Khi query contracts
- Luôn filter theo Contract Status trước
- Sau đó mới filter theo Renewal Status

### 5.2. Khi hiển thị trên UI
- Contract Status là trạng thái chính (luôn hiển thị)
- Renewal Status chỉ hiển thị khi:
  - Contract Type = RENTAL
  - Contract Status = ACTIVE
  - endDate IS NOT NULL

### 5.3. Khi test
- Test renewal workflow với contracts ACTIVE
- Test contract status changes riêng biệt
- Verify rằng contracts không ACTIVE không nhận reminders

## 6. Ví dụ Thực tế

### Ví dụ 1: Hợp đồng sắp hết hạn
```
Contract Status: ACTIVE
Renewal Status: PENDING
endDate: 2025-12-21 (20 ngày sau)
→ Sẽ nhận reminder lần 1 vào ngày 2025-12-06
→ Renewal Status: PENDING → REMINDED
```

### Ví dụ 2: Hợp đồng đã hết hạn
```
Contract Status: ACTIVE
Renewal Status: REMINDED
endDate: 2025-12-01 (đã qua 5 ngày)
→ Scheduled task mark EXPIRED
→ Contract Status: ACTIVE → EXPIRED
→ Renewal Status: REMINDED (giữ nguyên nhưng không còn ý nghĩa)
→ Không nhận reminder nữa
```

### Ví dụ 3: Gia hạn hợp đồng
```
Contract Status: ACTIVE
Renewal Status: DECLINED
endDate: 2025-12-20
→ Gọi extendContract(newEndDate = 2026-12-20)
→ Contract Status: ACTIVE (giữ nguyên)
→ Renewal Status: DECLINED → PENDING (reset)
→ Bắt đầu chu kỳ reminder mới
```

## 7. Kết luận

1. **Renewal Status là một tính năng PHỤ chỉ áp dụng cho RENTAL contracts**
2. **Renewal Status CHỈ hoạt động khi Contract Status = ACTIVE**
3. **Contract Status là trạng thái CHÍNH, quyết định liệu contract có thể nhận reminders hay không**
4. **Khi Contract Status thay đổi (EXPIRED/CANCELLED), Renewal Status không còn ý nghĩa**
5. **Khi extend contract, Renewal Status được reset về PENDING để bắt đầu chu kỳ mới**

