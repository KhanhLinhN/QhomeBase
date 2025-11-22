# 🔐 Hướng dẫn chi tiết: JWT Authentication trong hệ thống

## 📋 Tổng quan

Hệ thống sử dụng **JWT (JSON Web Token)** để xác thực và phân quyền giữa các microservices. JWT được tạo bởi **IAM Service** khi user đăng nhập, và được verify bởi các microservices khác qua **FilterChain**.

---

## 🔄 Quy trình hoàn chỉnh

### **Bước 1: Đăng nhập → IAM Service tạo JWT**

#### 1.1. User gửi request đăng nhập

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

#### 1.2. IAM Service xác thực và tạo JWT

**File**: `iam-service/src/main/java/com/QhomeBase/iamservice/service/AuthService.java`

```java
@Transactional
public LoginResponseDto login(LoginRequestDto loginRequestDto) {
    // 1. Tìm user trong database
    User user = userRepository.findByUsername(loginRequestDto.username())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    
    // 2. Verify password
    boolean passwordMatches = passwordEncoder.matches(...);
    
    // 3. Lấy roles và permissions của user
    List<String> roleNames = userRoles.stream()
            .map(UserRole::getRoleName)
            .collect(Collectors.toList());
    
    List<String> userPermissions = getUserPermissions(userRoles);
    
    // 4. Tạo JWT token
    String accessToken = jwtIssuer.issueForService(
            user.getId(),           // uid
            user.getUsername(),     // username
            null,                   // tenantId
            roleNames,              // roles: ["ADMIN", "STAFF", ...]
            userPermissions,        // permissions: ["READ_USERS", "WRITE_USERS", ...]
            "base-service,finance-service,customer-service,asset-maintenance-service,iam-service"  // audiences
    );
    
    return new LoginResponseDto(accessToken, "Bearer", 3600L, ...);
}
```

#### 1.3. JWT được tạo với cấu trúc

**File**: `iam-service/src/main/java/com/QhomeBase/iamservice/security/JwtIssuer.java`

```java
public String issueForService(UUID uid, String username, UUID tenantId,
                              List<String> roles, List<String> perms, String audiences) {
    var builder = Jwts.builder();
    
    // HEADER (tự động tạo)
    // {
    //   "alg": "HS256",
    //   "typ": "JWT"
    // }
    
    // PAYLOAD (claims)
    builder.setIssuer(issuer)                    // iss: "qhome-iam"
            .setSubject(username)                 // sub: "admin"
            .setId(UUID.randomUUID().toString())  // jti: "unique-token-id"
            .setIssuedAt(Date.from(Instant.now())) // iat: timestamp
            .setExpiration(...)                   // exp: timestamp
            .setAudience(audiences)               // aud: "base-service,finance-service,..."
            .claim("uid", uid.toString())         // uid: "user-uuid"
            .claim("roles", new ArrayList<>(roles))  // roles: ["ADMIN", "STAFF"]
            .claim("perms", new ArrayList<>(perms)); // perms: ["READ_USERS", "WRITE_USERS"]
    
    // SIGNATURE
    // Được tạo bằng: HMAC-SHA256(
    //   base64UrlEncode(header) + "." + base64UrlEncode(payload),
    //   secret_key
    // )
    return builder.signWith(key, SignatureAlgorithm.HS256).compact();
}
```

#### 1.4. Cấu hình JWT Secret Key

**File**: `iam-service/src/main/resources/application.properties`

```properties
# Secret key để ký JWT (phải >= 32 bytes)
security.jwt.secret=qhome-iam-secret-key-2024-very-long-and-secure-key-for-jwt-token-generation
security.jwt.issuer=qhome-iam
security.jwt.accessTtlMinutes=60
```

**Secret key được convert thành HMAC-SHA256 key:**
```java
byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
if (raw.length < 32) throw new IllegalStateException("JWT_SECRET must be >= 32 bytes");
this.key = Keys.hmacShaKeyFor(raw);  // Tạo HMAC-SHA256 signing key
```

---

### **Bước 2: JWT Token Structure**

