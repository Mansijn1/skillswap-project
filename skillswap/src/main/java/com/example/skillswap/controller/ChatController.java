package com.example.skillswap.controller;

import com.example.skillswap.model.ChatMessage;
import com.example.skillswap.model.User;
import com.example.skillswap.service.ChatService;
import com.example.skillswap.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import jakarta.transaction.Transactional; // Keep this import for @Transactional
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator; // Added for completeness, though not strictly used here
import java.util.List;
import java.util.UUID;
import java.nio.file.StandardCopyOption;

@Controller
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;
    private final UserService userService;

    @Value("${app.upload.dir:./uploads/}")
    private String uploadDir;

    // Use property injection for max size (from application.properties or default)
    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSizeString; // Read as String to parse later if needed

    private final long maxFileSize = 10 * 1024 * 1024; // Default to 10MB (as per application.properties)

    public ChatController(SimpMessageSendingOperations messagingTemplate,
                          ChatService chatService,
                          UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.userService = userService;
    }

    // ====================== CORE CHAT PAGE MAPPING ======================
    @GetMapping("/chat")
    public String chatPage(@RequestParam("recipient") String recipient,
                           Authentication authentication,
                           Model model) {
        String currentUser = authentication.getName();

        if (currentUser.equalsIgnoreCase(recipient)) {
            model.addAttribute("error", "You cannot chat with yourself. Choose another user.");
            return "redirect:/my-chats";
        }

        try {
            User recipientUser = userService.getUserWithSkills(recipient);

            model.addAttribute("currentUser", currentUser);
            model.addAttribute("recipient", recipient);
            model.addAttribute("recipientUser", recipientUser);

            // CRITICAL: Mark messages as read when viewing the chat
            chatService.markMessagesAsRead(currentUser, recipient);

            List<ChatMessage> chatHistory = chatService.getChatHistory(currentUser, recipient);
            model.addAttribute("chatHistory", chatHistory);
        } catch (Exception e) {
            model.addAttribute("error", "User '" + recipient + "' not found. Cannot start chat.");
            // Log the exception for debugging
            e.printStackTrace();
            return "redirect:/my-chats";
        }

        return "chat";
    }

    // ====================== FILE UPLOAD LOGIC ======================
    @PostMapping("/chat/upload")
    @Transactional // Ensures file save and message save are atomic
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("recipient") String recipient,
            Authentication authentication) {

        // 1. Authorization and validation
        String senderUsername = authentication.getName();

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }

        // Use the hardcoded/default maxFileSize in bytes for reliable check
        if (file.getSize() > maxFileSize) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File size exceeds limit (10MB).");
        }

        // Security Check for file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..") || !isAllowedFileType(originalFileName)) {
            return ResponseEntity.badRequest().body("Invalid file type or name.");
        }

        // Prevent self-chat
        if (senderUsername.equalsIgnoreCase(recipient)) {
            return ResponseEntity.badRequest().body("You cannot send files to yourself.");
        }

        try {
            // Check if recipient exists
            userService.getUserWithSkills(recipient);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Recipient user not found.");
        }

        try {
            // 2. File Handling
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String fileExtension = getFileExtension(originalFileName);
            String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String fileUrl = "/uploads/" + uniqueFileName;

            // 3. Message Persistence
            ChatMessage fileMessage = new ChatMessage();
            fileMessage.setSenderUsername(senderUsername);
            fileMessage.setRecipientUsername(recipient);
            fileMessage.setContent(fileUrl);
            fileMessage.setType("FILE");
            fileMessage.setTimestamp(LocalDateTime.now());
            chatService.saveMessage(fileMessage);

            // 4. WebSocket Broadcast
            try {
                messagingTemplate.convertAndSendToUser(recipient, "/topic/private", fileMessage);
                messagingTemplate.convertAndSendToUser(senderUsername, "/topic/private", fileMessage);
            } catch (Exception e) {
                System.err.println("WebSocket send failed for file message: " + e.getMessage());
            }

            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed due to server I/O error.");
        }
    }

    // ====================== TEXT MESSAGE LOGIC ======================
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        try {
            String sender = principal.getName();
            chatMessage.setSenderUsername(sender);

            if (sender.equalsIgnoreCase(chatMessage.getRecipientUsername())) {
                return;
            }

            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setType("TEXT");

            // Set message as unread for the recipient (default in model is fine, but good practice to confirm)
            chatMessage.setRead(false);
            chatService.saveMessage(chatMessage);

            // Send to recipient and sender
            messagingTemplate.convertAndSendToUser(chatMessage.getRecipientUsername(), "/topic/private", chatMessage);
            messagingTemplate.convertAndSendToUser(sender, "/topic/private", chatMessage);
        } catch (Exception e) {
            System.err.println("WebSocket message error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ====================== HELPER METHODS ======================
    private boolean isAllowedFileType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return List.of("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "txt").contains(extension);
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        // Return only the extension without the dot (e.g., "jpg" instead of ".jpg")
        return dotIndex > 0 && dotIndex < filename.length() - 1 ? filename.substring(dotIndex + 1) : "";
    }
}