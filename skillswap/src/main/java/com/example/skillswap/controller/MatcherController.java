package com.example.skillswap.controller;

import com.example.skillswap.dto.MatchDetails;
import com.example.skillswap.model.User;
import com.example.skillswap.service.CustomUserDetails;
import com.example.skillswap.service.UserService;
import com.example.skillswap.service.SkillService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
public class MatcherController {

    private final UserService userService;
    private final SkillService skillService;

    public MatcherController(UserService userService, SkillService skillService) {
        this.userService = userService;
        this.skillService = skillService;
    }

    @GetMapping("/user/matchers")
    public String showMutualMatches(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "location", required = false) String location, // ⭐ ACCEPTS LOCATION PARAM
            Model model) {

        String currentUsername = userDetails.getUsername();

        // ⭐ CALLS THE NEW LOCATION-FILTERED SERVICE METHOD
        List<MatchDetails> mutualMatches = userService.findFilteredMatchesByLocation(currentUsername, location);

        // Fetch user data for feedback
        User currentUser = userService.getUserWithSkills(currentUsername);

        model.addAttribute("matches", mutualMatches);
        model.addAttribute("location", location); // Passes location back to persist the filter in the input field

        // Feedback message logic (updated to acknowledge the filter)
        if (mutualMatches.isEmpty()) {
            if (currentUser.getSkillsToLearn().isEmpty() || currentUser.getSkillsToTeach().isEmpty()) {
                model.addAttribute("message", "🚫 Profile incomplete! Add both teaching and learning skills to find matches.");
                model.addAttribute("showProfileLink", true);
            } else {
                // Custom message if filtering results in zero matches
                if(location != null && !location.isEmpty()){
                    model.addAttribute("message", "😔 No mutual matches found in '" + location + "'. Try removing the filter!");
                } else {
                    model.addAttribute("message", "😔 No mutual matches found yet. Check your profile skills!");
                }
            }
        }

        return "matchers";
    }
}
