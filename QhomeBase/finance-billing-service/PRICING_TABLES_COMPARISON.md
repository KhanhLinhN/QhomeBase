# So Sánh 2 Bảng: service_pricing và pricing_tiers

## Tổng quan

Hệ thống có **2 bảng pricing** với mục đích khác nhau:

1. **`service_pricing`** - Giá dịch vụ cơ bản (flat price)
2. **`pricing_tiers`** - Giá theo bậc (tiered pricing)

---

## 📊 So Sánh Chi Tiết

| Tiêu chí | **service_pricing** | **pricing_tiers** |
|----------|---------------------|-------------------|
| **Mục đích** | Giá dịch vụ cơ bản (flat price) | Giá theo bậc (tiered pricing - nhiều mức giá) |
| **Cấu trúc** | 1 giá cho 1 service | Nhiều bậc (tier) cho 1 service |
| **Khi nào dùng** | Khi KHÔNG có tiered pricing | Khi CÓ tiered pricing (nhiều bậc giá) |
| **Fields chính** | `base_price` | `min_quantity`, `max_quantity`, `unit_price`, `tier_order` |
| **Tax rate** | ✅ Có `tax_rate` | ❌ Không có (lấy từ service_pricing) |
| **Service name** | ✅ Có `service_name`, `category` | ❌ Không có |
| **Unit** | ✅ Có `unit` | ❌ Không có (hardcode "kWh") |

---

## 📋 Chi Tiết Bảng service_pricing

### **Cấu trúc:**
```sql
CREATE TABLE billing.service_pricing (
    id                UUID PRIMARY KEY,
    service_code      TEXT NOT NULL,
    service_name      TEXT NOT NULL,
    category          TEXT,
    base_price        NUMERIC(14,4) NOT NULL,  -- Giá cơ bản
    unit              TEXT NOT NULL,            -- Đơn vị (kWh, m3, month...)
    tax_rate          NUMERIC(5,2) NOT NULL,    -- Thuế suất (%)
    effective_from    DATE NOT NULL,
    effective_until   DATE,
    active            BOOLEAN NOT NULL,
    description       TEXT,
    ...
);
```

### **Đặc điểm:**
- ✅ **1 giá cho 1 service** - Không phân bậc
- ✅ **Có tax_rate** - Thuế suất
- ✅ **Có service_name, category** - Tên và nhóm dịch vụ
- ✅ **Có unit** - Đơn vị tính (kWh, m3, month...)
- ✅ **Dùng khi:** Service KHÔNG có tiered pricing (ví dụ: PARKING_CAR, PARKING_MOTORBIKE)

### **Khi nào dùng:**
- ✅ Dịch vụ **flat price** (1 giá cố định)
- ✅ Dịch vụ **parking** (phí gửi xe theo tháng)
- ✅ Dịch vụ **maintenance** (phí bảo trì)
- ✅ **Fallback** khi không có tiers trong `pricing_tiers`

---

## 📋 Chi Tiết Bảng pricing_tiers

### **Cấu trúc:**
```sql
CREATE TABLE billing.pricing_tiers (
    id                  UUID PRIMARY KEY,
    service_code        TEXT NOT NULL,
    tier_order          INTEGER NOT NULL,       -- Thứ tự bậc (1, 2, 3...)
    min_quantity        NUMERIC(14,3) NOT NULL, -- Lượng tối thiểu (ví dụ: 0 kWh)
    max_quantity        NUMERIC(14,3),         -- Lượng tối đa (ví dụ: 50 kWh, NULL = không giới hạn)
    unit_price          NUMERIC(14,4) NOT NULL, -- Giá cho bậc này
    effective_from      DATE NOT NULL,
    effective_until     DATE,
    active              BOOLEAN NOT NULL,
    description         TEXT,
    ...
);
```

### **Đặc điểm:**
- ✅ **Nhiều bậc (tiers)** - 1 service có thể có nhiều tier (Bậc 1, Bậc 2, Bậc 3...)
- ✅ **Tính theo khoảng lượng** - Mỗi bậc áp dụng cho một khoảng (min_quantity - max_quantity)
- ✅ **Sắp xếp theo tier_order** - Bậc 1, Bậc 2, Bậc 3...
- ❌ **Không có tax_rate** - Phải lấy từ `service_pricing`
- ❌ **Không có service_name, category** - Chỉ có service_code
- ❌ **Không có unit** - Hardcode "kWh" trong code

