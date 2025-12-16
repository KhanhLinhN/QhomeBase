# Gia hạn bằng cách tạo hợp đồng mới (Renewal by Creating New Contract)

## Logic hiện tại vs Logic mới

### Logic hiện tại (Extend):
- **Method:** `extendContract()`
- **Hành động:** Update `endDate` của hợp đồng cũ
- **Kết quả:** 1 hợp đồng với endDate được kéo dài

### Logic mới (Tạo mới):
- **Method:** `renewContract()` hoặc `renewContractByCreatingNew()`
- **Hành động:** 
  1. Đóng hợp đồng cũ (set status = EXPIRED hoặc CANCELLED)
  2. Tạo hợp đồng mới
- **Kết quả:** 2 hợp đồng (cũ: EXPIRED, mới: ACTIVE)

---

## Code Implementation

### 1. Method mới trong ContractService.java

```java
@Transactional
public ContractDto renewContractByCreatingNew(UUID oldContractId, LocalDate newEndDate, UUID createdBy) {
    // 1. Lấy hợp đồng cũ
    Contract oldContract = contractRepository.findById(oldContractId)
            .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + oldContractId));
    
    if (!"RENTAL".equals(oldContract.getContractType())) {
        throw new IllegalArgumentException("Only RENTAL contracts can be renewed");
    }
    
    if (!"ACTIVE".equals(oldContract.getStatus())) {
        throw new IllegalArgumentException("Only ACTIVE contracts can be renewed");
    }
    
    if (oldContract.getEndDate() == null) {
        throw new IllegalArgumentException("Old contract must have end date to renew");
    }
    
    if (newEndDate.isBefore(oldContract.getEndDate()) || newEndDate.isEqual(oldContract.getEndDate())) {
        throw new IllegalArgumentException("New end date must be after old contract end date");
    }
    
    // 2. Đóng hợp đồng cũ
    oldContract.setStatus("EXPIRED");
    oldContract.setUpdatedBy(createdBy);
    contractRepository.save(oldContract);
    log.info("Closed old contract {} (endDate: {})", oldContractId, oldContract.getEndDate());
    
    // 3. Tạo hợp đồng mới
    LocalDate newStartDate = oldContract.getEndDate().plusDays(1); // Bắt đầu ngay sau khi cũ hết hạn
    
    // Tạo contract number mới (ví dụ: thêm suffix)
    String newContractNumber = generateRenewalContractNumber(oldContract.getContractNumber());
    
    Contract newContract = Contract.builder()
            .unitId(oldContract.getUnitId())
            .contractNumber(newContractNumber)
            .contractType("RENTAL")
            .startDate(newStartDate)
            .endDate(newEndDate)
            .monthlyRent(oldContract.getMonthlyRent()) // Giữ nguyên giá thuê, hoặc có thể update
            .paymentMethod(oldContract.getPaymentMethod())
            .paymentTerms(oldContract.getPaymentTerms())
            .notes(oldContract.getNotes() != null ? 
                   oldContract.getNotes() + " [Renewed from contract: " + oldContract.getContractNumber() + "]" 
                   : "Renewed from contract: " + oldContract.getContractNumber())
            .status("ACTIVE") // Hoặc "INACTIVE" nếu newStartDate > TODAY
            .createdBy(createdBy)
            .build();
    
    // Nếu startDate trong tương lai, set INACTIVE
    if (newStartDate.isAfter(LocalDate.now())) {
        newContract.setStatus("INACTIVE");
    }
    
    newContract = contractRepository.save(newContract);
    log.info("Created new contract {} (contractNumber: {}, startDate: {}, endDate: {}) for renewal of contract {}", 
            newContract.getId(), newContractNumber, newStartDate, newEndDate, oldContractId);
    
    return toDto(newContract);
}

// Helper method để tạo contract number mới
private String generateRenewalContractNumber(String oldContractNumber) {
    // Cách 1: Thêm suffix -RENEWAL-1, -RENEWAL-2, ...
    int renewalCount = contractRepository.countByContractNumberStartingWith(oldContractNumber + "-RENEWAL-");
    return oldContractNumber + "-RENEWAL-" + (renewalCount + 1);
    
    // Hoặc cách 2: Tạo số mới hoàn toàn
    // return "REN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
}
```

