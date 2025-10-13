# Hướng dẫn cài đặt JUnit cho QhomeBase Microservices

## ✅ Đã hoàn thành

### 1. Cấu hình JUnit trong Parent POM
- **File**: `QhomeBase/pom.xml`
- **JUnit 5**: Version 5.10.1
- **Mockito**: Version 5.8.0  
- **AssertJ**: Version 3.25.1
- **TestContainers**: Version 1.19.6
- **Spring Security Test**: Version 6.3.4

### 2. Các Microservices đã được cấu hình JUnit
- ✅ **base-service** - Đã có sẵn JUnit + thêm dependencies bổ sung
- ✅ **iam-service** - Đã thêm JUnit dependencies + Maven plugins
- ✅ **api-gateway** - Đã thêm JUnit dependencies + Maven plugins
- ✅ **asset-maintenance-service** - Đã thêm JUnit dependencies + Maven plugins
- ✅ **customer-interaction-service** - Đã thêm JUnit dependencies + Maven plugins
- ✅ **data-docs-service** - Đã thêm JUnit dependencies + Maven plugins
- ✅ **finance-billing-service** - Đã thêm JUnit dependencies + Maven plugins
- ✅ **services-card-service** - Đã thêm JUnit dependencies + Maven plugins
- ✅ **staff-work-service** - Đã thêm JUnit dependencies + Maven plugins

## 🛠️ Cấu hình Maven Plugins

Mỗi service đã được cấu hình:

### Maven Surefire Plugin
- Chạy Unit Tests (`*Test.java`, `*Tests.java`)
- Loại trừ Integration Tests (`*IntegrationTest.java`)

### Maven Failsafe Plugin  
- Chạy Integration Tests (`*IntegrationTest.java`, `*IT.java`)

### JaCoCo Plugin
- Tạo báo cáo test coverage
- Báo cáo được tạo trong `target/site/jacoco/index.html`

## 🚀 Cách sử dụng

### Chạy tất cả tests
```bash
mvn test
```

### Chạy tests cho service cụ thể
```bash
mvn test -pl base-service
mvn test -pl iam-service
```

### Chạy tests với coverage
```bash
mvn test jacoco:report
```

### Chạy integration tests
```bash
mvn verify
```

### Chạy tests từ IDE
- Click chuột phải vào class test → "Run Tests"
- Hoặc click vào icon ▶️ bên cạnh method `@Test`

## 📝 Các loại test có thể viết

### 1. Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Repository repository;
    
    @InjectMocks
    private Service service;
    
    @Test
    void testMethod() {
        // Test logic
    }
}
```

### 2. Integration Tests
```java
@SpringBootTest
@AutoConfigureWebMvc
class ControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/api/endpoint"))
               .andExpect(status().isOk());
    }
}
```

### 3. Web Layer Tests
```java
@WebMvcTest(Controller.class)
class ControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private Service service;
    
    @Test
    void testEndpoint() throws Exception {
        // Test controller
    }
}
```

## 🔧 Dependencies đã cài đặt

### JUnit 5
- `junit-jupiter` - Core JUnit 5
- `junit-jupiter-engine` - Test engine
- `junit-jupiter-api` - API annotations
- `junit-jupiter-params` - Parameterized tests

### Mockito
- `mockito-core` - Core mocking functionality
- `mockito-junit-jupiter` - JUnit 5 integration

### AssertJ
- `assertj-core` - Fluent assertions

### TestContainers
- `junit-jupiter` - JUnit 5 integration
- `postgresql` - PostgreSQL test containers

### Spring Test
- `spring-boot-starter-test` - Spring Boot test starter
- `spring-security-test` - Security testing support

## 📊 Test Coverage

Sau khi chạy tests, xem báo cáo coverage tại:
- `target/site/jacoco/index.html`

## 🎯 Ví dụ test hiện có

Dự án đã có sẵn các test mẫu:
- `TenantDeletionServiceTest.java` - Unit test với Mockito
- `BuildingDeletionControllerTest.java` - Integration test với MockMvc

## ⚠️ Lưu ý

1. **Spring Security Test version**: Đã sửa từ 3.5.6 (không tồn tại) thành 6.3.4
2. **Dependencies**: Tất cả dependencies được quản lý tập trung trong parent POM
3. **Maven plugins**: Mỗi service đều có cấu hình Maven plugins riêng
4. **Test naming**: Tuân theo convention `*Test.java` cho unit tests, `*IntegrationTest.java` cho integration tests

## 🔄 Cập nhật dependencies

Để cập nhật version JUnit hoặc các dependencies khác:
1. Sửa version trong `QhomeBase/pom.xml` (dependencyManagement section)
2. Chạy `mvn clean install` để download dependencies mới