### **Khi nào dùng:**
- ✅ Dịch vụ **ELECTRIC** (điện - tính theo bậc)
- ✅ Dịch vụ **WATER** (nước - tính theo bậc)
- ✅ Dịch vụ có **nhiều mức giá** tùy theo lượng sử dụng

---

## 🔄 Logic Tính Giá (trong code)

### **MeterReadingImportService.calculateInvoiceLines():**

```java
// 1. ƯU TIÊN: Tìm tiers trong pricing_tiers
List<PricingTier> tiers = pricingTierRepository.findActiveTiersByServiceAndDate(serviceCode, serviceDate);

if (tiers.isEmpty()) {
    // 2. FALLBACK: Nếu không có tiers → dùng service_pricing
    BigDecimal unitPrice = resolveUnitPrice(serviceCode, serviceDate);
    // Tạo invoice line với giá cố định
}

// 3. Nếu có tiers → tính theo từng bậc
for (PricingTier tier : tiers) {
    // Tính giá cho từng bậc:
    // - Bậc 1: 0-50 kWh × 1800 VND/kWh
    // - Bậc 2: 51-100 kWh × 2100 VND/kWh
    // - Bậc 3: 101-200 kWh × 2500 VND/kWh
    // - Bậc 4: >200 kWh × 3000 VND/kWh
}
```

### **resolveUnitPrice()** (Fallback):

```java
private BigDecimal resolveUnitPrice(String serviceCode, LocalDate serviceDate) {
    // Lấy từ service_pricing.base_price
    return servicePricingRepository.findActivePriceGlobal(serviceCode, serviceDate)
        .map(ServicePricing::getBasePrice)
        .orElse(defaultPrice);
}
```

---

## 📝 Ví Dụ Cụ Thể

### **Example 1: ELECTRIC (Dùng pricing_tiers)**

**Bảng pricing_tiers:**
| service_code | tier_order | min_quantity | max_quantity | unit_price |
|-------------|------------|--------------|--------------|------------|
| ELECTRIC    | 1          | 0            | 50           | 1800       |
| ELECTRIC    | 2          | 51           | 100          | 2100       |
| ELECTRIC    | 3          | 101          | 200          | 2500       |
| ELECTRIC    | 4          | 201          | NULL         | 3000       |

**Usage = 150 kWh:**
- Bậc 1: 50 kWh × 1800 = 90,000 VND
- Bậc 2: 50 kWh × 2100 = 105,000 VND
- Bậc 3: 50 kWh × 2500 = 125,000 VND
- **Total = 320,000 VND**

---

### **Example 2: PARKING_CAR (Dùng service_pricing)**

**Bảng service_pricing:**
| service_code   | base_price | unit  | tax_rate |
|----------------|------------|-------|----------|
| PARKING_CAR    | 500000     | month | 0.1      |

**Usage = 1 month:**
- 1 month × 500,000 VND = **500,000 VND**
- Tax (10%): 50,000 VND
- **Total = 550,000 VND**

---

## 🎯 Kết Luận

### **Khi nào dùng service_pricing:**
1. ✅ Service **KHÔNG có tiered pricing** (1 giá cố định)
2. ✅ Service **parking**, **maintenance**, **other services**
3. ✅ **Fallback** khi không có tiers

### **Khi nào dùng pricing_tiers:**
1. ✅ Service **CÓ tiered pricing** (nhiều mức giá)
2. ✅ Service **ELECTRIC**, **WATER** (tính theo bậc)
3. ✅ Service có **nhiều tiers** tùy theo lượng sử dụng

### **Mối quan hệ:**
- `service_pricing` = **Giá cơ bản** (flat price)
- `pricing_tiers` = **Giá theo bậc** (tiered pricing)
- **Ưu tiên:** `pricing_tiers` → `service_pricing` (fallback)

---

## 📌 Lưu Ý

1. **Tax rate:** Chỉ có trong `service_pricing`, nếu dùng `pricing_tiers` thì lấy tax_rate từ `service_pricing` theo service_code.

2. **Unit:** Chỉ có trong `service_pricing`, trong `pricing_tiers` hardcode "kWh" trong code.

3. **Logic tính giá:** Code luôn **ưu tiên `pricing_tiers`** trước, nếu không có thì mới dùng `service_pricing`.

