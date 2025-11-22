# 🚀 Hướng dẫn chi tiết: Tạo Spring Boot Project, API, Deploy và Call API

## 📋 Mục lục
1. [Tạo Spring Boot Project](#1-tạo-spring-boot-project)
2. [Tạo API (RESTful)](#2-tạo-api-restful)
3. [Deploy lên Server](#3-deploy-lên-server)
4. [Call API để chạy](#4-call-api-để-chạy)

---

## 1. Tạo Spring Boot Project

### Cách 1: Dùng Spring Initializr (Khuyến nghị cho người mới)

1. **Truy cập**: https://start.spring.io/

2. **Cấu hình project**:
   - **Project**: Maven (hoặc Gradle)
   - **Language**: Java
   - **Spring Boot**: 3.5.6 (hoặc latest)
   - **Group**: `com.yourcompany` (VD: `com.QhomeBase`)
   - **Artifact**: `your-service` (VD: `base-service`)
   - **Packaging**: Jar
   - **Java**: 17 (hoặc 21)

3. **Chọn Dependencies** (bấm "Add Dependencies"):
   ```
   - Spring Web (REST APIs)
   - Spring Data JPA (Database)
   - PostgreSQL Driver (Database driver)
   - Lombok (Code generation)
   - Spring Boot Actuator (Monitoring)
   - Spring Doc OpenAPI (Swagger UI)
   - Spring Security (Authentication)
   - Flyway Migration (Database migrations)
   ```

4. **Generate và Download** → Giải nén vào thư mục làm việc

### Cách 2: Tạo bằng Maven (Như dự án hiện tại)

#### Bước 1: Tạo cấu trúc thư mục

```bash
mkdir my-spring-boot-service
cd my-spring-boot-service
mkdir -p src/main/java/com/mycompany/myservice
mkdir -p src/main/resources
mkdir -p src/test/java/com/mycompany/myservice
```

#### Bước 2: Tạo `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>
    
    <groupId>com.mycompany</groupId>
    <artifactId>my-spring-boot-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>My Spring Boot Service</name>
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### Bước 3: Tạo Main Application Class

**File**: `src/main/java/com/mycompany/myservice/MyServiceApplication.java`

```java
package com.mycompany.myservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyServiceApplication.class, args);
    }
}
```

#### Bước 4: Tạo `application.properties`

**File**: `src/main/resources/application.properties`

```properties
# Server Configuration
server.port=8080
spring.application.name=my-service

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

#### Bước 5: Build và chạy

```bash
# Build
mvn clean install

# Chạy
mvn spring-boot:run
# hoặc
java -jar target/my-spring-boot-service-1.0.0-SNAPSHOT.jar
```

---

## 2. Tạo API (RESTful)

Dựa trên dự án hiện tại, quy trình tạo API gồm các bước:

### 📐 Kiến trúc Layer

```
Controller (API Endpoints)
    ↓
Service (Business Logic)
    ↓
Repository (Data Access)
    ↓
Model/Entity (Database Tables)
```

### Bước 1: Tạo Model (Entity)

**File**: `src/main/java/com/mycompany/myservice/model/Product.java`

```java
package com.mycompany.myservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "price", nullable = false)
    private Double price;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
```

### Bước 2: Tạo Repository (Data Access Layer)

**File**: `src/main/java/com/mycompany/myservice/repository/ProductRepository.java`

```java
package com.mycompany.myservice.repository;

import com.mycompany.myservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    // Spring Data JPA tự động tạo query từ method name
    List<Product> findByNameContainingIgnoreCase(String name);
    
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
    
    Optional<Product> findById(UUID id);
    
    // Custom query với JPQL
    @Query("SELECT p FROM Product p WHERE p.price > :minPrice ORDER BY p.price DESC")
    List<Product> findExpensiveProducts(@Param("minPrice") Double minPrice);
    
    // Native SQL query
    @Query(value = "SELECT * FROM products WHERE price > :price", nativeQuery = true)
    List<Product> findByPriceGreaterThan(@Param("price") Double price);
}
```

### Bước 3: Tạo DTO (Data Transfer Object)

**File**: `src/main/java/com/mycompany/myservice/dto/ProductDto.java`

```java
package com.mycompany.myservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private UUID id;
    private String name;
    private Double price;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
```

### Bước 4: Tạo Service (Business Logic Layer)

**File**: `src/main/java/com/mycompany/myservice/service/ProductService.java`

```java
package com.mycompany.myservice.service;

import com.mycompany.myservice.dto.ProductDto;
import com.mycompany.myservice.model.Product;
import com.mycompany.myservice.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    
    // Lấy tất cả products
    @Transactional
    public List<ProductDto> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    // Lấy product theo ID
    @Transactional
    public ProductDto getProductById(UUID id) {
        log.info("Fetching product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return toDto(product);
    }
    
    // Tạo product mới
    @Transactional
    public ProductDto createProduct(ProductDto productDto) {
        log.info("Creating new product: {}", productDto.getName());
        
        Product product = Product.builder()
                .name(productDto.getName())
                .price(productDto.getPrice())
                .description(productDto.getDescription())
                .build();
        
        Product savedProduct = productRepository.save(product);
        return toDto(savedProduct);
    }
    
    // Cập nhật product
    @Transactional
    public ProductDto updateProduct(UUID id, ProductDto productDto) {
        log.info("Updating product with id: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        product.setName(productDto.getName());
        product.setPrice(productDto.getPrice());
        product.setDescription(productDto.getDescription());
        
        Product updatedProduct = productRepository.save(product);
        return toDto(updatedProduct);
    }
    
    // Xóa product
    @Transactional
    public void deleteProduct(UUID id) {
        log.info("Deleting product with id: {}", id);
        
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        
        productRepository.deleteById(id);
    }
    
    // Tìm kiếm products theo tên
    @Transactional
    public List<ProductDto> searchProducts(String name) {
        log.info("Searching products with name: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    // Convert Entity → DTO
    private ProductDto toDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
```

### Bước 5: Tạo Controller (API Endpoints)

**File**: `src/main/java/com/mycompany/myservice/controller/ProductController.java`

```java
package com.mycompany.myservice.controller;

import com.mycompany.myservice.dto.ProductDto;
import com.mycompany.myservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    /**
     * GET /api/products
     * Lấy tất cả products
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET /api/products/{id}
     * Lấy product theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable UUID id) {
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    /**
     * POST /api/products
     * Tạo product mới
     */
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto createdProduct = productService.createProduct(productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }
    
    /**
     * PUT /api/products/{id}
     * Cập nhật product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductDto productDto) {
        ProductDto updatedProduct = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updatedProduct);
    }
    
    /**
     * DELETE /api/products/{id}
     * Xóa product
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * GET /api/products/search?name=...
     * Tìm kiếm products theo tên
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> searchProducts(@RequestParam String name) {
        List<ProductDto> products = productService.searchProducts(name);
        return ResponseEntity.ok(products);
    }
}
```

### Bước 6: Tạo Exception Handler (Optional nhưng nên có)

**File**: `src/main/java/com/mycompany/myservice/exception/GlobalExceptionHandler.java`

```java
package com.mycompany.myservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("timestamp", OffsetDateTime.now());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", "Internal server error");
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("timestamp", OffsetDateTime.now());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### Bước 7: Test API trên Local

```bash
# Chạy ứng dụng
mvn spring-boot:run

# Test với cURL hoặc Postman
curl http://localhost:8080/api/products

# Tạo product mới
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "price": 15000000,
    "description": "High performance laptop"
  }'
```

---

## 3. Deploy lên Server

### Cách 1: Deploy với Docker (Khuyến nghị)

#### Bước 1: Tạo Dockerfile

**File**: `Dockerfile` (ở root của project)

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml và download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code và build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM openjdk:17-jre-slim
WORKDIR /app

# Copy JAR từ stage 1
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Bước 2: Build Docker Image

```bash
# Build image
docker build -t my-spring-boot-service:latest .

# Kiểm tra image đã tạo
docker images | grep my-spring-boot-service
```

#### Bước 3: Chạy Container

```bash
# Chạy container với database
docker run -d \
  --name my-service \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/mydb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  my-spring-boot-service:latest

# Xem logs
docker logs -f my-service

# Kiểm tra container đang chạy
docker ps
```

#### Bước 4: Dùng Docker Compose (Khuyến nghị cho production)

**File**: `docker-compose.yml`

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: my-postgres
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  my-service:
    build: .
    container_name: my-spring-boot-service
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mydb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

volumes:
  postgres-data:
```

**Chạy Docker Compose**:

```bash
# Build và chạy tất cả services
docker-compose up -d

# Xem logs
docker-compose logs -f my-service

# Dừng services
docker-compose down
```

### Cách 2: Deploy JAR trực tiếp lên Server

#### Bước 1: Build JAR file

```bash
# Build JAR
mvn clean package -DskipTests

# JAR sẽ được tạo ở: target/my-spring-boot-service-1.0.0-SNAPSHOT.jar
```

#### Bước 2: Upload JAR lên Server

```bash
# SCP lên server
scp target/my-spring-boot-service-1.0.0-SNAPSHOT.jar user@server:/opt/my-service/

# SSH vào server
ssh user@server
```

#### Bước 3: Chạy JAR trên Server

```bash
# Cách 1: Chạy trực tiếp
java -jar /opt/my-service/my-spring-boot-service-1.0.0-SNAPSHOT.jar

# Cách 2: Chạy background với nohup
nohup java -jar /opt/my-service/my-spring-boot-service-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  > /var/log/my-service.log 2>&1 &

# Cách 3: Dùng systemd service (Khuyến nghị)
```

#### Bước 4: Tạo Systemd Service (Khuyến nghị)

**File**: `/etc/systemd/system/my-service.service`

```ini
[Unit]
Description=My Spring Boot Service
After=network.target postgresql.service

[Service]
Type=simple
User=myuser
WorkingDirectory=/opt/my-service
ExecStart=/usr/bin/java -jar /opt/my-service/my-spring-boot-service-1.0.0-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Quản lý service**:

```bash
# Reload systemd
sudo systemctl daemon-reload

# Start service
sudo systemctl start my-service

# Enable auto-start on boot
sudo systemctl enable my-service

# Check status
sudo systemctl status my-service

# View logs
sudo journalctl -u my-service -f
```

### Cách 3: Deploy lên Cloud (AWS, Azure, GCP)

#### AWS (Elastic Beanstalk hoặc EC2)

```bash
# 1. Build JAR
mvn clean package

# 2. Upload lên S3 hoặc Elastic Beanstalk
aws s3 cp target/my-service.jar s3://my-bucket/
```

#### Google Cloud Platform (Cloud Run)

```bash
# 1. Build với Cloud Build
gcloud builds submit --tag gcr.io/my-project/my-service

# 2. Deploy lên Cloud Run
gcloud run deploy my-service \
  --image gcr.io/my-project/my-service \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

---

## 4. Call API để chạy

### Cách 1: Dùng cURL (Command Line)

```bash
# GET - Lấy tất cả products
curl http://localhost:8080/api/products

# GET - Lấy product theo ID
curl http://localhost:8080/api/products/123e4567-e89b-12d3-a456-426614174000

# POST - Tạo product mới
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15",
    "price": 25000000,
    "description": "Latest iPhone model"
  }'

# PUT - Cập nhật product
curl -X PUT http://localhost:8080/api/products/123e4567-e89b-12d3-a456-426614174000 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "price": 30000000,
    "description": "Updated description"
  }'

# DELETE - Xóa product
curl -X DELETE http://localhost:8080/api/products/123e4567-e89b-12d3-a456-426614174000

# GET - Tìm kiếm
curl "http://localhost:8080/api/products/search?name=iPhone"
```

### Cách 2: Dùng Postman

1. **Tạo Request**:
   - Method: GET, POST, PUT, DELETE
   - URL: `http://localhost:8080/api/products`
   - Headers: `Content-Type: application/json`
   - Body (cho POST/PUT): JSON format

2. **Ví dụ POST request trong Postman**:
   ```
   POST http://localhost:8080/api/products
   Headers:
     Content-Type: application/json
   
   Body (raw JSON):
   {
     "name": "MacBook Pro",
     "price": 45000000,
     "description": "Apple MacBook Pro 16 inch"
   }
   ```

### Cách 3: Dùng Swagger UI (Tự động có nếu đã cấu hình)

```properties
# Thêm vào application.properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.packages-to-scan=com.mycompany.myservice.controller
```

**Truy cập**: `http://localhost:8080/swagger-ui.html`

### Cách 4: Dùng JavaScript/TypeScript (Frontend)

```typescript
// Fetch API
const response = await fetch('http://localhost:8080/api/products', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json',
  }
});
const products = await response.json();

// POST request
const createProduct = async (product: ProductDto) => {
  const response = await fetch('http://localhost:8080/api/products', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(product)
  });
  return await response.json();
};

// Axios
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  }
});

// GET
const products = await apiClient.get('/products');

// POST
const newProduct = await apiClient.post('/products', {
  name: 'Laptop',
  price: 15000000,
  description: 'High performance laptop'
});
```

### Cách 5: Dùng Python

```python
import requests

base_url = "http://localhost:8080/api/products"

# GET all products
response = requests.get(base_url)
products = response.json()
print(products)

# POST new product
new_product = {
    "name": "Laptop",
    "price": 15000000,
    "description": "High performance laptop"
}
response = requests.post(base_url, json=new_product)
created_product = response.json()
print(created_product)
```

---

## 📊 Tóm tắt quy trình hoàn chỉnh

```
1. TẠO PROJECT
   ├─ Spring Initializr hoặc Maven
   ├─ Cấu hình pom.xml
   └─ Tạo Main Application class

2. TẠO API
   ├─ Model (Entity) → Database mapping
   ├─ Repository → Data access
   ├─ Service → Business logic
   ├─ Controller → API endpoints
   └─ DTO → Data transfer

3. DEPLOY
   ├─ Build JAR: mvn clean package
   ├─ Docker: docker build & run
   └─ Deploy: Cloud hoặc Server

4. CALL API
   ├─ cURL
   ├─ Postman
   ├─ Swagger UI
   ├─ Frontend (JavaScript/TypeScript)
   └─ Python/Other languages
```

---

## 🔍 Checklist trước khi Deploy

- [ ] ✅ Test API trên local thành công
- [ ] ✅ Build JAR thành công (`mvn clean package`)
- [ ] ✅ Cấu hình database connection đúng
- [ ] ✅ Cấu hình environment variables
- [ ] ✅ Logging được cấu hình
- [ ] ✅ Error handling đầy đủ
- [ ] ✅ Security (nếu cần)
- [ ] ✅ Health check endpoint (Actuator)
- [ ] ✅ Backup database (nếu có dữ liệu)
- [ ] ✅ Document API (Swagger)

---

## 📚 Resources

- Spring Boot Docs: https://spring.io/projects/spring-boot
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Docker: https://docs.docker.com/
- Postman: https://www.postman.com/

---

**Chúc bạn thành công! 🎉**




