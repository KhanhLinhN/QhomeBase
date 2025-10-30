package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.TenantRequestDto;
import com.QhomeBase.baseservice.dto.TenantResponseDto;
import com.QhomeBase.baseservice.dto.TenantUpdateDto;
import com.QhomeBase.baseservice.model.Tenant;
import com.QhomeBase.baseservice.repository.TenantRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private Authentication authentication;
    @Mock private UserPrincipal userPrincipal;

    private TenantService tenantService;
    private TenantRequestDto validTenantRequest;
    private Tenant validTenant;
    private TenantUpdateDto validTenantUpdate;
    private UUID testTenantId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testUserId = UUID.randomUUID();


        LocalValidatorFactoryBean lvfb = new LocalValidatorFactoryBean();
        lvfb.afterPropertiesSet();
        tenantService = new TenantService(tenantRepository, lvfb);

        validTenantRequest = TenantRequestDto.builder()
                .code("TENANT001")
                .name("FPT Tower")
                .contact("+84 123 456 789")
                .email("Qhome@gmail.com")
                .address("123 Hoa Lac, Thach That, Ha Noi")
                .status("ACTIVE")
                .description("best building")
                .build();

        validTenant = Tenant.builder()
                .id(testTenantId)
                .code("TENANT001")
                .name("FPT Tower")
                .contact("+84 123 456 789")
                .email("Qhome@gmail.com")
                .address("123 Hoa Lac, Thach That, Ha Noi")
                .status("ACTIVE")
                .description("best building")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(testUserId.toString())
                .updatedBy(testUserId.toString())
                .isDeleted(false)
                .build();

        validTenantUpdate = new TenantUpdateDto();
        validTenantUpdate.setName("Updated FPT Tower");
        validTenantUpdate.setContact("+84 987 654 321");
        validTenantUpdate.setEmail("updatedQhome@gmail.com");
        validTenantUpdate.setAddress("456 Hoa Lac, Thach That, Ha Noi");
        validTenantUpdate.setStatus("ACTIVE");
        validTenantUpdate.setDescription("Updated description");
    }

    @Test
    void createTenants_WithValidData_ShouldReturnTenantResponse() {

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(tenantRepository.existsByCode(eq("TENANT001"))).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(validTenant);

        TenantResponseDto result = tenantService.createTenants(validTenantRequest, authentication);

        assertNotNull(result);
        assertEquals(validTenant.getId(), result.getId());
        assertEquals(validTenant.getCode(), result.getCode());
        assertEquals(validTenant.getName(), result.getName());
        assertEquals(validTenant.getContact(), result.getContact());
        assertEquals(validTenant.getEmail(), result.getEmail());
        assertEquals(validTenant.getAddress(), result.getAddress());
        assertEquals(validTenant.getStatus(), result.getStatus());
        assertEquals(validTenant.getDescription(), result.getDescription());
        assertEquals(validTenant.getCreatedAt(), result.getCreatedAt());
        assertEquals(validTenant.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(validTenant.getCreatedBy(), result.getCreatedBy());
        assertEquals(validTenant.getUpdatedBy(), result.getUpdatedBy());
        assertEquals(validTenant.isDeleted(), result.isDeleted());

        verify(tenantRepository).existsByCode("TENANT001");
        verify(tenantRepository).save(any(Tenant.class));
        verify(authentication).getPrincipal();
        verify(userPrincipal).uid();
    }

    @Test
    void createTenants_WithEmptyName_ShouldThrow() {
        var req = TenantRequestDto.builder()
                .code("TENANT002")
                .name("")
                .email("Qhome@gmail.com")
                .build();

        assertThrows(ConstraintViolationException.class,
                () -> tenantService.createTenants(req, authentication));
        verifyNoInteractions(tenantRepository, authentication);
    }

    @Test
    void createTenants_MissingCode_ShouldThrow() {
        var reqNull = TenantRequestDto.builder()
                .code(null)
                .name("FPT Tower")
                .email("a@b.com")
                .build();
        assertThrows(ConstraintViolationException.class,
                () -> tenantService.createTenants(reqNull, authentication));
        verifyNoInteractions(tenantRepository, authentication);

        var reqBlank = TenantRequestDto.builder()
                .code(" ")
                .name("FPT Tower")
                .email("a@b.com")
                .build();
        assertThrows(ConstraintViolationException.class,
                () -> tenantService.createTenants(reqBlank, authentication));
        verifyNoInteractions(tenantRepository, authentication);

        var reqEmpty = TenantRequestDto.builder()
                .code("")
                .name("FPT Tower")
                .email("a@b.com")
                .build();
        assertThrows(ConstraintViolationException.class,
                () -> tenantService.createTenants(reqEmpty, authentication));
        verifyNoInteractions(tenantRepository, authentication);

        var reqWhitespace = TenantRequestDto.builder()
                .code("\t\n")
                .name("FPT Tower")
                .email("a@b.com")
                .build();
        assertThrows(ConstraintViolationException.class,
                () -> tenantService.createTenants(reqWhitespace, authentication));
        verifyNoInteractions(tenantRepository, authentication);
    }

    @Test
    void createTenants_NameTooShortOrTooLong_ShouldThrow_and_BoundariesPass() {
        var invalidNames = java.util.List.of(
                "A",
                "A".repeat(101)

        );
        for (String badName : invalidNames) {
            var req = TenantRequestDto.builder()
                    .code("TEN-BAD-" + badName.length())
                    .name(badName)
                    .email("a@b.com")
                    .build();

            assertThrows(ConstraintViolationException.class,
                    () -> tenantService.createTenants(req, authentication));
            verifyNoInteractions(tenantRepository, authentication);
        }

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(UUID.randomUUID());

        var validNames = java.util.List.of(
                "AB",
                "A".repeat(100)

        );
        int i = 0;
        for (String goodName : validNames) {
            String code = "TEN-OK-NAME-" + (++i);
            var req = TenantRequestDto.builder()
                    .code(code)
                    .name(goodName)
                    .email("a@b.com")
                    .build();
            when(tenantRepository.existsByCode(eq(code))).thenReturn(false);
            when(tenantRepository.save(any(Tenant.class))).thenReturn(validTenant);
            assertDoesNotThrow(() -> tenantService.createTenants(req, authentication));
            verify(tenantRepository).existsByCode(code);
            verify(tenantRepository).save(any(Tenant.class));
            reset(tenantRepository);
        }
    }


    @Test
    void createTenants_CodeWrongPattern_ShouldThrow() {
        var badCodes = java.util.List.of(
                "TENANT 002",
                "TENANT@002",
                "TENANT#1",
                "has.dots",
                "has/slash",
                "việt nam",
                "TEN ANT",
                "TENANT-OK!"
        );

        for (String bad : badCodes) {
            var req = TenantRequestDto.builder()
                    .code(bad)
                    .name("FPT Tower")
                    .email("a@b.com")
                    .build();

            assertThrows(ConstraintViolationException.class,
                    () -> tenantService.createTenants(req, authentication));
            verifyNoInteractions(tenantRepository, authentication);
        }
    }


    @Test
    void createTenants_EmailInvalid_ShouldThrow() {
        var badEmails = java.util.List.of(
                "not-an-email",
                "a@b",
                "a@@b.com",
                "a..b@c.com",
                "a b@c.com",
                "@no-local.com",
                "local@",
                "local@domain..com"
        );

        for (String email : badEmails) {
            var req = TenantRequestDto.builder()
                    .code("TEN-E-" + Math.abs(email.hashCode()))
                    .name("FPT Tower")
                    .email(email)
                    .build();

            assertThrows(ConstraintViolationException.class,
                    () -> tenantService.createTenants(req, authentication));
            verifyNoInteractions(tenantRepository, authentication);
        }
    }


    @Test
    void createTenants_AddressTooLong_ShouldThrow_and_BoundariesPass() {

        var tooLongAddresses = java.util.List.of(
                "A".repeat(501),
                " ".repeat(501),
                "Đ".repeat(501),
                ("Số nhà 1, Phường ABC, Quận XYZ, Thành phố HN, ").repeat(20)
        );

        for (String addr : tooLongAddresses) {
            var req = TenantRequestDto.builder()
                    .code("TEN-ADDR-" + addr.length())
                    .name("FPT Tower")
                    .email("valid@email.com")
                    .address(addr)
                    .build();

            assertThrows(ConstraintViolationException.class,
                    () -> tenantService.createTenants(req, authentication));
            verifyNoInteractions(tenantRepository, authentication);
        }


        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(UUID.randomUUID());

        var okAddresses = java.util.List.of(
                "A",
                "B".repeat(500),
                "123 Hoa Lac, Thach That, Ha Noi",
                "Tầng 10, Tòa F, KĐT XX, Q. YY, TP. HN"
        );

        int i = 0;
        for (String okAddr : okAddresses) {
            String code = "TEN-OK-ADDR-" + (++i);
            var req = TenantRequestDto.builder()
                    .code(code)
                    .name("FPT Tower")
                    .email("valid@email.com")
                    .address(okAddr)
                    .build();

            when(tenantRepository.existsByCode(eq(code))).thenReturn(false);
            when(tenantRepository.save(any(Tenant.class))).thenReturn(validTenant);

            assertDoesNotThrow(() -> tenantService.createTenants(req, authentication));
            verify(tenantRepository).existsByCode(code);
            verify(tenantRepository).save(any(Tenant.class));
            reset(tenantRepository);
        }
    }


    @Test
    void createTenants_InvalidPhoneNumber_ShouldThrow() {
        var badPhones = java.util.List.of(
                "abc-def-ghij",
                "+++++",
                "12345",
                "1".repeat(21),
                "12 34 56 78 90 12 34 56 78 90 12",
                "()--"

        );

        for (String phone : badPhones) {
            var req = TenantRequestDto.builder()
                    .code("TEN-P-" + Math.abs(phone.hashCode()))
                    .name("FPT Tower")
                    .contact(phone)
                    .email("valid@email.com")
                    .build();

            assertThrows(ConstraintViolationException.class,
                    () -> tenantService.createTenants(req, authentication));
            verifyNoInteractions(tenantRepository, authentication);
        }
    }


    @Test
    void createTenants_ValidPhoneNumbers_ShouldPass() {
        // stub auth dùng chung cho các lần pass trong vòng lặp
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(UUID.randomUUID());

        String[] validPhones = {
                "+84 123 456 789",
                "0123456789",
                "+1-555-123-4567",
                "(555) 123-4567",
                "555-123-4567"
        };

        for (int i = 0; i < validPhones.length; i++) {
            String phone = validPhones[i];
            String code = "TENANT" + (1000 + i);

            var req = TenantRequestDto.builder()
                    .code(code)
                    .name("FPT Tower")
                    .contact(phone)
                    .email("valid@email.com")
                    .build();

            when(tenantRepository.existsByCode(eq(code))).thenReturn(false);
            when(tenantRepository.save(any(Tenant.class))).thenReturn(validTenant);

            assertDoesNotThrow(() -> tenantService.createTenants(req, authentication));
            verify(tenantRepository).existsByCode(code);
        }
        verify(tenantRepository, times(validPhones.length)).save(any(Tenant.class));
    }

    @Test
    void createTenants_DuplicateCodeIgnoreCase_ShouldThrow() {
        var dupCodes = java.util.List.of("sunrise", "SUNRISE", "SunRise");

        for (String code : dupCodes) {
            var req = TenantRequestDto.builder()
                    .code(code)
                    .name("Sunrise Apt")
                    .email("a@b.com")
                    .build();

            when(tenantRepository.existsByCode(eq(code))).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> tenantService.createTenants(req, authentication));

            verify(tenantRepository).existsByCode(code);
            verify(tenantRepository, never()).save(any());

            reset(tenantRepository);
        }
    }

    @Test
    void getAllTenants_ShouldReturnAllNonDeletedTenants() {
        // Create test tenants
        Tenant tenant1 = Tenant.builder()
                .id(UUID.randomUUID())
                .code("TENANT001")
                .name("Building A")
                .contact("+84 123 456 789")
                .email("buildingA@example.com")
                .address("Address A")
                .status("ACTIVE")
                .description("Building A description")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(testUserId.toString())
                .updatedBy(testUserId.toString())
                .isDeleted(false)
                .build();

        Tenant tenant2 = Tenant.builder()
                .id(UUID.randomUUID())
                .code("TENANT002")
                .name("Building B")
                .contact("+84 987 654 321")
                .email("buildingB@example.com")
                .address("Address B")
                .status("ACTIVE")
                .description("Building B description")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(testUserId.toString())
                .updatedBy(testUserId.toString())
                .isDeleted(false)
                .build();

        Tenant deletedTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .code("TENANT003")
                .name("Deleted Building")
                .contact("+84 111 222 333")
                .email("deleted@example.com")
                .address("Deleted Address")
                .status("ACTIVE")
                .description("This building is deleted")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(testUserId.toString())
                .updatedBy(testUserId.toString())
                .isDeleted(true)
                .build();

        List<Tenant> allTenants = Arrays.asList(tenant1, tenant2, deletedTenant);

        when(tenantRepository.findAll()).thenReturn(allTenants);

        List<TenantResponseDto> result = tenantService.getAllTenants();

        assertNotNull(result);
        assertEquals(2, result.size()); // Only non-deleted tenants should be returned

        // Verify the first tenant
        TenantResponseDto resultTenant1 = result.get(0);
        assertEquals(tenant1.getId(), resultTenant1.getId());
        assertEquals(tenant1.getCode(), resultTenant1.getCode());
        assertEquals(tenant1.getName(), resultTenant1.getName());
        assertEquals(tenant1.getContact(), resultTenant1.getContact());
        assertEquals(tenant1.getEmail(), resultTenant1.getEmail());
        assertEquals(tenant1.getAddress(), resultTenant1.getAddress());
        assertEquals(tenant1.getStatus(), resultTenant1.getStatus());
        assertEquals(tenant1.getDescription(), resultTenant1.getDescription());
        assertEquals(tenant1.getCreatedAt(), resultTenant1.getCreatedAt());
        assertEquals(tenant1.getUpdatedAt(), resultTenant1.getUpdatedAt());
        assertEquals(tenant1.getCreatedBy(), resultTenant1.getCreatedBy());
        assertEquals(tenant1.getUpdatedBy(), resultTenant1.getUpdatedBy());
        assertEquals(tenant1.isDeleted(), resultTenant1.isDeleted());

        // Verify the second tenant
        TenantResponseDto resultTenant2 = result.get(1);
        assertEquals(tenant2.getId(), resultTenant2.getId());
        assertEquals(tenant2.getCode(), resultTenant2.getCode());
        assertEquals(tenant2.getName(), resultTenant2.getName());

        // Verify that deleted tenant is not included
        boolean deletedTenantFound = result.stream()
                .anyMatch(t -> t.getCode().equals("TENANT003"));
        assertFalse(deletedTenantFound, "Deleted tenant should not be included in results");

        verify(tenantRepository).findAll();
    }

}
