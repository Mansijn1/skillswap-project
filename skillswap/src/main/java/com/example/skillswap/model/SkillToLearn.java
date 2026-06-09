package com.example.skillswap.model;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
@Entity
@Table(name = "skills_to_learn")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillToLearn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String skill;
}
