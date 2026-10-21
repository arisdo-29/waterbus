package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.entity.Account;
import com.hsf302.waterbus.repository.AccountRepository;
import com.hsf302.waterbus.service.impl.AccountServiceImpl;
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

        Account result = accountService.register("test@example.com", "Password@123", "Nguyen Van B", "0901234567");

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("CUSTOMER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        assertTrue(passwordEncoder.matches("Password@123", result.getPasswordHash()));
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

        Account loggedIn = accountService.login("user@example.com", "Secret@123");
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

        Account loggedIn = accountService.login("user@example.com", "WrongPassword");
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

        Account loggedIn = accountService.login("locked@example.com", "Secret@123");
        assertNull(loggedIn);
    }

    @Test
    void testToDTO() {
        Account account = new Account();
        account.setId(10);
        account.setEmail("test@gmail.com");
        account.setFullName("Nguyen Van Test");
        account.setPhone("0912345678");
        account.setRole("STAFF");
        account.setStatus("ACTIVE");

        AccountDTO dto = accountService.toDTO(account);
        assertNotNull(dto);
        assertEquals(10, dto.getId());
        assertEquals("test@gmail.com", dto.getEmail());
        assertEquals("Nguyen Van Test", dto.getFullName());
        assertEquals("STAFF", dto.getRole());
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
}
