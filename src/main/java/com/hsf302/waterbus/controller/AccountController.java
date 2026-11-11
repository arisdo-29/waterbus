package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.ChangePasswordRequest;
import com.hsf302.waterbus.dto.UpdateProfileRequest;
import com.hsf302.waterbus.service.AccountService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        if (loginAccount == null) {
            session.setAttribute("redirectAfterLogin", "/account/profile");
            return "redirect:/auth/login";
        }

        if (!model.containsAttribute("updateProfileRequest")) {
            model.addAttribute("updateProfileRequest", new UpdateProfileRequest(
                    loginAccount.getFullName(),
                    loginAccount.getPhone()
            ));
        }
        model.addAttribute("account", loginAccount);
        return "account/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("updateProfileRequest") UpdateProfileRequest request,
                                BindingResult bindingResult,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        if (loginAccount == null) {
            session.setAttribute("redirectAfterLogin", "/account/profile");
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("account", loginAccount);
            return "account/profile";
        }

        String trimmedName = request.getFullName() == null ? "" : request.getFullName().trim();
        String trimmedPhone = request.getPhone() == null ? "" : request.getPhone().trim();
        String currentName = loginAccount.getFullName() == null ? "" : loginAccount.getFullName().trim();
        String currentPhone = loginAccount.getPhone() == null ? "" : loginAccount.getPhone().trim();

        if (trimmedName.equals(currentName) && trimmedPhone.equals(currentPhone)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa thay đổi thông tin nào.");
            return "redirect:/account/profile";
        }

        try {
            AccountDTO updated = accountService.updateProfile(
                    loginAccount.getId(),
                    request.getFullName(),
                    request.getPhone()
            );
            session.setAttribute("loginAccount", updated);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
            return "redirect:/account/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("account", loginAccount);
            return "account/profile";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(HttpSession session, Model model) {
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        if (loginAccount == null) {
            session.setAttribute("redirectAfterLogin", "/account/change-password");
            return "redirect:/auth/login";
        }

        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
        return "account/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        if (loginAccount == null) {
            return "redirect:/auth/login";
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không khớp.");
        }

        if (bindingResult.hasErrors()) {
            return "account/change-password";
        }

        try {
            accountService.changePassword(
                    loginAccount.getId(),
                    request.getCurrentPassword(),
                    request.getNewPassword()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
            return "redirect:/account/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "account/change-password";
        }
    }
}
