package com.server.talkup_be.service;

import com.server.talkup_be.dto.UserDto;
import com.server.talkup_be.entity.EyeCalibration;
import com.server.talkup_be.entity.User;
import com.server.talkup_be.repo.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo;

    public UserService(PasswordEncoder passwordEncoder, UserRepo userRepo) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
    }

    // user login 확인
    public User validateUser(String loginId, String passWord) {
        // 1. DB에서 유저 찾기
        User user = (User) userRepo.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("해당 아이디가 없습니다."));

        // 2. 비밀번호가 일치하는지 확인(암호화 된 비번과 비교)
        if (!passwordEncoder.matches(passWord, user.getPassWord())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        return user;
    }

    // user 저장
    public void save(UserDto.UserInput userInput) {
        // 1. 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(userInput.getPassWord());

        // 2. user 저장
        User newUser = User.builder()
                .loginId(userInput.getLoginId())
                .passWord(encodedPassword)
                .email(userInput.getEmail())
                .name(userInput.getName())
                .build();

        userRepo.save(newUser);
    }

    //user 정보 수정
    @Transactional
    public void updateUser(UUID userId, UserDto.UserUpdate updateDto) {

        // 1. 내 정보 찾기
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 비번 변경 여부 확인 (null이거나 빈 칸이 아닌지 확인)
        boolean hasPast = updateDto.getPastPassWord() != null && !updateDto.getPastPassWord().isBlank();
        boolean hasNew = updateDto.getNewPassWord() != null && !updateDto.getNewPassWord().isBlank();

        // 3. 403 에러 조건: 둘 중 하나만 온 경우
        if (hasPast != hasNew) {
            throw new IllegalStateException("past와 new가 같이 오지 않음");
        }

        // 4. 400 에러 조건: 기존 비번이 틀린 경우
        if (hasPast && hasNew) {
            if (!passwordEncoder.matches(updateDto.getPastPassWord(), user.getPassWord())) {
                throw new IllegalArgumentException("기존 비번 맞지 않아 해당 권한이 없음");
            }
            // 5. 검증 통과 시 새 비번 암호화해서 갈아끼우기
            user.updatePassword(passwordEncoder.encode(updateDto.getNewPassWord()));
        }

        // 6. 이름이나 이메일 업데이트
        user.updateUser(updateDto.getName(), updateDto.getEmail());
    }

    // 만들려는 loginId 중복 체크
    public Integer check(String loginId) {
        //있으면? 0
        return userRepo.findByLoginId(loginId).isPresent() ? 1 : 0;
    }

    // user 정보 반환
    public UserDto.UserInfo getUser(UUID myId) {
        return userRepo.findByIdFromFront(myId);
    }

    // user 시선 캘리브레이션 정보 반환
    public UserDto.UserEye getUserEye(UUID myId) {
        return userRepo.findByIdFromEyeCalibration(myId);
    }

    // user 시선 캘리브레이션 정보 저장
    public void saveUserEyeData(UUID userId, UserDto.UserEye userEye) {
        // 1. 내 정보 찾기
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. EyeCalibration 객체 생성
        EyeCalibration newCalibration = EyeCalibration.builder()
                .leftEyeOffset(userEye.getLeftEyeOffset())
                .rightEyeOffset(userEye.getRightEyeOffset())
                .ratio(userEye.getRatio())
                .build();

        // 3. user Update eye Calibration
        user.updateEyeCalibration(newCalibration);
    }
}
