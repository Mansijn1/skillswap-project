package com.example.skillswap.controller;

import com.example.skillswap.model.User;
import com.example.skillswap.model.UserProfile;
import com.example.skillswap.service.CustomUserDetails;
import com.example.skillswap.service.UserService;
import com.example.skillswap.service.SkillService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class UserController {

    private final UserService userService;
    private final SkillService skillService; // ✅ Added SkillService
    private final String uploadDir;

    // ✅ Updated constructor to inject SkillService
    public UserController(UserService userService,
                          SkillService skillService,
                          @Value("${app.upload.dir:./uploads/}") String uploadDir) {
        this.userService = userService;
        this.skillService = skillService;
        this.uploadDir = uploadDir;
    }

    // ✅ Explore/Search
    @GetMapping("/explore")
    public String explorePage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "query", required = false) String query,
            Model model) {

        String currentUsername = userDetails.getUsername();
        List<User> users;

        if (query != null && !query.trim().isEmpty()) {
            users = userService.searchUsersBySkillOrName(currentUsername, query.trim());
            model.addAttribute("query", query.trim());
        } else {
            users = userService.findAllOtherUsers(currentUsername);
        }

        model.addAttribute("users", users);
        return "explore";
    }

    // ✅ Edit Profile Page
    @GetMapping("/user/profile/edit")
    public String editProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam(value = "message", required = false) String message,
                              Model model) {

        User user = userService.getUserWithSkills(userDetails.getUsername());
        model.addAttribute("user", user);

        if ("add_skills".equals(message)) {
            model.addAttribute("warning", "Please add skills to both the 'Teach' and 'Learn' sections to find your mutual matches!");
        }

        // ✅ Fetch all available skills for dropdowns
        List<String> allAvailableSkills = skillService.getAllDistinctSkills();
        model.addAttribute("allAvailableSkills", allAvailableSkills);

        return "edit-profile";
    }

    // ✅ Save Profile Changes
    @PostMapping("/user/profile/save")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam("email") String email,
                                @RequestParam(value = "pronouns", required = false) String pronouns,
                                @RequestParam(value = "location", required = false) String location,
                                @RequestParam(value = "photo", required = false) MultipartFile photo,
                                @RequestParam(value = "skillsToLearn", required = false) String skillsToLearnStr,
                                @RequestParam(value = "skillsToTeach", required = false) String skillsToTeachStr,
                                @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        String username = userDetails.getUsername();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
        }

        profile.setFullName(fullName);
        profile.setEmail(email);
        profile.setPronouns(pronouns);
        profile.setLocation(location);

        // ✅ Handle profile photo upload
        if (photo != null && !photo.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String originalFileName = photo.getOriginalFilename();
            String fileExtension = "";
            int dotIndex = originalFileName != null ? originalFileName.lastIndexOf('.') : -1;
            if (dotIndex > 0) fileExtension = originalFileName.substring(dotIndex);

            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            profile.setPhotoUrl("/uploads/" + uniqueFileName);
        }

        userService.saveOrUpdateProfile(username, profile);

        // ✅ Save Skills
        if (skillsToLearnStr != null && !skillsToLearnStr.isEmpty()) {
            Set<String> skillsToLearn = Arrays.stream(skillsToLearnStr.split(","))
                    .map(String::trim).collect(Collectors.toSet());
            userService.saveSkillsToLearn(username, skillsToLearn);
        }

        if (skillsToTeachStr != null && !skillsToTeachStr.isEmpty()) {
            Set<String> skillsToTeach = Arrays.stream(skillsToTeachStr.split(","))
                    .map(String::trim).collect(Collectors.toSet());
            userService.saveSkillsToTeach(username, skillsToTeach);
        }

        return "redirect:/user/dashboard";
    }
}
