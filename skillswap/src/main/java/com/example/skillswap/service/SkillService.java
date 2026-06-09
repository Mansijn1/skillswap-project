package com.example.skillswap.service;

import com.example.skillswap.model.SkillToLearn;
import com.example.skillswap.model.SkillToTeach;
import com.example.skillswap.repository.SkillToLearnRepository;
import com.example.skillswap.repository.SkillToTeachRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SkillService {

    private final SkillToLearnRepository learnRepo;
    private final SkillToTeachRepository teachRepo;

    public SkillService(SkillToLearnRepository learnRepo, SkillToTeachRepository teachRepo) {
        this.learnRepo = learnRepo;
        this.teachRepo = teachRepo;
    }

    // --- Skills To Learn ---
    public SkillToLearn addSkillToLearn(SkillToLearn skill) {
        return learnRepo.save(skill);
    }

    public List<SkillToLearn> getSkillsToLearn(Long userId) {
        return learnRepo.findByUserId(userId);
    }

    public void deleteSkillToLearn(Long id) {
        learnRepo.deleteById(id);
    }

    // --- Skills To Teach ---
    public SkillToTeach addSkillToTeach(SkillToTeach skill) {
        return teachRepo.save(skill);
    }

    public List<SkillToTeach> getSkillsToTeach(Long userId) {
        return teachRepo.findByUserId(userId);
    }

    public void deleteSkillToTeach(Long id) {
        teachRepo.deleteById(id);
    }

    // ✅ NEW — Combine & return all distinct skills (for dropdowns)
    public List<String> getAllDistinctSkills() {
        List<String> teachSkills = teachRepo.findAll().stream()
                .map(SkillToTeach::getSkill)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        List<String> learnSkills = learnRepo.findAll().stream()
                .map(SkillToLearn::getSkill)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        return Stream.concat(teachSkills.stream(), learnSkills.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
