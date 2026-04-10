package com.server.talkup_be.repo;

import com.server.talkup_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<User, UUID> {
    Optional<User> findById(UUID id);

    Optional<User> findByLoginId(String loginId);
}
