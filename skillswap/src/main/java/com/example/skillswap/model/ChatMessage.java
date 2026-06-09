package com.example.skillswap.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;  // Change import

@Entity
@Table(name = "chat_messages",
        indexes = {
        // Index 1: Fast searching for (User A to User B) messages
        @Index(name = "idx_sender_recipient_ts", columnList = "senderUsername, recipientUsername, timestamp"),
        // Index 2: Fast searching for (User B to User A) messages
        @Index(name = "idx_recipient_sender_ts", columnList = "recipientUsername, senderUsername, timestamp")
})
@Getter
@Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderUsername;
    private String recipientUsername;
    private String content;
    private String type;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(name = "timestamp")  // No @Temporal needed for LocalDateTime (Hibernate 5.2+ auto-handles)
    private LocalDateTime timestamp;  // Changed to LocalDateTime
}
