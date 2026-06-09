package com.example.skillswap.repository;

import com.example.skillswap.model.SkillToLearn;
import com.example.skillswap.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillToLearnRepository extends JpaRepository<SkillToLearn, Long> {
    List<SkillToLearn> findByUserId(Long userId);  // ek user ke saare "learn" skills laane ke liye
    void deleteByUser(User user);                  // ek user ke saare "learn" skills ek saath delete karne ke liye
}
