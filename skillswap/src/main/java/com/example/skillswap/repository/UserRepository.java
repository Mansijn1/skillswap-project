package com.example.skillswap.repository;

import com.example.skillswap.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile LEFT JOIN FETCH u.skillsToLearn LEFT JOIN FETCH u.skillsToTeach WHERE u.username = :username")
    Optional<User> findUserWithSkills(@Param("username") String username);

    List<User> findByUsernameIn(List<String> usernames);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile LEFT JOIN FETCH u.skillsToLearn LEFT JOIN FETCH u.skillsToTeach")
    List<User> findAllUsersWithSkills();
}
