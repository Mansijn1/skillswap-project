package com.example.skillswap.controller;

import com.example.skillswap.model.User;
import com.example.skillswap.service.CustomUserDetails;
import com.example.skillswap.service.ChatService;
import com.example.skillswap.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime; //  Use LocalDateTime
import java.util.*;

@Controller
public class ChatListController {

    private final ChatService chatService;
    private final UserService userService;

    public ChatListController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    @GetMapping("/my-chats")
    public String myChatsPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String currentUsername = userDetails.getUsername();

        // 1. Get list of all chat partners
        List<String> chatPartnersUsernames = chatService.getChatPartners(currentUsername);

        // 2. Get unread message counts for each partner
        Map<String, Integer> unreadCounts = chatService.getUnreadCounts(currentUsername);

        // 3. FIX: Correctly receive the Map<String, LocalDateTime>
        Map<String, LocalDateTime> lastMessageTimestamps = chatService.getLastMessageTimestamps(currentUsername);

        // 4. Fetch User objects for each partner
        List<User> partners = userService.findUsersByUsernames(chatPartnersUsernames);

        // 5. Combine User info, unread count, and timestamp
        List<Map<String, Object>> partnerListWithCounts = new ArrayList<>();

        for (User partner : partners) {
            Map<String, Object> partnerMap = new HashMap<>();
            partnerMap.put("user", partner);

            int count = unreadCounts.getOrDefault(partner.getUsername(), 0);
            partnerMap.put("unreadCount", count);


            LocalDateTime lastTimestamp = lastMessageTimestamps.getOrDefault(partner.getUsername(), LocalDateTime.MIN);
            partnerMap.put("lastMessageTimestamp", lastTimestamp);

            partnerListWithCounts.add(partnerMap);
        }

        // 6. CORE LOGIC: Sort the list
        partnerListWithCounts.sort((map1, map2) -> {

            // .Cast to LocalDateTime
            LocalDateTime t1 = (LocalDateTime) map1.get("lastMessageTimestamp");
            LocalDateTime t2 = (LocalDateTime) map2.get("lastMessageTimestamp");
            int c1 = (int) map1.get("unreadCount");
            int c2 = (int) map2.get("unreadCount");

            // Sub-sort: Prioritize unread chats (Moves UNREAD to the top)
            if (c1 > 0 && c2 == 0) return -1; // map1 (unread) comes before map2 (read)
            if (c1 == 0 && c2 > 0) return 1;  // map2 (unread) comes before map1 (read)

            // Primary Sort: Sort by timestamp (Descending: newest first)
            // t2.compareTo(t1) gives descending order.
            return t2.compareTo(t1);
        });

        // 7. Add combined and sorted list to model
        model.addAttribute("chatPartnersWithCount", partnerListWithCounts);

        return "my-chats";
    }
}