package com.server.talkup_be.entity;

import lombok.*;

// user 내의 시선 calibration으로 쓸 예정
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // jpa가 user entity 속 eyeCalibration 비교 가능하도록 해줌
public class EyeCalibration {
    private Double leftEyeOffset;
    private Double rightEyeOffset;
    private Double ratio;
}