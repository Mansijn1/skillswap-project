package com.example.skillswap.dto;

import com.example.skillswap.model.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
public class MatchDetails {
    // The user who is a potential swap partner
    private User matchedUser;

    // Skills the matchedUser can TEACH that the current user WANTS (Teaching overlap)
    private Set<String> matchingTeachSkills;

    // Skills the matchedUser WANTS that the current user can TEACH (Learning overlap)
    private Set<String> matchingLearnSkills;
}
