package com.example.skillswap.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Added the new field to store the full name
    private String fullName;

    @Email
    private String email;
    private String pronouns;
    private String photoUrl;
    private String location;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}