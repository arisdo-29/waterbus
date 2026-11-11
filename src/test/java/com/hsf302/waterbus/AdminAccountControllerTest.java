package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.entity.Account;
import com.hsf302.waterbus.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AdminAccountControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AccountRepository accountRepository;

    private MockMvc mvc;
    private Account adminAccount;
    private Account staffAccount;
    private Account customerAccount;
    private AccountDTO adminDTO;
    private AccountDTO staffDTO;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        adminAccount = new Account();
        adminAccount.setEmail("admin_test@waterbus.vn");
        adminAccount.setFullName("Admin Hệ Thống");
        adminAccount.setPasswordHash(encoder.encode("AdminPass123!"));
        adminAccount.setRole("ADMIN");
        adminAccount.setStatus("ACTIVE");
        adminAccount.setCreatedAt(LocalDateTime.now());
        adminAccount = accountRepository.save(adminAccount);

        staffAccount = new Account();
        staffAccount.setEmail("staff_test@waterbus.vn");
        staffAccount.setFullName("Nhân Viên Test");
        staffAccount.setPasswordHash(encoder.encode("StaffPass123!"));
        staffAccount.setRole("STAFF");
        staffAccount.setStatus("ACTIVE");
        staffAccount.setCreatedAt(LocalDateTime.now());
        staffAccount = accountRepository.save(staffAccount);

        customerAccount = new Account();
        customerAccount.setEmail("customer_test@waterbus.vn");
        customerAccount.setFullName("Khách Hàng Test");
        customerAccount.setPhone("0901234567");
        customerAccount.setPasswordHash(encoder.encode("CustPass123!"));
        customerAccount.setRole("CUSTOMER");
        customerAccount.setStatus("ACTIVE");
        customerAccount.setCreatedAt(LocalDateTime.now());
        customerAccount = accountRepository.save(customerAccount);

        adminDTO = new AccountDTO(adminAccount.getId(), adminAccount.getEmail(), adminAccount.getFullName(), adminAccount.getPhone(), "ADMIN", "ACTIVE");
        staffDTO = new AccountDTO(staffAccount.getId(), staffAccount.getEmail(), staffAccount.getFullName(), staffAccount.getPhone(), "STAFF", "ACTIVE");
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /admin/accounts -> chuyển hướng đăng nhập")
    void testList_Unauthenticated_Redirects() throws Exception {
        mvc.perform(get("/admin/accounts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=unauthorized"));
    }

    @Test
    @DisplayName("Vai trò STAFF truy cập /admin/accounts -> bị chặn quyền")
    void testList_StaffRole_RedirectsForbidden() throws Exception {
        mvc.perform(get("/admin/accounts").sessionAttr("loginAccount", staffDTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/checkin?error=forbidden"));
    }

    @Test
    @DisplayName("Vai trò ADMIN truy cập /admin/accounts -> hiển thị danh sách thành công")
    void testList_AdminRole_Success() throws Exception {
        mvc.perform(get("/admin/accounts").sessionAttr("loginAccount", adminDTO))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/list"))
                .andExpect(model().attributeExists("accounts", "page"));
    }

    @Test
    @DisplayName("Mở form tạo nhân viên -> hiển thị view form")
    void testShowCreateForm_AdminRole() throws Exception {
        mvc.perform(get("/admin/accounts/create").sessionAttr("loginAccount", adminDTO))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/form"))
                .andExpect(model().attributeExists("createStaffRequest"));
    }

    @Test
    @DisplayName("Tạo tài khoản STAFF mới thành công")
    void testCreateStaff_Success() throws Exception {
        mvc.perform(post("/admin/accounts/create")
                        .sessionAttr("loginAccount", adminDTO)
                        .param("email", "new_staff@waterbus.vn")
                        .param("fullName", "Nhân Viên Mới")
                        .param("phone", "0909999888")
                        .param("password", "StaffPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("successMessage"));

        Account created = accountRepository.findByEmailIgnoreCase("new_staff@waterbus.vn").orElse(null);
        assertNotNull(created);
        assertEquals("STAFF", created.getRole());
        assertEquals("ACTIVE", created.getStatus());
        assertEquals("Nhân Viên Mới", created.getFullName());
    }

    @Test
    @DisplayName("Đổi vai trò tài khoản: CUSTOMER -> STAFF")
    void testChangeRole_CustomerToStaff() throws Exception {
        mvc.perform(post("/admin/accounts/" + customerAccount.getId() + "/change-role")
                        .sessionAttr("loginAccount", adminDTO)
                        .param("role", "STAFF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("successMessage"));

        Account updated = accountRepository.findById(customerAccount.getId()).orElseThrow();
        assertEquals("STAFF", updated.getRole());
    }

    @Test
    @DisplayName("Không được tự đổi vai trò của chính mình")
    void testChangeRole_Self_Fails() throws Exception {
        mvc.perform(post("/admin/accounts/" + adminAccount.getId() + "/change-role")
                        .sessionAttr("loginAccount", adminDTO)
                        .param("role", "STAFF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Khóa và mở khóa tài khoản")
    void testToggleStatus_Success() throws Exception {
        // 1. Khóa
        mvc.perform(post("/admin/accounts/" + customerAccount.getId() + "/toggle-status")
                        .sessionAttr("loginAccount", adminDTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("successMessage"));

        Account locked = accountRepository.findById(customerAccount.getId()).orElseThrow();
        assertEquals("LOCKED", locked.getStatus());

        // 2. Mở khóa
        mvc.perform(post("/admin/accounts/" + customerAccount.getId() + "/toggle-status")
                        .sessionAttr("loginAccount", adminDTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("successMessage"));

        Account unlocked = accountRepository.findById(customerAccount.getId()).orElseThrow();
        assertEquals("ACTIVE", unlocked.getStatus());
    }

    @Test
    @DisplayName("Không được tự khóa tài khoản của chính mình")
    void testToggleStatus_Self_Fails() throws Exception {
        mvc.perform(post("/admin/accounts/" + adminAccount.getId() + "/toggle-status")
                        .sessionAttr("loginAccount", adminDTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("errorMessage"));

        Account stillActive = accountRepository.findById(adminAccount.getId()).orElseThrow();
        assertEquals("ACTIVE", stillActive.getStatus());
    }

    @Test
    @DisplayName("Đặt lại mật khẩu cho tài khoản")
    void testResetPassword_Success() throws Exception {
        mvc.perform(post("/admin/accounts/" + customerAccount.getId() + "/reset-password")
                        .sessionAttr("loginAccount", adminDTO)
                        .param("newPassword", "NewPassword789!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("successMessage"));

        Account updated = accountRepository.findById(customerAccount.getId()).orElseThrow();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches("NewPassword789!", updated.getPasswordHash()));
    }

    @Test
    @DisplayName("Xem chi tiết tài khoản và lịch sử đặt vé")
    void testViewDetail_Success() throws Exception {
        mvc.perform(get("/admin/accounts/" + customerAccount.getId())
                        .sessionAttr("loginAccount", adminDTO))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/detail"))
                .andExpect(model().attributeExists("account", "bookings"));
    }

    @Test
    @DisplayName("Gõ URL xem chi tiết Super Admin ID 0 -> báo không tìm thấy và chuyển hướng")
    void testViewDetail_SuperAdminId0_RedirectsWithError() throws Exception {
        mvc.perform(get("/admin/accounts/0")
                        .sessionAttr("loginAccount", adminDTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attribute("errorMessage", "Không tìm thấy tài khoản có mã ID: 0"));
    }

    @Test
    @DisplayName("Gõ URL xem chi tiết với ID sai định dạng -> chuyển hướng và báo lỗi thân thiện")
    void testViewDetail_InvalidIdFormat_RedirectsWithError() throws Exception {
        mvc.perform(get("/admin/accounts/abc")
                        .sessionAttr("loginAccount", adminDTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
