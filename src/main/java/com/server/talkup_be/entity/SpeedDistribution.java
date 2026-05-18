package com.server.talkup_be.entity;

import lombok.*;

// analysis 내의 발화 속도 백분율로 쓸 예정
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // jpa가 analysis entity 속 SpeedDistribution 비교 가능하도록 해줌
public class SpeedDistribution {
    private Float fast;
    private Float optimal;
    private Float slow;
}