---

### 2. Thêm Repository method (nếu cần)

```java
// Trong ContractRepository.java
int countByContractNumberStartingWith(String prefix);
```

Hoặc nếu không có, có thể query:
```java
@Query("SELECT COUNT(c) FROM Contract c WHERE c.contractNumber LIKE :prefix%")
int countByContractNumberPrefix(@Param("prefix") String prefix);
```

---

### 3. Endpoint mới trong ContractController.java

```java
@PostMapping("/{contractId}/renew")
@Operation(summary = "Renew contract by creating new contract", 
           description = "Renew a RENTAL contract by closing the old one and creating a new contract. The old contract will be set to EXPIRED.")
public ResponseEntity<ContractDto> renewContract(
        @PathVariable UUID contractId,
        @RequestParam("newEndDate") java.time.LocalDate newEndDate,
        @RequestParam(value = "createdBy", required = false) UUID createdBy) {
    
    if (createdBy == null) {
        createdBy = UUID.randomUUID();
    }
    
    ContractDto newContract = contractService.renewContractByCreatingNew(contractId, newEndDate, createdBy);
    return ResponseEntity.ok(newContract);
}
```

---

### 4. So sánh 2 cách

| Tiêu chí | Extend (hiện tại) | Tạo mới |
|----------|-------------------|---------|
| **Số lượng hợp đồng** | 1 (update) | 2 (cũ + mới) |
| **Lịch sử** | Không có lịch sử rõ ràng | Có lịch sử đầy đủ |
| **Contract Number** | Giữ nguyên | Tạo mới |
| **Phức tạp** | Đơn giản | Phức tạp hơn |
| **Theo dõi** | Khó theo dõi các lần gia hạn | Dễ theo dõi |
| **Báo cáo** | Khó báo cáo | Dễ báo cáo |

---

### 5. Hybrid Approach (Cả 2 cách)

Có thể giữ cả 2 endpoint:
- `PUT /api/contracts/{contractId}/extend` - Extend hợp đồng hiện tại
- `POST /api/contracts/{contractId}/renew` - Tạo hợp đồng mới

---

### 6. Logic bổ sung (nếu cần)

#### Option A: Tự động update monthlyRent
```java
@RequestParam(value = "newMonthlyRent", required = false) BigDecimal newMonthlyRent

if (newMonthlyRent != null) {
    newContract.setMonthlyRent(newMonthlyRent);
} else {
    newContract.setMonthlyRent(oldContract.getMonthlyRent()); // Giữ nguyên
}
```

#### Option B: Link contracts (parent-child relationship)
Nếu muốn link contracts lại với nhau, có thể thêm field:
```java
@Column(name = "parent_contract_id")
private UUID parentContractId; // Hợp đồng gốc

@Column(name = "renewal_sequence")
private Integer renewalSequence; // Số lần gia hạn (1, 2, 3...)
```

---

## Lưu ý quan trọng

1. **Contract Number:** Phải unique, cần logic tạo số mới
2. **Start Date:** Nên là `oldEndDate + 1` để không có gap
3. **Old Contract Status:** Nên set `EXPIRED` hoặc có thể thêm status `RENEWED`
4. **Renewal Status:** Hợp đồng mới sẽ có `renewalStatus = PENDING` (mặc định)
5. **Files:** Files của hợp đồng cũ có thể copy sang mới nếu cần

---

## Migration (nếu cần thêm fields)

```sql
-- Nếu muốn link contracts
ALTER TABLE files.contracts 
ADD COLUMN parent_contract_id UUID REFERENCES files.contracts(id),
ADD COLUMN renewal_sequence INTEGER DEFAULT 0;
```

---

## Quyết định

Bạn muốn:
1. **Giữ logic hiện tại (Extend)** - Đơn giản, 1 hợp đồng
2. **Chuyển sang tạo mới** - Phức tạp hơn, có lịch sử rõ ràng
3. **Cả 2 cách** - Linh hoạt, user chọn


