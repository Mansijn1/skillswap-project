package com.example.skillswap.repository;

import com.example.skillswap.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional; // ❗ Import Optional

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    // -------------------------------------------------------------------------
    // Existing/Historical Queries
    // -------------------------------------------------------------------------
    List<ChatMessage> findBySenderUsernameAndRecipientUsernameOrderByTimestampAsc(String senderUsername, String recipientUsername);
    List<ChatMessage> findByRecipientUsernameAndSenderUsernameOrderByTimestampAsc(String recipientUsername, String senderUsername);

    List<ChatMessage> findBySenderUsernameOrderByTimestampAsc(String senderUsername);
    List<ChatMessage> findByRecipientUsernameOrderByTimestampAsc(String recipientUsername);

    // Query for marking messages as read
    List<ChatMessage> findByRecipientUsernameAndSenderUsernameAndIsReadFalse(String recipientUsername, String senderUsername);

    // -------------------------------------------------------------------------
    // Query for Chat List Logic (Unread Counts)
    // -------------------------------------------------------------------------

    /**
     * ✅ NEW/UPDATED Query for getUnreadCounts in ChatService.
     * Fetches all unread messages received by the current user.
     */
    List<ChatMessage> findByRecipientUsernameAndIsReadFalse(String recipientUsername);

    // -------------------------------------------------------------------------
    // Query for Chat List Logic (Sorting by Last Message)
    // -------------------------------------------------------------------------

    /**
     * ✨ CRUCIAL NEW QUERY for sorting the chat list.
     * Finds the single most recent message between two users (u1/u2).
     * This covers both directions (u1->u2 or u2->u1).
     * * Example usage in ChatService:
     * findTopBySenderUsernameAndRecipientUsernameOrRecipientUsernameAndSenderUsernameOrderByTimestampDesc(
     * currentUsername, partnerUsername,
     * partnerUsername, currentUsername
     * );
     */
    Optional<ChatMessage> findTopBySenderUsernameAndRecipientUsernameOrRecipientUsernameAndSenderUsernameOrderByTimestampDesc(
            String sender1, String recipient1,
            String sender2, String recipient2);

    // Note: The custom COUNT query below is now redundant if ChatService uses
    // findByRecipientUsernameAndIsReadFalse for counting, but I'll leave it
    // in case you need it elsewhere.
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.recipientUsername = :recipient AND m.senderUsername = :sender AND m.isRead = FALSE")
    int countUnreadMessagesBetween(@Param("recipient") String recipient, @Param("sender") String sender);
}