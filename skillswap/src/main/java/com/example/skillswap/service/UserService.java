package com.example.skillswap.service;

import com.example.skillswap.model.User;
import com.example.skillswap.model.UserProfile;
import com.example.skillswap.model.SkillToLearn;
import com.example.skillswap.model.SkillToTeach;
import com.example.skillswap.repository.UserRepository;
import com.example.skillswap.repository.UserProfileRepository;
import com.example.skillswap.repository.SkillToLearnRepository;
import com.example.skillswap.repository.SkillToTeachRepository;
import com.example.skillswap.dto.MatchDetails;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SkillToLearnRepository skillToLearnRepository;
    private final SkillToTeachRepository skillToTeachRepository;
    private final PasswordEncoder passwordEncoder;
    // NOTE: SkillService is NOT injected here to keep the structure simple

    public UserService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       SkillToLearnRepository skillToLearnRepository,
                       SkillToTeachRepository skillToTeachRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.skillToLearnRepository = skillToLearnRepository;
        this.skillToTeachRepository = skillToTeachRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Existing Methods (Remain Unchanged) ---
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new CustomUserDetails(user);
    }

    @Transactional
    public User registerUser(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        User u = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role("USER")
                .build();
        return userRepository.save(u);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<User> findAllOtherUsers(String currentUsername) {
        return userRepository.findAllUsersWithSkills().stream()
                .filter(u -> !u.getUsername().equals(currentUsername) && !u.getRole().equals("ADMIN"))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<User> searchUsersBySkillOrName(String currentUsername, String query) {
        String lowerCaseQuery = query.toLowerCase();

        return userRepository.findAllUsersWithSkills().stream()
                .filter(u -> !u.getUsername().equals(currentUsername) && !u.getRole().equals("ADMIN"))
                .filter(u ->
                        u.getUsername().toLowerCase().contains(lowerCaseQuery) ||
                                (u.getProfile() != null && u.getProfile().getFullName() != null &&
                                        u.getProfile().getFullName().toLowerCase().contains(lowerCaseQuery)) ||
                                u.getSkillsToTeach().stream()
                                        .anyMatch(s -> s.getSkill().toLowerCase().contains(lowerCaseQuery)) ||
                                u.getSkillsToLearn().stream()
                                        .anyMatch(s -> s.getSkill().toLowerCase().contains(lowerCaseQuery))
                )
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfile saveOrUpdateProfile(String username, UserProfile newProfileData) {
        User user = findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found during profile update: " + username));

        UserProfile existingProfile = userProfileRepository.findByUserUsername(username).orElse(null);

        if (existingProfile != null) {
            existingProfile.setFullName(newProfileData.getFullName());
            existingProfile.setEmail(newProfileData.getEmail());
            existingProfile.setPronouns(newProfileData.getPronouns());
            if (newProfileData.getPhotoUrl() != null && !newProfileData.getPhotoUrl().isEmpty()) {
                existingProfile.setPhotoUrl(newProfileData.getPhotoUrl());
            }
            return userProfileRepository.save(existingProfile);
        } else {
            newProfileData.setUser(user);
            return userProfileRepository.save(newProfileData);
        }
    }

    @Transactional
    public void saveSkillsToLearn(String username, Set<String> skills) {
        User user = findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found for skill update."));
        skillToLearnRepository.deleteByUser(user);

        Set<SkillToLearn> newSkills = skills.stream()
                .map(skill -> SkillToLearn.builder()
                        .user(user)
                        .skill(skill.trim())
                        .build())
                .collect(Collectors.toSet());

        skillToLearnRepository.saveAll(newSkills);
    }

    @Transactional
    public void saveSkillsToTeach(String username, Set<String> skills) {
        User user = findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found for skill update."));
        skillToTeachRepository.deleteByUser(user);

        Set<SkillToTeach> newSkills = skills.stream()
                .map(skill -> SkillToTeach.builder()
                        .user(user)
                        .skill(skill.trim())
                        .build())
                .collect(Collectors.toSet());

        skillToTeachRepository.saveAll(newSkills);
    }

    @Transactional(readOnly = true)
    public User getUserWithSkills(String username) {
        return userRepository.findUserWithSkills(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByUsernames(List<String> usernames) {
        return userRepository.findByUsernameIn(usernames);
    }

    // BASE METHOD: Calculates all initial mutual matches (Unchanged)
    @Transactional(readOnly = true)
    public List<MatchDetails> findMutualMatches(String currentUsername) {
        User currentUser = getUserWithSkills(currentUsername);

        Set<String> userWantsSkills = currentUser.getSkillsToLearn().stream()
                .map(SkillToLearn::getSkill)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> userTeachesSkills = currentUser.getSkillsToTeach().stream()
                .map(SkillToTeach::getSkill)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return userRepository.findAllUsersWithSkills().stream()
                .filter(u -> !u.getUsername().equals(currentUsername) && !u.getRole().equals("ADMIN"))
                .map(targetUser -> {
                    Set<String> teachingOverlap = targetUser.getSkillsToTeach().stream()
                            .map(SkillToTeach::getSkill)
                            .filter(skill -> userWantsSkills.contains(skill.toLowerCase()))
                            .collect(Collectors.toSet());

                    Set<String> learningOverlap = targetUser.getSkillsToLearn().stream()
                            .map(SkillToLearn::getSkill)
                            .filter(skill -> userTeachesSkills.contains(skill.toLowerCase()))
                            .collect(Collectors.toSet());

                    if (!teachingOverlap.isEmpty() && !learningOverlap.isEmpty()) {
                        return MatchDetails.builder()
                                .matchedUser(targetUser)
                                .matchingTeachSkills(teachingOverlap)
                                .matchingLearnSkills(learningOverlap)
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ⭐ NEW METHOD: Filters the mutual matches list based ONLY on Location
    @Transactional(readOnly = true)
    public List<MatchDetails> findFilteredMatchesByLocation(String currentUsername, String locationFilter) {

        // 1. Get the complete list of mutual matches using the existing method
        List<MatchDetails> allMutualMatches = findMutualMatches(currentUsername);

        // If no location filter is provided, return all matches
        if (locationFilter == null || locationFilter.trim().isEmpty()) {
            return allMutualMatches;
        }

        final String lowerLocation = locationFilter.trim().toLowerCase();

        // 2. Filter the list using a Stream based on location
        return allMutualMatches.stream()
                .filter(match -> {
                    User targetUser = match.getMatchedUser();

                    // Location Check: Profile and Location must exist and match (case-insensitive contains)
                    if (targetUser.getProfile() == null ||
                            targetUser.getProfile().getLocation() == null) {
                        return false; // Skip if no profile or location exists
                    }

                    // Use 'contains' for partial matches (e.g., "Bhilwara" matches "Bhilwara City")
                    return targetUser.getProfile().getLocation().toLowerCase().contains(lowerLocation);
                })
                .collect(Collectors.toList());
    }
}

