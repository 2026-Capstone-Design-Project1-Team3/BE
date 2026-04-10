package com.server.talkup_be.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// user 내의 시선 calibration으로 쓸 예정
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EyeCalibration {
    private Double leftEyeOffset;
    private Double rightEyeOffset;
    private Double ratio;
}