JWT gồm 3 phần, ngăn cách bởi dấu chấm (`.`):

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIkFETUlOIl0sInBlcm1zIjpbIkJFQVNFLVJFQUQiXX0.signature
```

#### **Phần 1: Header (Base64 URL-encoded)**

```json
{
  "alg": "HS256",    // Algorithm: HMAC SHA256
  "typ": "JWT"       // Type: JSON Web Token
}
```

#### **Phần 2: Payload (Claims) - Base64 URL-encoded**

```json
{
  "iss": "qhome-iam",              // Issuer: Ai tạo token
  "sub": "admin",                  // Subject: Username
  "jti": "unique-token-id",        // JWT ID: Unique token identifier
  "iat": 1704067200,               // Issued At: Thời gian tạo
  "exp": 1704070800,               // Expiration: Thời gian hết hạn (60 phút)
  "aud": "base-service,finance-service,customer-service,asset-maintenance-service,iam-service",  // Audience: Các service được phép dùng token này
  "uid": "123e4567-e89b-12d3-a456-426614174000",  // User ID
  "roles": ["ADMIN", "STAFF"],     // Danh sách roles
  "perms": ["READ_USERS", "WRITE_USERS", "BASE_READ"]  // Danh sách permissions
}
```

#### **Phần 3: Signature**

```
HMAC-SHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```

**Chữ ký này đảm bảo:**
- Token không bị giả mạo
- Token không bị sửa đổi
- Chỉ IAM Service (có secret key) mới tạo được token hợp lệ

---

### **Bước 3: Client gửi JWT trong Request**

Client lưu JWT và gửi trong header mỗi request:

```http
GET /api/products
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIkFETUlOIl0sInBlcm1zIjpbIkJFQVNFLVJFQUQiXX0.signature
```

---

### **Bước 4: FilterChain xác thực JWT**

#### 4.1. Security Filter Chain Configuration

**File**: `base-service/src/main/java/com/QhomeBase/baseservice/security/SecurityConfig.java`

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
            .cors(...)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .anyRequest().authenticated()  // Tất cả request khác cần authenticate
            )
            // JwtAuthFilter chạy TRƯỚC UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
```

**Thứ tự Filter Chain:**
```
1. CORS Filter
2. JwtAuthFilter ← Xác thực JWT ở đây
3. UsernamePasswordAuthenticationFilter
4. Authorization Filter
5. Controller
```

#### 4.2. JwtAuthFilter - Xác thực JWT

**File**: `base-service/src/main/java/com/QhomeBase/baseservice/security/JwtAuthFilter.java`

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    
    private final JwtVerifier jwtVerifier;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        // 1. Lấy token từ Authorization header
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                // 2. Extract token (bỏ "Bearer ")
                String token = auth.substring(7);
                
                // 3. Verify JWT (kiểm tra signature, issuer, audience, expiration)
                Claims claims = jwtVerifier.verify(token);
                
                // 4. Extract thông tin từ claims
                UUID uid = UUID.fromString(claims.get("uid", String.class));
                String username = claims.getSubject();  // sub claim
                List<String> roles = claims.get("roles", List.class);
                List<String> perms = claims.get("perms", List.class);
                
                // 5. Tạo authorities từ roles và permissions
                var authorities = new ArrayList<SimpleGrantedAuthority>();
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));  // ROLE_ADMIN
                }
                for (String perm : perms) {
                    authorities.add(new SimpleGrantedAuthority("PERM_" + perm));  // PERM_BASE_READ
                }
                
                // 6. Tạo UserPrincipal và Authentication object
                var principal = new UserPrincipal(uid, username, roles, perms, token);
                var authn = new UsernamePasswordAuthenticationToken(
                    principal,      // Principal: UserPrincipal object
                    null,           // Credentials: null (đã verify rồi)
                    authorities     // Authorities: Danh sách roles và permissions
                );
                
                // 7. Lưu vào SecurityContext để Spring Security sử dụng
                SecurityContextHolder.getContext().setAuthentication(authn);
                
            } catch (Exception e) {
                // JWT invalid → Trả về 401 Unauthorized
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        
        // 8. Tiếp tục filter chain
        filterChain.doFilter(request, response);
    }
}
```

#### 4.3. JwtVerifier - Verify JWT Signature và Claims

**File**: `base-service/src/main/java/com/QhomeBase/baseservice/security/JwtVerifier.java`

```java
@Component
public class JwtVerifier {
    private final SecretKey key;              // Secret key để verify signature
    private final String issuer;              // Issuer phải là "qhome-iam"
    private final String expectedAudience;    // Audience phải chứa tên microservice (VD: "base-service")
    
    public Claims verify(String token) {
        // 1. Parse và verify JWT
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)                    // Secret key để verify signature
                .requireIssuer(issuer)                 // Kiểm tra issuer = "qhome-iam"
                .setAllowedClockSkewSeconds(Duration.ofMinutes(5).getSeconds())  // Cho phép sai lệch 5 phút
                .build()
                .parseClaimsJws(token)                 // Parse token
                .getBody();                            // Lấy payload (claims)
        
        // 2. Verify signature
        // JWT library tự động verify signature khi parse
        // Nếu signature không hợp lệ → throw exception
        
