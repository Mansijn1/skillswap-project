package com.example.skillswap.service;

import com.example.skillswap.model.ChatMessage;
import com.example.skillswap.repository.ChatRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    // Save message with timestamp
    public ChatMessage saveMessage(ChatMessage message) {
        // Ensure isRead is false for new messages
        message.setRead(false);
        message.setTimestamp(LocalDateTime.now());
        return chatRepository.save(message);
    }

    // Get chat history between two users (sorted by time)
    // (Existing method - no change)
    public List<ChatMessage> getChatHistory(String username1, String username2) {
        List<ChatMessage> history1 = chatRepository.findBySenderUsernameAndRecipientUsernameOrderByTimestampAsc(username1, username2);
        List<ChatMessage> history2 = chatRepository.findByRecipientUsernameAndSenderUsernameOrderByTimestampAsc(username1, username2);

        history1.addAll(history2);
        history1.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));
        return history1;
    }

    // Get all unique chat partners of a user
    // (Existing method - no change)
    public List<String> getChatPartners(String currentUsername) {
        List<String> chatPartners = new ArrayList<>();
        List<ChatMessage> sentMessages = chatRepository.findBySenderUsernameOrderByTimestampAsc(currentUsername);
        List<ChatMessage> receivedMessages = chatRepository.findByRecipientUsernameOrderByTimestampAsc(currentUsername);

        for (ChatMessage message : sentMessages) {
            if (!chatPartners.contains(message.getRecipientUsername())) {
                chatPartners.add(message.getRecipientUsername());
            }
        }

        for (ChatMessage message : receivedMessages) {
            if (!chatPartners.contains(message.getSenderUsername())) {
                chatPartners.add(message.getSenderUsername());
            }
        }
        return chatPartners;
    }


    @Transactional // Needed for modifying entity state
    public void markMessagesAsRead(String recipientUsername, String senderUsername) {
        // 1. Fetch all unread messages sent by 'senderUsername' to 'recipientUsername'
        List<ChatMessage> unreadMessages = chatRepository.findByRecipientUsernameAndSenderUsernameAndIsReadFalse(recipientUsername, senderUsername);

        // 2. Mark them as read
        for (ChatMessage message : unreadMessages) {
            message.setRead(true);
        }
        // Save is good practice for explicit flushing
        chatRepository.saveAll(unreadMessages);
    }

    /**
     * ✅ REVISED: Get the unread message count for the current user, grouped by sender.
     * This method now relies on the 'isRead' field in ChatMessage.
     */
    public Map<String, Integer> getUnreadCounts(String currentUsername) {
        Map<String, Integer> unreadCounts = new HashMap<>();

        // Fetch all UNREAD messages where the current user is the recipient
        List<ChatMessage> unreadReceived = chatRepository.findByRecipientUsernameAndIsReadFalse(currentUsername);

        for (ChatMessage message : unreadReceived) {
            String sender = message.getSenderUsername();
            unreadCounts.put(sender, unreadCounts.getOrDefault(sender, 0) + 1);
        }

        return unreadCounts;
    }

    /**
     * ✨ NEW METHOD: Gets the timestamp of the very last message in each conversation.
     * This is crucial for sorting the chat list by activity.
     * NOTE: This returns LocalDateTime, which must be handled in the Controller.
     */
    public Map<String, LocalDateTime> getLastMessageTimestamps(String currentUsername) {
        Map<String, LocalDateTime> timestamps = new HashMap<>();
        List<String> partners = getChatPartners(currentUsername);

        for (String partnerUsername : partners) {
            // Find the most recent message between the current user and the partner
            Optional<ChatMessage> latestMessage = chatRepository
                    .findTopBySenderUsernameAndRecipientUsernameOrRecipientUsernameAndSenderUsernameOrderByTimestampDesc(
                            currentUsername, partnerUsername,
                            currentUsername, partnerUsername
                    );

            latestMessage.ifPresent(message ->
                    timestamps.put(partnerUsername, message.getTimestamp())
            );
        }

        return timestamps;
    }
}