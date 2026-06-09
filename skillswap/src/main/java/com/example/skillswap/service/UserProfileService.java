package com.example.skillswap.service;

import com.example.skillswap.model.UserProfile;
import com.example.skillswap.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile saveProfile(UserProfile profile) {
        return userProfileRepository.save(profile);
    }

    public Optional<UserProfile> getProfileByUsername(String username) {
        return userProfileRepository.findByUserUsername(username);
    }

    public void deleteProfile(Long id) {
        userProfileRepository.deleteById(id);
    }
}
