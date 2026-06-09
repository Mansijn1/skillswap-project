package com.example.skillswap.controller;

import com.example.skillswap.service.UserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ✅ Login page (Spring Security handles the POST)
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ✅ Signup page
    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("signupRequest", new SignupRequest());
        return "signup";
    }

    // ✅ Register new user
    @PostMapping("/register")
    public String register(@ModelAttribute("signupRequest") SignupRequest req, Model model) {
        try {
            userService.registerUser(req.getUsername(), req.getPassword());
            model.addAttribute("msg", "Signup successful. Please login.");
            return "login"; // show login page
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
    }

    // DTO for signup
    public static class SignupRequest {
        @NotBlank private String username;
        @NotBlank private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}

