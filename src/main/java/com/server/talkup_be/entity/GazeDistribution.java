package com.server.talkup_be.entity;

import lombok.*;

// analysis 내의 시선 백분율로 쓸 예정
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // jpa가 analysis entity 속 GazeDistribution 비교 가능하도록 해줌
public class GazeDistribution {
    private Float screen;
    private Float camera;
}
