package com.server.talkup_be.repo;

import com.server.talkup_be.dto.UserDto;
import com.server.talkup_be.entity.EyeCalibration;
import com.server.talkup_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<User, UUID> {
    //고유id로 user정보 반환
    Optional<User> findById(UUID id);

    // loginId 중복 체크 및 로그인시 필요
    Optional<User> findByLoginId(String loginId);

    // 내 정보 조회시 필요
    @Query("SELECT new com.server.talkup_be.dto.UserDto$UserInfo(" +
            "u.loginId, u.name, u.email ) "+
            "FROM User u " +
            "WHERE u.id = :myId")
    UserDto.UserInfo findByIdFromFront(UUID myId);

    UserDto.UserEye findByIdFromEyeCalibration(UUID myId);
}
