package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.RegisterRequest;
import com.hsf302.waterbus.dto.UpdateProfileRequest;
import com.hsf302.waterbus.entity.Account;
import com.hsf302.waterbus.repository.AccountRepository;
import com.hsf302.waterbus.service.impl.AccountServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private AccountService accountService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        accountRepository = Mockito.mock(AccountRepository.class);
        accountService = new AccountServiceImpl(accountRepository);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void testRegister_Success() {
        when(accountRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            acc.setId(1);
            return acc;
        });

        AccountDTO result = accountService.register("test@example.com", "Password@123", "Nguyen Van B", "0901234567");

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("CUSTOMER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testRegister_WithRequest_Success() {
        when(accountRepository.existsByEmailIgnoreCase("req@example.com")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            acc.setId(2);
            return acc;
        });

        com.hsf302.waterbus.dto.RegisterRequest req = new com.hsf302.waterbus.dto.RegisterRequest();
        req.setEmail("req@example.com");
        req.setPassword("Password@123");
        req.setFullName("Nguyen Van Req");
        req.setPhone("0901234568");

        AccountDTO result = accountService.register(req);

        assertNotNull(result);
        assertEquals(2, result.getId());
        assertEquals("req@example.com", result.getEmail());
        assertEquals("CUSTOMER", result.getRole());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testRegister_DuplicateEmail_ThrowsException() {
        when(accountRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                accountService.register("dup@example.com", "123456", "Test", "0900000000")
        );
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testLogin_Success() {
        Account account = new Account();
        account.setId(1);
        account.setEmail("user@example.com");
        account.setPasswordHash(passwordEncoder.encode("Secret@123"));
        account.setStatus("ACTIVE");
        account.setRole("CUSTOMER");

        when(accountRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(account));

        AccountDTO loggedIn = accountService.login("user@example.com", "Secret@123");
        assertNotNull(loggedIn);
        assertEquals(1, loggedIn.getId());
    }

    @Test
    void testLogin_WrongPassword() {
        Account account = new Account();
        account.setId(1);
        account.setEmail("user@example.com");
        account.setPasswordHash(passwordEncoder.encode("Secret@123"));
        account.setStatus("ACTIVE");

        when(accountRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(account));

        AccountDTO loggedIn = accountService.login("user@example.com", "WrongPassword");
        assertNull(loggedIn);
    }

    @Test
    void testLogin_LockedAccount() {
        Account account = new Account();
        account.setId(1);
        account.setEmail("locked@example.com");
        account.setPasswordHash(passwordEncoder.encode("Secret@123"));
        account.setStatus("LOCKED");

        when(accountRepository.findByEmailIgnoreCase("locked@example.com")).thenReturn(Optional.of(account));

        AccountDTO loggedIn = accountService.login("locked@example.com", "Secret@123");
        assertNull(loggedIn);
    }

    @Test
    void testAccountDTOConversion() {
        Account account = new Account();
        account.setId(10);
        account.setEmail("test@gmail.com");
        account.setFullName("Nguyen Van Test");
        account.setPhone("0912345678");
        account.setRole("STAFF");
        account.setStatus("ACTIVE");

        AccountDTO directDto = AccountDTO.fromEntity(account);
        assertNotNull(directDto);
        assertEquals(10, directDto.getId());
        assertEquals("test@gmail.com", directDto.getEmail());
        assertEquals("Nguyen Van Test", directDto.getFullName());
        assertEquals("STAFF", directDto.getRole());

        Account convertedBack = directDto.toEntity();
        assertNotNull(convertedBack);
        assertEquals(account.getId(), convertedBack.getId());
        assertEquals(account.getEmail(), convertedBack.getEmail());
    }

    @Test
    void testIsLocked() {
        Account locked = new Account();
        locked.setId(1);
        locked.setStatus("LOCKED");

        Account active = new Account();
        active.setId(2);
        active.setStatus("ACTIVE");

        when(accountRepository.findById(1)).thenReturn(Optional.of(locked));
        when(accountRepository.findById(2)).thenReturn(Optional.of(active));
        when(accountRepository.findById(99)).thenReturn(Optional.empty());

        assertTrue(accountService.isLocked(1));
        assertFalse(accountService.isLocked(2));
        assertFalse(accountService.isLocked(99));
    }

    @Test
    void testUpdateProfile_Success() {
        Account account = new Account();
        account.setId(5);
        account.setEmail("user5@gmail.com");
        account.setFullName("Old Name");
        account.setPhone("0900000005");
        account.setRole("CUSTOMER");
        account.setStatus("ACTIVE");

        when(accountRepository.findById(5)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        AccountDTO updated = accountService.updateProfile(5, "New Name", "0988888888");

        assertNotNull(updated);
        assertEquals("New Name", updated.getFullName());
        assertEquals("0988888888", updated.getPhone());
    }

    @Test
    void testChangePassword_Success() {
        Account account = new Account();
        account.setId(5);
        account.setPasswordHash(passwordEncoder.encode("OldPass@123"));

        when(accountRepository.findById(5)).thenReturn(Optional.of(account));

        accountService.changePassword(5, "OldPass@123", "NewPass@456");

        assertTrue(passwordEncoder.matches("NewPass@456", account.getPasswordHash()));
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void testChangePassword_WrongCurrentPassword() {
        Account account = new Account();
        account.setId(5);
        account.setPasswordHash(passwordEncoder.encode("OldPass@123"));

        when(accountRepository.findById(5)).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () ->
                accountService.changePassword(5, "WrongPass", "NewPass@456")
        );
        verify(accountRepository, never()).save(account);
    }

    @Test
    void testChangePassword_SameAsCurrentPassword_ThrowsException() {
        Account account = new Account();
        account.setId(5);
        account.setPasswordHash(passwordEncoder.encode("SamePass@123"));

        when(accountRepository.findById(5)).thenReturn(Optional.of(account));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                accountService.changePassword(5, "SamePass@123", "SamePass@123")
        );
        assertTrue(ex.getMessage().contains("Mật khẩu mới phải khác mật khẩu hiện tại."));
        verify(accountRepository, never()).save(account);
    }

    @Test
    void testCreateStaffAccount_Success() {
        when(accountRepository.existsByEmailIgnoreCase("staff@waterbus.vn")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> {
            Account a = i.getArgument(0);
            a.setId(10);
            return a;
        });

        AccountDTO result = accountService.createStaffAccount("staff@waterbus.vn", "Staff@123", "Nhan Vien 1", "0901234567");

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("staff@waterbus.vn", result.getEmail());
        assertEquals("STAFF", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("Nhan Vien 1", result.getFullName());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testCreateStaffAccount_DuplicateEmail() {
        when(accountRepository.existsByEmailIgnoreCase("staff@waterbus.vn")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                accountService.createStaffAccount("staff@waterbus.vn", "Staff@123", "Nhan Vien 1", "0901234567")
        );
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testChangeRole_CustomerToStaff_Success() {
        Account user = new Account();
        user.setId(2);
        user.setEmail("user@gmail.com");
        user.setRole("CUSTOMER");

        when(accountRepository.findById(2)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        AccountDTO updated = accountService.changeRole(2, "STAFF", 1);

        assertNotNull(updated);
        assertEquals("STAFF", updated.getRole());
        assertEquals("STAFF", user.getRole());
        verify(accountRepository, times(1)).save(user);
    }

    @Test
    void testChangeRole_StaffToCustomer_Success() {
        Account staff = new Account();
        staff.setId(2);
        staff.setEmail("staff@waterbus.vn");
        staff.setRole("STAFF");

        when(accountRepository.findById(2)).thenReturn(Optional.of(staff));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        AccountDTO updated = accountService.changeRole(2, "CUSTOMER", 1);

        assertNotNull(updated);
        assertEquals("CUSTOMER", updated.getRole());
        assertEquals("CUSTOMER", staff.getRole());
    }

    @Test
    void testChangeRole_Self_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                accountService.changeRole(1, "CUSTOMER", 1)
        );
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testChangeRole_TargetAdmin_ThrowsException() {
        Account admin = new Account();
        admin.setId(2);
        admin.setRole("ADMIN");
        when(accountRepository.findById(2)).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class, () ->
                accountService.changeRole(2, "STAFF", 1)
        );
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testChangeRole_PromoteToAdmin_ThrowsException() {
        Account user = new Account();
        user.setId(2);
        user.setRole("CUSTOMER");
        when(accountRepository.findById(2)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () ->
                accountService.changeRole(2, "ADMIN", 1)
        );
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testToggleStatus_LockAndUnlock() {
        Account user = new Account();
        user.setId(2);
        user.setRole("CUSTOMER");
        user.setStatus("ACTIVE");

        when(accountRepository.findById(2)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        AccountDTO locked = accountService.toggleStatus(2, 1);
        assertEquals("LOCKED", locked.getStatus());

        AccountDTO unlocked = accountService.toggleStatus(2, 1);
        assertEquals("ACTIVE", unlocked.getStatus());
    }

    @Test
    void testToggleStatus_Self_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                accountService.toggleStatus(1, 1)
        );
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testToggleStatus_Admin_ThrowsException() {
        Account admin = new Account();
        admin.setId(2);
        admin.setRole("ADMIN");
        when(accountRepository.findById(2)).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class, () ->
                accountService.toggleStatus(2, 1)
        );
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testResetPassword_Success() {
        Account user = new Account();
        user.setId(2);
        user.setPasswordHash(passwordEncoder.encode("OldPassword123"));
        when(accountRepository.findById(2)).thenReturn(Optional.of(user));

        accountService.resetPassword(2, "NewPassword999");

        assertTrue(passwordEncoder.matches("NewPassword999", user.getPasswordHash()));
        verify(accountRepository, times(1)).save(user);
    }

    @Test
    void testPhoneValidation_SameAsCheckoutRegex() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        RegisterRequest reqValid = new RegisterRequest("test@test.com", "123456", "Test", "0901234567");
        assertTrue(validator.validate(reqValid).isEmpty());

        RegisterRequest reqNull = new RegisterRequest("test@test.com", "123456", "Test", null);
        assertTrue(validator.validate(reqNull).isEmpty());
        RegisterRequest reqEmpty = new RegisterRequest("test@test.com", "123456", "Test", "   ");
        assertTrue(validator.validate(reqEmpty).isEmpty());

        RegisterRequest reqInvalid1 = new RegisterRequest("test@test.com", "123456", "Test", "090123456");
        assertFalse(validator.validate(reqInvalid1).isEmpty());

        RegisterRequest reqInvalid2 = new RegisterRequest("test@test.com", "123456", "Test", "09012345678");
        assertFalse(validator.validate(reqInvalid2).isEmpty());

        RegisterRequest reqInvalid3 = new RegisterRequest("test@test.com", "123456", "Test", "+84901234567");
        assertFalse(validator.validate(reqInvalid3).isEmpty());

        RegisterRequest reqInvalid4 = new RegisterRequest("test@test.com", "123456", "Test", "1234567890");
        assertFalse(validator.validate(reqInvalid4).isEmpty());

        UpdateProfileRequest profileValid = new UpdateProfileRequest("Test", "0912345678");
        assertTrue(validator.validate(profileValid).isEmpty());

        UpdateProfileRequest profileInvalid = new UpdateProfileRequest("Test", "091234");
        assertFalse(validator.validate(profileInvalid).isEmpty());
    }

    @Test
    void testSuperAdmin_CannotBeModified_ThrowsException() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                accountService.changeRole(AccountService.SUPER_ADMIN_ID, "STAFF", 1)
        );
        assertTrue(ex1.getMessage().contains("Super Admin"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                accountService.toggleStatus(AccountService.SUPER_ADMIN_ID, 1)
        );
        assertTrue(ex2.getMessage().contains("Super Admin"));

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                accountService.resetPassword(AccountService.SUPER_ADMIN_ID, "Password123")
        );
        assertTrue(ex3.getMessage().contains("Super Admin"));
    }

    @Test
    void testSuperAdmin_CanManageOtherAdminAccounts() {
        Account otherAdmin = new Account();
        otherAdmin.setId(2);
        otherAdmin.setEmail("admin2@waterbus.vn");
        otherAdmin.setRole("ADMIN");
        otherAdmin.setStatus("LOCKED");

        when(accountRepository.findById(2)).thenReturn(Optional.of(otherAdmin));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(IllegalArgumentException.class, () ->
                accountService.toggleStatus(2, 1)
        );

        AccountDTO unlocked = accountService.toggleStatus(2, AccountService.SUPER_ADMIN_ID);
        assertNotNull(unlocked);
        assertEquals("ACTIVE", unlocked.getStatus());

        AccountDTO roleChanged = accountService.changeRole(2, "STAFF", AccountService.SUPER_ADMIN_ID);
        assertNotNull(roleChanged);
        assertEquals("STAFF", roleChanged.getRole());
    }
}
