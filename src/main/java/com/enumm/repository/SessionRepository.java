package com.enumm.repository;

import com.enumm.model.Session;
import com.enumm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByToken(String token);
    void deleteByUser(User user);
    void deleteByExpiryDateBefore(LocalDateTime dateTime);
}