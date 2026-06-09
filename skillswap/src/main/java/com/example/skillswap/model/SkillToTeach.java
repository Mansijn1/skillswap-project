package com.example.skillswap.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skills_to_teach")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillToTeach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String skill;
}