        // 3. Verify expiration
        // JWT library tự động kiểm tra exp claim
        // Nếu token hết hạn → throw exception
        
        // 4. Verify audience (tên microservice)
        if (!isAudienceValid(claims)) {
            throw new SecurityException(
                "JWT audience does not include " + expectedAudience
            );
        }
        
        return claims;
    }
    
    private boolean isAudienceValid(Claims claims) {
        Object audClaim = claims.get("aud");
        
        // Audience có thể là String hoặc List<String>
        // VD: "base-service,finance-service" hoặc ["base-service", "finance-service"]
        
        if (audClaim instanceof String) {
            String audString = (String) audClaim;
            
            // Check exact match
            if (expectedAudience.equals(audString)) {
                return true;
            }
            
            // Check comma-separated audiences
            if (audString.contains(",")) {
                String[] audiences = audString.split(",");
                for (String aud : audiences) {
                    if (expectedAudience.equals(aud.trim())) {
                        return true;  // Tên microservice này có trong danh sách audience
                    }
                }
            }
        }
        
        return false;
    }
}
```

#### 4.4. Cấu hình JWT cho Base Service

**File**: `base-service/src/main/resources/application.properties`

```properties
# JWT Configuration
security.jwt.secret=qhome-iam-secret-key-2024-very-long-and-secure-key-for-jwt-token-generation
security.jwt.issuer=qhome-iam
security.jwt.audience=base-service  # Tên microservice này

# ⚠️ QUAN TRỌNG: Secret key phải GIỐNG với IAM Service
# Nếu khác → Signature verification sẽ fail
```

**Signature được xác định như thế nào?**

1. **IAM Service tạo JWT:**
   - Dùng secret key: `qhome-iam-secret-key-2024...`
   - Ký bằng HMAC-SHA256: `signWith(key, SignatureAlgorithm.HS256)`

2. **Base Service verify JWT:**
   - Dùng CÙNG secret key: `qhome-iam-secret-key-2024...`
   - Verify signature: `setSigningKey(key)`
   - Nếu signature hợp lệ → Token đúng do IAM Service tạo

3. **Vì sao phải verify issuer và audience?**
   - **Issuer (`iss`)**: Đảm bảo token được tạo bởi `qhome-iam` (không phải service khác)
   - **Audience (`aud`)**: Đảm bảo token này được phép dùng cho microservice hiện tại (VD: `base-service`)

---

### **Bước 5: Controller sử dụng Authentication**

Sau khi JWT được verify, Controller có thể lấy thông tin user:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")  // Yêu cầu đã authenticate
    public ResponseEntity<List<Product>> getAllProducts(Authentication authentication) {
        // Lấy UserPrincipal từ Authentication
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        
        UUID userId = principal.uid();           // User ID
        String username = principal.username();  // Username
        List<String> roles = principal.roles();  // Roles
        List<String> perms = principal.perms();  // Permissions
        
        // Logic xử lý...
        return ResponseEntity.ok(products);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // Yêu cầu role ADMIN
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        // Chỉ ADMIN mới được tạo product
        return ResponseEntity.ok(savedProduct);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BASE_WRITE')")  // Yêu cầu permission
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        // Chỉ user có permission BASE_WRITE mới được xóa
        return ResponseEntity.noContent().build();
    }
}
```

---

## 🔍 Chi tiết về Signature Verification

### **Làm sao Signature xác định được tên microservice?**

**Signature KHÔNG xác định tên microservice trực tiếp**, nhưng:

1. **Signature verify secret key:**
   ```java
   // IAM Service tạo signature với secret key
   signature = HMAC-SHA256(header + "." + payload, secret_key)
   
   // Base Service verify với CÙNG secret key
   if (HMAC-SHA256(header + "." + payload, secret_key) == signature) {
       // ✅ Token hợp lệ - Được tạo bởi IAM Service
   }
   ```

2. **Audience xác định tên microservice:**
   ```java
   // Payload chứa audience
   {
     "aud": "base-service,finance-service,customer-service,..."
   }
   
   // Base Service kiểm tra
   if (audience.contains("base-service")) {
       // ✅ Token này được phép dùng cho base-service
   }
   ```

3. **Kết hợp:**
   - **Signature verify** → Token được tạo bởi IAM Service (có secret key)
   - **Audience verify** → Token được phép dùng cho microservice này
   - **Issuer verify** → Token được tạo bởi `qhome-iam`

---

## 📊 Flow Diagram

