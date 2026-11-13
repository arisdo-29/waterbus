package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.CreateStaffRequest;
import com.hsf302.waterbus.service.AccountService;
import com.hsf302.waterbus.service.BookingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountController {

    private final AccountService accountService;
    private final BookingService bookingService;

    public AdminAccountController(AccountService accountService, BookingService bookingService) {
        this.accountService = accountService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(@RequestParam(name = "keyword", required = false) String keyword,
                       @RequestParam(name = "role", defaultValue = "ALL") String role,
                       @RequestParam(name = "status", defaultValue = "ALL") String status,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        Page<AccountDTO> accountPage = accountService.getAccounts(keyword, role, status, page, 10);
        model.addAttribute("accounts", accountPage.getContent());
        model.addAttribute("page", accountPage);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("currentRole", role != null ? role.trim().toUpperCase() : "ALL");
        model.addAttribute("currentStatus", status != null ? status.trim().toUpperCase() : "ALL");
        model.addAttribute("currentPage", page);
        return "admin/account/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("createStaffRequest")) {
            model.addAttribute("createStaffRequest", new CreateStaffRequest());
        }
        return "admin/account/form";
    }

    @PostMapping("/create")
    public String createStaff(@Valid @ModelAttribute("createStaffRequest") CreateStaffRequest request,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/account/form";
        }

        try {
            AccountDTO staff = accountService.createStaffAccount(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFullName(),
                    request.getPhone()
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo tài khoản nhân viên [" + staff.getEmail() + "] thành công!");
            return "redirect:/admin/accounts";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/account/form";
        }
    }

    @GetMapping("/{id}")
    public String viewDetail(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        if (id == AccountService.SUPER_ADMIN_ID) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản có mã ID: 0");
            return "redirect:/admin/accounts";
        }

        AccountDTO account = accountService.getAccountById(id);
        if (account == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản có mã ID: " + id);
            return "redirect:/admin/accounts";
        }

        List<BookingDTO> bookings = bookingService.getBookingsOfAccount(id);
        model.addAttribute("account", account);
        model.addAttribute("bookings", bookings);
        return "admin/account/detail";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Đường dẫn không hợp lệ hoặc tham số sai định dạng.");
        return "redirect:/admin/accounts";
    }

    @PostMapping("/{id}/change-role")
    public String changeRole(@PathVariable int id,
                             @RequestParam("role") String role,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        AccountDTO currentAdmin = (AccountDTO) session.getAttribute("loginAccount");
        int currentAdminId = (currentAdmin != null && currentAdmin.getId() != null) ? currentAdmin.getId() : -1;

        try {
            AccountDTO updated = accountService.changeRole(id, role, currentAdminId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đổi vai trò tài khoản [" + updated.getEmail() + "] thành " + updated.getRole() + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable int id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        AccountDTO currentAdmin = (AccountDTO) session.getAttribute("loginAccount");
        int currentAdminId = (currentAdmin != null && currentAdmin.getId() != null) ? currentAdmin.getId() : -1;

        try {
            AccountDTO updated = accountService.toggleStatus(id, currentAdminId);
            String statusText = "ACTIVE".equalsIgnoreCase(updated.getStatus()) ? "Mở khóa" : "Khóa";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã " + statusText.toLowerCase() + " tài khoản [" + updated.getEmail() + "] thành công.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

}
