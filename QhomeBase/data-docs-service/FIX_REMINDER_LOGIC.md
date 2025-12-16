# Fix Reminder Logic - Chỉ gửi mỗi reminder 1 lần

## Vấn đề hiện tại:
- Reminder 2: Gửi lại mỗi ngày từ ngày 7-19 (>= 7 && < 20)
- Reminder 3: Gửi lại mỗi ngày từ ngày 20 trở đi (>= 20)

## Giải pháp: Kiểm tra chính xác số ngày

---

## 1. Sửa ContractScheduler.java

### File: `src/main/java/com/QhomeBase/datadocsservice/service/ContractScheduler.java`

### Phần Reminder 2 (dòng 102-124):

**CODE CŨ:**
```java
int secondReminderCount = 0;
List<Contract> secondReminderContracts = contractService.findContractsNeedingSecondReminder();
for (Contract contract : secondReminderContracts) {
    try {
        if (contract.getEndDate() != null 
                && "REMINDED".equals(contract.getRenewalStatus())
                && contract.getRenewalReminderSentAt() != null) {
            long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                contract.getRenewalReminderSentAt().toLocalDate(),
                today
            );
            
            if (daysSinceFirstReminder >= 7 && daysSinceFirstReminder < 20) {  // ❌ SAI: Gửi lại mỗi ngày
                contractService.sendRenewalReminder(contract.getId());
                secondReminderCount++;
                log.info("Sent second renewal reminder for contract {} (expires on {}, {} days since first reminder)", 
                        contract.getContractNumber(), contract.getEndDate(), daysSinceFirstReminder);
            }
        }
    } catch (Exception e) {
        log.error("Error sending second renewal reminder for contract {}", contract.getId(), e);
    }
}
```

**CODE MỚI (SỬA):**
```java
int secondReminderCount = 0;
List<Contract> secondReminderContracts = contractService.findContractsNeedingSecondReminder();
for (Contract contract : secondReminderContracts) {
    try {
        if (contract.getEndDate() != null 
                && "REMINDED".equals(contract.getRenewalStatus())
                && contract.getRenewalReminderSentAt() != null) {
            long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                contract.getRenewalReminderSentAt().toLocalDate(),
                today
            );
            
            // ✅ SỬA: Chỉ gửi đúng ngày thứ 7 (chính xác 7 ngày sau reminder 1)
            if (daysSinceFirstReminder == 7) {
                contractService.sendRenewalReminder(contract.getId());
                secondReminderCount++;
                log.info("Sent second renewal reminder for contract {} (expires on {}, {} days since first reminder)", 
                        contract.getContractNumber(), contract.getEndDate(), daysSinceFirstReminder);
            }
        }
    } catch (Exception e) {
        log.error("Error sending second renewal reminder for contract {}", contract.getId(), e);
    }
}
```

---

### Phần Reminder 3 (dòng 126-148):

**CODE CŨ:**
```java
int thirdReminderCount = 0;
List<Contract> thirdReminderContracts = contractService.findContractsNeedingThirdReminder();
for (Contract contract : thirdReminderContracts) {
    try {
        if (contract.getEndDate() != null 
                && "REMINDED".equals(contract.getRenewalStatus())
                && contract.getRenewalReminderSentAt() != null) {
            long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                contract.getRenewalReminderSentAt().toLocalDate(),
                today
            );
            
            if (daysSinceFirstReminder >= 20) {  // ❌ SAI: Gửi lại mỗi ngày từ ngày 20 trở đi
                contractService.sendRenewalReminder(contract.getId());
                thirdReminderCount++;
                log.info("Sent third (FINAL) renewal reminder for contract {} (expires on {}, {} days since first reminder - THIS IS THE DEADLINE)", 
                        contract.getContractNumber(), contract.getEndDate(), daysSinceFirstReminder);
            }
        }
    } catch (Exception e) {
        log.error("Error sending third renewal reminder for contract {}", contract.getId(), e);
    }
}
```

**CODE MỚI (SỬA):**
```java
int thirdReminderCount = 0;
List<Contract> thirdReminderContracts = contractService.findContractsNeedingThirdReminder();
for (Contract contract : thirdReminderContracts) {
    try {
        if (contract.getEndDate() != null 
                && "REMINDED".equals(contract.getRenewalStatus())
                && contract.getRenewalReminderSentAt() != null) {
            long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                contract.getRenewalReminderSentAt().toLocalDate(),
                today
            );
            
            // ✅ SỬA: Chỉ gửi đúng ngày thứ 20 (chính xác 20 ngày sau reminder 1)
            if (daysSinceFirstReminder == 20) {
                contractService.sendRenewalReminder(contract.getId());
                thirdReminderCount++;
                log.info("Sent third (FINAL) renewal reminder for contract {} (expires on {}, {} days since first reminder - THIS IS THE DEADLINE)", 
                        contract.getContractNumber(), contract.getEndDate(), daysSinceFirstReminder);
            }
        }
    } catch (Exception e) {
        log.error("Error sending third renewal reminder for contract {}", contract.getId(), e);
    }
}
```

---

## 2. Tóm tắt thay đổi:

| Phần | Dòng | Thay đổi |
|------|------|----------|
| **Reminder 2** | 114 | `if (daysSinceFirstReminder >= 7 && daysSinceFirstReminder < 20)` → `if (daysSinceFirstReminder == 7)` |
| **Reminder 3** | 138 | `if (daysSinceFirstReminder >= 20)` → `if (daysSinceFirstReminder == 20)` |

---

## 3. Kết quả sau khi sửa:

### Timeline mới:
```
Ngày 0:  Reminder 1 ✅
         → renewalStatus: PENDING → REMINDED
         
Ngày 7:  Reminder 2 ✅ (chỉ gửi 1 lần)
         
Ngày 8-19: Không gửi gì (chờ đến ngày 20)
         
Ngày 20: Reminder 3 ✅ (chỉ gửi 1 lần)
         
Ngày 21: Scheduled task markRenewalDeclined chạy
         → renewalStatus: REMINDED → DECLINED
         
Ngày 22+: Không gửi reminder nữa (đã DECLINED)
```

---

## 4. Lưu ý:

- **Ưu điểm:** Đơn giản, không cần thêm field mới vào database
- **Nhược điểm:** Nếu scheduled task bị lỗi hoặc skip ngày đó, sẽ mất reminder (nhưng có thể trigger thủ công)
- **Đề xuất:** Vẫn giữ điều kiện query `<= 7 ngày trước` và `<= 20 ngày trước` để tìm contracts, nhưng chỉ gửi khi `== 7` và `== 20`

---

## 5. Alternative: Nếu muốn cho phép gửi lại nếu đã lỡ

Có thể sửa thành:
- Reminder 2: `if (daysSinceFirstReminder >= 7 && daysSinceFirstReminder < 20)` - nhưng thêm check: chỉ gửi nếu chưa gửi reminder 3
- Reminder 3: `if (daysSinceFirstReminder >= 20 && daysSinceFirstReminder < 21)` - chỉ gửi trong ngày thứ 20

Nhưng cách này vẫn có thể gửi lại nếu task chạy nhiều lần trong ngày.


