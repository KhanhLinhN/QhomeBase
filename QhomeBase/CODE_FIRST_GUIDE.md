# Hướng dẫn chuyển sang Code-First với Hibernate

## ⚠️ CẢNH BÁO QUAN TRỌNG

**Code-First KHÔNG phù hợp cho production database đã có dữ liệu!**
- Hibernate có thể xóa/sửa dữ liệu hiện có
- Khó kiểm soát version schema
- Không có rollback như Flyway migrations

**Chỉ nên dùng Code-First cho:**
- Development/Testing môi trường
- Dự án mới chưa có dữ liệu
- Prototype nhanh

---

## Các bước chuyển sang Code-First

### Bước 1: Thay đổi cấu hình `application.properties`

#### Option 1: `update` (Khuyến nghị cho dev)
```properties
######## Database Configuration  #########
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/qhome_base_db?currentSchema=svc,data,public}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.jpa.open-in-view=false

# Thay đổi từ validate sang update
spring.jpa.hibernate.ddl-auto=update

# Bật log SQL để debug
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Tắt Flyway (vì Hibernate sẽ tự quản lý schema)
spring.flyway.enabled=false
```

#### Option 2: `create` (Tạo mới mỗi lần start - CHỈ cho test)
```properties
spring.jpa.hibernate.ddl-auto=create
# ⚠️ CẢNH BÁO: Sẽ XÓA TẤT CẢ DỮ LIỆU mỗi lần restart!
```

#### Option 3: `create-drop` (Tạo khi start, xóa khi shutdown - CHỈ cho test)
```properties
spring.jpa.hibernate.ddl-auto=create-drop
# ⚠️ CẢNH BÁO: Sẽ XÓA TẤT CẢ DỮ LIỆU khi shutdown!
```

---

## Các giá trị `ddl-auto` và ý nghĩa

| Giá trị | Mô tả | Khi nào dùng |
|---------|-------|--------------|
| `validate` | Chỉ kiểm tra schema khớp với entities | **Production** (hiện tại) |
| `update` | Tự động tạo/sửa bảng, không xóa | **Development** |
| `create` | Xóa và tạo lại schema mỗi lần start | **Test only** |
| `create-drop` | Tạo khi start, xóa khi shutdown | **Test only** |
| `none` | Không làm gì | Khi dùng Flyway hoàn toàn |

---

## Bước 2: Đảm bảo Entities có đầy đủ annotations

Ví dụ entity cần có:

```java
@Entity
@Table(name = "meter_reading_reminders", schema = "data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReadingReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // Hoặc AUTO, SEQUENCE
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private MeterReadingAssignment assignment;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
```

### Các annotation quan trọng:

- `@Entity`: Đánh dấu class là entity
- `@Table(name = "...", schema = "..."): Map với bảng trong DB
- `@Id` + `@GeneratedValue`: Primary key
- `@Column(name = "...", nullable = true/false, length = ...)`: Map với cột
- `@ManyToOne`, `@OneToMany`, `@OneToOne`: Quan hệ giữa entities
- `@JoinColumn`: Foreign key column

---

## Bước 3: Cấu hình Schema tự động tạo

Nếu cần tạo schema tự động:

```properties
# Tự động tạo schema nếu chưa có
spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true
```

Hoặc trong code:

```java
@Configuration
public class JpaConfig {
    
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (properties) -> {
            properties.put("hibernate.hbm2ddl.create_namespaces", true);
        };
    }
}
```

---

## Bước 4: Xử lý xung đột với Flyway

### Cách 1: Tắt Flyway hoàn toàn (Code-First thuần)
```properties
spring.flyway.enabled=false
```

### Cách 2: Dùng cả hai (Không khuyến nghị)
```properties
# Flyway chạy trước, Hibernate update sau
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=update
# ⚠️ Có thể gây xung đột!
```

### Cách 3: Profile riêng cho Code-First
Tạo `application-dev.properties`:
```properties
spring.jpa.hibernate.ddl-auto=update
spring.flyway.enabled=false
spring.jpa.show-sql=true
```

Chạy với profile:
```bash
java -jar app.jar --spring.profiles.active=dev
```

---

## Bước 5: Kiểm tra kết quả

1. **Xem log SQL**: Với `show-sql=true`, bạn sẽ thấy:
   ```
   Hibernate: create table data.meter_reading_reminders (
       id uuid not null,
       assignment_id uuid not null,
       ...
   )
   ```

2. **Kiểm tra database**: 
   ```sql
   SELECT table_name 
   FROM information_schema.tables 
   WHERE table_schema = 'data';
   ```

3. **Xem DDL được tạo**: Hibernate sẽ log các câu lệnh CREATE TABLE

---

## So sánh Code-First vs Database-First

| Tiêu chí | Code-First | Database-First (hiện tại) |
|----------|------------|---------------------------|
| **Tốc độ phát triển** | ⚡ Nhanh hơn | 🐌 Chậm hơn (phải viết SQL) |
| **Kiểm soát schema** | ❌ Hạn chế | ✅ Hoàn toàn kiểm soát |
| **Version control** | ❌ Khó track | ✅ Flyway migrations |
| **Production ready** | ❌ Không an toàn | ✅ An toàn |
| **Rollback** | ❌ Không có | ✅ Có với Flyway |
| **Phù hợp** | Dev/Test | Production |

---

## Best Practices khi dùng Code-First

1. **Chỉ dùng cho Development**
   ```properties
   # application-dev.properties
   spring.jpa.hibernate.ddl-auto=update
   spring.flyway.enabled=false
   ```

2. **Production vẫn dùng Database-First**
   ```properties
   # application-prod.properties
   spring.jpa.hibernate.ddl-auto=validate
   spring.flyway.enabled=true
   ```

3. **Backup database trước khi test**
   ```bash
   pg_dump -U postgres qhome_base_db > backup.sql
   ```

4. **Review SQL được tạo**: Kiểm tra log để đảm bảo schema đúng

5. **Dùng `update` thay vì `create`**: Tránh mất dữ liệu

---

## Ví dụ cấu hình đầy đủ cho Development

```properties
######## Database Configuration (Code-First)  #########
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/qhome_base_db?currentSchema=svc,data,public}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.jpa.open-in-view=false

# Code-First: Hibernate tự tạo/sửa schema
spring.jpa.hibernate.ddl-auto=update

# Bật log để debug
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Tự động tạo schema nếu chưa có
spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true

# Tắt Flyway (vì dùng Code-First)
spring.flyway.enabled=false
```

---

## Migration từ Database-First sang Code-First

Nếu muốn chuyển hoàn toàn (KHÔNG khuyến nghị cho production):

1. **Export schema hiện tại từ entities**:
   ```bash
   # Sử dụng Hibernate để generate DDL
   spring.jpa.properties.hibernate.hbm2ddl.auto=update
   # Xem log SQL và lưu lại
   ```

2. **Xóa tất cả Flyway migrations** (hoặc backup)

3. **Chuyển sang Code-First** với cấu hình trên

4. **Test kỹ** trước khi deploy

---

## Kết luận

- ✅ **Code-First tốt cho**: Development nhanh, prototype, test
- ❌ **Code-First KHÔNG tốt cho**: Production, dự án có dữ liệu quan trọng
- 💡 **Khuyến nghị**: Dùng Code-First cho dev, Database-First cho production




