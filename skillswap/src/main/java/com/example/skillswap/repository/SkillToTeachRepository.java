package com.example.skillswap.repository;

import com.example.skillswap.model.SkillToTeach;
import com.example.skillswap.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillToTeachRepository extends JpaRepository<SkillToTeach, Long> {
    List<SkillToTeach> findByUserId(Long userId);  // skills list ke liye
    void deleteByUser(User user);                  // bulk delete ke liye
}
