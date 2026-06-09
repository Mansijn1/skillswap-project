package com.example.skillswap.controller;

import com.example.skillswap.model.ChatMessage;
import com.example.skillswap.model.User;
import com.example.skillswap.service.ChatService;
import com.example.skillswap.service.CustomUserDetails;
import com.example.skillswap.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class DashboardController {

    private final UserService userService;
    private final ChatService chatService; // New dependency

    public DashboardController(UserService userService, ChatService chatService) {
        this.userService = userService;
        this.chatService = chatService;
    }

    @GetMapping("/user/dashboard")
    public String userDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // Fetch user with skills initialized
        User user = userService.getUserWithSkills(userDetails.getUsername());

        model.addAttribute("user", user); // now Thymeleaf can access skills safely
        return "user-dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("username", userDetails.getUsername());
        return "admin-dashboard";
    }

//    @GetMapping("/explore")
//    public String explorePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
//        List<User> users = userService.findAllOtherUsers(userDetails.getUsername());
//        model.addAttribute("users", users);
//        return "explore";
//    }


}