```
┌─────────────┐
│   Client    │
│  (Frontend) │
└──────┬──────┘
       │
       │ 1. POST /api/auth/login
       │    { username, password }
       ▼
┌─────────────────┐
│  IAM Service    │
│                 │
│ 1. Verify user  │
│ 2. Get roles    │
│ 3. Get perms    │
│ 4. Create JWT   │
│                 │
│ JwtIssuer:      │
│ - Header        │
│ - Payload:      │
│   • uid         │
│   • username    │
│   • roles       │
│   • perms       │
│   • aud         │
│ - Signature     │
│   (HMAC-SHA256) │
└──────┬──────────┘
       │
       │ 2. Response: { accessToken, ... }
       ▼
┌─────────────┐
│   Client    │
│  (Frontend) │
└──────┬──────┘
       │
       │ 3. GET /api/products
       │    Authorization: Bearer <JWT>
       ▼
┌──────────────────┐
│  Base Service    │
│                  │
│ FilterChain:     │
│ 1. CORS          │
│ 2. JwtAuthFilter │ ← Verify JWT
│    │             │
│    ├─ JwtVerifier│
│    │  • Check    │
│    │    signature│
│    │  • Check    │
│    │    issuer   │
│    │  • Check    │
│    │    audience │
│    │  • Check    │
│    │    expiration│
│    │             │
│    ├─ Extract    │
│    │  claims     │
│    │             │
│    └─ Set        │
│       Security-  │
│       Context    │
│                  │
│ 3. Controller    │
│    │             │
│    └─ Use        │
│       Authentication│
└──────────────────┘
```

---

## 🔑 Key Points

### **1. Signature = Tên Microservice?**

**KHÔNG!** Signature xác định:
- ✅ Token được tạo bởi IAM Service (có secret key)
- ✅ Token không bị sửa đổi
- ✅ Token hợp lệ

**Audience (`aud`)** xác định:
- ✅ Token được phép dùng cho microservice nào
- ✅ Mỗi microservice kiểm tra tên mình có trong `aud` không

### **2. Secret Key phải giống nhau**

```
IAM Service secret key = base-service secret key
                       = finance-service secret key
                       = ...
```

Nếu khác → Signature verification fail!

### **3. Quy trình xác thực**

```
1. Extract token từ Authorization header
   ↓
2. Verify signature (bằng secret key)
   ↓
3. Verify issuer (= "qhome-iam")
   ↓
4. Verify audience (chứa tên microservice)
   ↓
5. Verify expiration (chưa hết hạn)
   ↓
6. Extract claims (uid, username, roles, perms)
   ↓
7. Tạo Authentication object
   ↓
8. Set vào SecurityContext
```

---

## 🛠️ Debugging

### **Xem JWT token được tạo:**

1. **Decode JWT online**: https://jwt.io/
2. **Xem trong code**: `JwtAuthFilter` có debug log

```java
System.out.println("=== JWT TOKEN DEBUG ===");
System.out.println("Token: " + token);
System.out.println("=== JWT CLAIMS DEBUG ===");
System.out.println("Issuer: " + claims.getIssuer());
System.out.println("Subject: " + claims.getSubject());
System.out.println("Audience: " + claims.getAudience());
System.out.println("UID: " + claims.get("uid"));
System.out.println("Roles: " + claims.get("roles"));
System.out.println("Perms: " + claims.get("perms"));
```

### **Các lỗi thường gặp:**

1. **Signature verification failed**
   - Secret key không khớp giữa IAM và microservice
   - Token bị sửa đổi

2. **Token expired**
   - Token đã hết hạn (default 60 phút)

3. **Invalid audience**
   - Token không có microservice hiện tại trong `aud`

4. **Invalid issuer**
   - Issuer không phải `qhome-iam`

---

## 📚 Tóm tắt

| Bước | Component | Hành động |
|------|-----------|-----------|
| **1. Đăng nhập** | IAM Service | Tạo JWT với header, payload (username, roles, perms), signature |
| **2. Client** | Frontend | Lưu JWT, gửi trong Authorization header |
| **3. Verify** | JwtAuthFilter | Lấy token từ header |
| **4. Verify** | JwtVerifier | Verify signature (bằng secret key) |
| **5. Verify** | JwtVerifier | Verify issuer (= "qhome-iam") |
| **6. Verify** | JwtVerifier | Verify audience (chứa tên microservice) |
| **7. Extract** | JwtAuthFilter | Lấy uid, username, roles, perms từ claims |
| **8. Authenticate** | JwtAuthFilter | Tạo Authentication object, set vào SecurityContext |
| **9. Authorize** | Controller | Sử dụng `@PreAuthorize` để kiểm tra roles/permissions |

---

**Chúc bạn hiểu rõ! 🎉**




