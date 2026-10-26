package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.LoginRequest;
import com.hsf302.waterbus.dto.RegisterRequest;
import com.hsf302.waterbus.entity.Account;
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

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi. Mọi URL của controller này bắt đầu bằng /auth
// View đặt trong templates/auth/ (giao diện: Tân)
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session) {
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        if (loginAccount != null) {
            if ("STAFF".equalsIgnoreCase(loginAccount.getRole())) {
                return "redirect:/admin/checkin";
            }
            if ("ADMIN".equalsIgnoreCase(loginAccount.getRole())) {
                return "redirect:/admin/trips";
            }
            return "redirect:/";
        }
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                        BindingResult bindingResult,
                        HttpSession session,
                        Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        Account account = accountService.login(loginRequest.getEmail(), loginRequest.getPassword());
        if (account == null) {
            model.addAttribute("errorMessage", "Email hoặc mật khẩu không chính xác hoặc tài khoản đã bị khóa.");
            return "auth/login";
        }

        AccountDTO accountDTO = accountService.toDTO(account);
        session.setAttribute("loginAccount", accountDTO);

        if ("STAFF".equalsIgnoreCase(accountDTO.getRole())) {
            return "redirect:/admin/checkin";
        }
        if ("ADMIN".equalsIgnoreCase(accountDTO.getRole())) {
            return "redirect:/admin/trips";
        }
        return "redirect:/";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, HttpSession session) {
        if (session.getAttribute("loginAccount") != null) {
            return "redirect:/";
        }
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            accountService.register(
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getFullName(),
                    registerRequest.getPhone()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("loginAccount");
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logoutGet(HttpSession session) {
        return logout(session);
    }
}
